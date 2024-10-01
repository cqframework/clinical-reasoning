package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_POPULATION_SYSTEM;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
     * <p>
     * GapStatus is determined by interpreting a MeasureReport resource of Type Ratio or Proportion that contain the populations: Numerator & Denominator
     * </p>
     *<p>
     * <ul>
     *   <li>'not-applicable': When a subject does not meet the criteria for the Measure scenario, whether by exclusion or exception criteria, or just by not meeting any required criteria, they will not show membership results in the 'Denominator'.</li>
     *   <li> subject is applicable (not a status): When a subject meets the criteria for a Measure they will have membership results in the 'Denominator', indicating they are of the appropriate criteria for the Measure scenario.</li>
     * If in membership of 'Denominator', the subject will be assigned a 'closed-gap' or 'open-gap' status based on membership in 'Numerator' and the 'improvement notation'.
     *</ul>
     * </p>
     * <p>
     * Improvement Notation of Scoring Algorithm indicates whether the ratio of Numerator over Denominator populations represents a scenario to increase the Numerator to improve outcomes, or to decrease the Numerator count. If this value is not set on a Measure resource, then it is defaulted to 'Increase' under the IsPositive variable.
     * </p>
     * <ul>
     * <li>ex: 1/10 with improvementNotation "decrease" means that the measureScore is 90%, therefore absence from 'Numerator' means criteria for care was met</li>
     * <li>ex: 1/10 with improvementNotation "increase" means that the measureScore is 10%, therefore absence from 'Numerator' means criteria for care was NOT met.</li>
     * </ul>
     * <ul>
     * <li>'open-gap': if in 'Denominator' & NOT in 'Numerator', where 'improvement notation' = increase. Then the subject is 'open-gap'</li>
     * <li>'open-gap': if in 'Denominator' & in 'Numerator', where 'improvement notation' = decrease. Then the subject is 'open-gap'</li>
     * <li>'closed-gap': if in 'Denominator' & NOT in 'Numerator', where 'improvement notation' = decrease. Then the subject is 'closed-gap'</li>
     * <li>'closed-gap': if in 'Denominator' & in 'Numerator', where 'improvement notation' = increase. Then the subject is 'closed-gap'</li>
     * </ul>
     * <p>'prospective-gap' is a concept that represents a period of time where a 'care-gap' measure has opportunity to address recommended care in a specific window of time. This 'window of time' we call the 'Date of Compliance' to indicate a range of time that optimally represents when care is meant to be provided.</p>
     *<br/>
     * <p>If care has not been provided ('open-gap'), and the date (reportDate) of evaluating for the Measure is before or within the 'Date of Compliance' interval, then the Measure is considered a 'prospective-gap' for the subject evaluated.</p>
     *
     * <p></p>
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
        Pair<String, Boolean> inNumerator = new MutablePair<>("numerator", false);
        Pair<String, Boolean> inDenominator = new MutablePair<>("denominator", false);
        // get Numerator and Denominator membership
        measureReportGroup.getPopulation().forEach(population -> {
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

        // default improvementNotation
        boolean isPositive = true;

        // TODO: look for group specified 'improvement notation', if missing, then look on measure
        /*if (groupHasImprovementNotation(measureReportGroup)) {
            isPositive = groupImprovementNotationIsPositive(measureReportGroup);
        } else if (measure.hasImprovementNotation()) {*/
        if (measure.hasImprovementNotation()) {
            isPositive = measure.getImprovementNotation()
                    .hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);
        }

        if (Boolean.FALSE.equals(inDenominator.getValue())) {
            // patient is not in eligible population
            return CareGapsStatusCode.NOT_APPLICABLE;
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

    /*
    // TODO implement Measure Group Level improvement notation extension
    private boolean groupHasImprovementNotation(MeasureReportGroupComponent groupComponent) {
        return groupComponent.getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION) != null;
    }

    private boolean groupImprovementNotationIsPositive(MeasureReportGroupComponent groupComponent) {
        var code = (CodeableConcept) groupComponent
                .getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION)
                .getValue();
        return code.hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, "increase");
    }*/
}
