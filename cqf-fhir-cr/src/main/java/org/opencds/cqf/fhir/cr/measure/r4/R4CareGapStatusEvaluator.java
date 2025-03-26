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
     * Compares MeasureReport values and calculated results for various gap statuses.
     *
     * <p>This table evaluates performance across different population criteria:
     * Initial Population, Denominator, Denominator Exclusion, Numerator,
     * Denominator Exception, and Numerator Exclusion.
     *
     * <pre>{@code
     * Gap-Status       | Improvement | IP MR | IP Calc | D MR  | D Calc | DX MR | DX Calc | N MR  | N Calc | DE MR | DE Calc | NX MR | NX Calc
     * -----------------|-------------|-------|---------|-------|--------|-------|---------|-------|--------|-------|---------|-------|---------
     * not-applicable   | N/A         | FALSE | FALSE   | N/A   | N/A    | N/A   | N/A     | N/A   | N/A    | N/A   | N/A     | N/A   | N/A
     * closed-gap       | N/A         | TRUE  | TRUE    | FALSE | FALSE  | FALSE | FALSE   | FALSE | FALSE  | FALSE | FALSE   | FALSE | FALSE
     * closed-gap       | N/A         | TRUE  | TRUE    | TRUE  | FALSE  | TRUE  | TRUE    | FALSE | FALSE  | FALSE | FALSE   | FALSE | FALSE
     * closed-gap       | N/A         | TRUE  | TRUE    | TRUE  | FALSE  | FALSE | FALSE   | FALSE | FALSE  | TRUE  | TRUE    | FALSE | FALSE
     * prospective-gap* | N/A         | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | FALSE | FALSE  | FALSE | FALSE   | FALSE | FALSE
     * prospective-gap* | N/A         | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | TRUE  | FALSE  | FALSE | FALSE   | TRUE  | TRUE
     * open-gap         | increase    | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | TRUE  | FALSE  | FALSE | FALSE   | TRUE  | TRUE
     * open-gap         | increase    | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | FALSE | FALSE  | FALSE | FALSE   | FALSE | FALSE
     * open-gap         | decrease    | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | TRUE  | TRUE   | FALSE | FALSE   | FALSE | FALSE
     * closed-gap       | decrease    | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | TRUE  | FALSE  | FALSE | FALSE   | TRUE  | TRUE
     * closed-gap       | increase    | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | TRUE  | TRUE   | FALSE | FALSE   | FALSE | FALSE
     * closed-gap       | decrease    | TRUE  | TRUE    | TRUE  | TRUE   | FALSE | FALSE   | FALSE | FALSE  | FALSE | FALSE   | FALSE | FALSE
     * }</pre>
     *
     * <p><strong>Legend:</strong><br>
     * - MR = MeasureReport<br>
     * - Calc = Calculated Result<br>
     * - IP = Initial Population<br>
     * - D = Denominator<br>
     * - DX = Denominator Exclusion<br>
     * - N = Numerator<br>
     * - DE = Denominator Exception<br>
     * - NX = Numerator Exclusion<br>
     *
     * <p>Rows marked with <code>*</code> (e.g., prospective-gap*) indicate predictive or future-looking evaluations.
     *
     * <p> Previous interpretation of care-gap status from MeasureReport v3.18.0 and below only interpreted Numerator, Denominator, Initial-Population membership to determine care-gap status. Since exclusions and exceptions are now present in Denominator and Numerator populations, the care-gap evaluator had to take into account additional population membership to determine Final-Numerator and Final-Denominator membership</p>
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
        Pair<String, Boolean> inNumeratorExclusion = new MutablePair<>("numerator-exclusion", false);
        Pair<String, Boolean> inDenominator = new MutablePair<>("denominator", false);
        Pair<String, Boolean> inDenominatorException = new MutablePair<>("denominator-exception", false);
        Pair<String, Boolean> inDenominatorExclusion = new MutablePair<>("denominator-exclusion", false);

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
            if (population.hasCode()
                    && population
                            .getCode()
                            .hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inDenominatorExclusion.getKey())
                    && population.getCount() == 1) {
                inDenominatorExclusion.setValue(true);
                // if in Exclusion, then move to false
                inDenominator.setValue(false);
            }
            if (population.hasCode()
                    && population
                            .getCode()
                            .hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inDenominatorException.getKey())
                    && population.getCount() == 1) {
                inDenominatorException.setValue(true);
                // if in Exception, then move to false
                inDenominator.setValue(false);
            }
            if (population.hasCode()
                    && population
                            .getCode()
                            .hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inNumeratorExclusion.getKey())
                    && population.getCount() == 1) {
                inNumeratorExclusion.setValue(true);
                // if in Exclusion, then move to false
                inNumerator.setValue(false);
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
