package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_POPULATION_SYSTEM;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.Period;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;

/**
 * Care Gaps Status Evaluator houses the algorithm logic for which Care-Gap status is applicable to a Measure Report.
 */
public class R4CareGapStatusEvaluator {
    /**
     * Below table is a mapping of expected behavior of the care-gap-status to Measure Report score
     * | Gap-Status        | initial-population | denominator | denominator-exclusion | denominator-exception\*\* | numerator-exclusion | numerator | Improvement Notation\*\*\* |
     * | ----------------- | ------------------ | ----------- | --------------------- | ------------------------- | ------------------- | --------- | -------------------------- |
     * | not-applicable    | FALSE              | N/A         | N/A                   | N/A                       | N/A                 | N/A       | N/A                        |
     * | closed-gap        | TRUE               | FALSE       | FALSE                 | FALSE                     | FALSE               | FALSE     | N/A                        |
     * | closed-gap        | TRUE               | FALSE       | TRUE                  | FALSE                     | FALSE               | FALSE     | N/A                        |
     * | closed-gap        | TRUE               | FALSE       | FALSE                 | TRUE                      | FALSE               | FALSE     | N/A                        |
     * | prospective-gap\* | TRUE               | TRUE        | FALSE                 | FALSE                     | FALSE               | FALSE     | N/A                        |
     * | prospective-gap\* | TRUE               | TRUE        | FALSE                 | FALSE                     | TRUE                | FALSE     | N/A                        |
     * | open-gap          | TRUE               | TRUE        | FALSE                 | FALSE                     | TRUE                | FALSE     | increase                   |
     * | open-gap          | TRUE               | TRUE        | FALSE                 | FALSE                     | FALSE               | FALSE     | increase                   |
     * | open-gap          | TRUE               | TRUE        | FALSE                 | FALSE                     | FALSE               | TRUE      | decrease                   |
     * | closed-gap        | TRUE               | TRUE        | FALSE                 | FALSE                     | TRUE                | FALSE     | decrease                   |
     * | closed-gap        | TRUE               | TRUE        | FALSE                 | FALSE                     | FALSE               | TRUE      | increase                   |
     * | closed-gap        | TRUE               | TRUE        | FALSE                 | FALSE                     | FALSE               | FALSE     | decrease                   |
     *
     * *`prospective-gap` status requires additional data points than just population-code values within a MeasureReport in order to determine if ‘prospective-gap’ or just ‘open-gap’.
     * **denominator-exception: is only for ‘proportion’ scoring type.
     * ***improvement Notation: is a Measure defined value that tells users how to view results of the Measure Report.
     */
    public Map<String, CareGapsStatusCode> getGroupGapStatus(Measure measure, MeasureReport measureReport) {
        Map<String, CareGapsStatusCode> groupStatus = new HashMap<>();
        var reportDate = measureReport.getDateElement();

        for (MeasureReportGroupComponent group : measureReport.getGroup()) {
            var groupId = group.getId();
            var gapStatus = getGapStatus(measure, group, reportDate);

            groupStatus.put(groupId, gapStatus);
        }
        return groupStatus;
    }

    private CareGapsStatusCode getGapStatus(
            Measure measure, MeasureReportGroupComponent measureReportGroup, DateTimeType reportDate) {
        Pair<String, Boolean> inInitialPopulation = new MutablePair<>("initial-population", false);
        Pair<String, Boolean> inNumerator = new MutablePair<>("numerator", false);
        Pair<String, Boolean> inDenominator = new MutablePair<>("denominator", false);
        // get Numerator and Denominator membership
        measureReportGroup.getPopulation().forEach(population -> {
            if (population.hasCode()
                    && population
                            .getCode()
                            .hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inInitialPopulation.getKey())
                    && population.getCount() == 1) {
                inInitialPopulation.setValue(true);
            }
            if (population.hasCode()
                    && population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inNumerator.getKey())
                    && population.getCount() == 1) {
                inNumerator.setValue(true);
            }
            if (population.hasCode()
                    && population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inDenominator.getKey())
                    && population.getCount() == 1) {
                inDenominator.setValue(true);
            }
        });

        // is subject eligible for measure?
        if (Boolean.FALSE.equals(inInitialPopulation.getValue())) {
            return CareGapsStatusCode.NOT_APPLICABLE;
        }
        // default improvementNotation
        boolean isPositive = true;

        // look for group specified 'improvement notation', if missing, then look on measure
        if (groupHasImprovementNotation(measureReportGroup)) {
            isPositive = groupImprovementNotationIsPositive(measureReportGroup);
        } else if (measure.hasImprovementNotation()) {
            isPositive = measure.getImprovementNotation()
                    .hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);
        }

        if (Boolean.TRUE.equals(inDenominator.getValue())
                && ((isPositive && !inNumerator.getValue()) || (!isPositive && inNumerator.getValue()))) {
            return getOpenOrProspectiveStatus(measureReportGroup, reportDate);
        }

        return CareGapsStatusCode.CLOSED_GAP;
    }

    private CareGapsStatusCode getOpenOrProspectiveStatus(
            MeasureReportGroupComponent measureReportGroup, DateTimeType reportDate) {
        if (hasDateOfComplianceExt(measureReportGroup)) {
            Period dateOfCompliance = getDateOfComplianceExt(measureReportGroup);

            boolean reportBeforeEndOfDOC = reportDate.before(dateOfCompliance.getEndElement());
            boolean reportAfterStartOfDOC = reportDate.after(dateOfCompliance.getStartElement());
            boolean reportBeforeStartOfDOC = reportDate.before(dateOfCompliance.getStartElement());

            boolean reportWithinDOC = reportAfterStartOfDOC && reportBeforeEndOfDOC;
            if (reportWithinDOC || reportBeforeStartOfDOC) {
                return CareGapsStatusCode.PROSPECTIVE_GAP;
            }
        }
        return CareGapsStatusCode.OPEN_GAP;
    }

    private boolean hasDateOfComplianceExt(MeasureReportGroupComponent measureReportGroup) {
        var ext = measureReportGroup.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL);
        return ext != null && !ext.getValue().isEmpty();
    }

    private Period getDateOfComplianceExt(MeasureReportGroupComponent measureReportGroup) {
        var ext = measureReportGroup.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL);
        var extValue = ext.getValue();
        assert extValue instanceof Period;
        return (Period) extValue;
    }

    // Measure Group Level improvement notation extension
    private boolean groupHasImprovementNotation(MeasureReportGroupComponent groupComponent) {
        return groupComponent.getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION) != null;
    }

    private boolean groupImprovementNotationIsPositive(MeasureReportGroupComponent groupComponent) {
        var code = (CodeableConcept) groupComponent
                .getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION)
                .getValue();
        return code.hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);
    }
}
