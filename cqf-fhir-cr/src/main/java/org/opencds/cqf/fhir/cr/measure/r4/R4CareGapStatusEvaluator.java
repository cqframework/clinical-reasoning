package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_POPULATION_SYSTEM;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;

/*
Care Gaps Status Evaluator houses the algorithm logic for which Care-Gap status is applicable to a Measure Report.
 */
public class R4CareGapStatusEvaluator {

    public CareGapsStatusCode getGapStatus(Measure measure, MeasureReport measureReport) {
        Pair<String, Boolean> inNumerator = new MutablePair<>("numerator", false);
        Pair<String, Boolean> inDenominator = new MutablePair<>("denominator", false);
        measureReport.getGroup().forEach(group -> group.getPopulation().forEach(population -> {
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
        }));
        // default improvementNotation
        boolean isPositive = true;

        // if value is present, set value from measure if populated
        if (measure.hasImprovementNotation()) {
            isPositive =
                    measure.getImprovementNotation().hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, "increase");
        }

        if (Boolean.FALSE.equals(inDenominator.getValue())) {
            // patient is not in eligible population
            return CareGapsStatusCode.NOT_APPLICABLE;
        }

        if (Boolean.TRUE.equals(inDenominator.getValue())
                && ((isPositive && !inNumerator.getValue()) || (!isPositive && inNumerator.getValue()))) {
            return CareGapsStatusCode.OPEN_GAP;
        }

        return CareGapsStatusCode.CLOSED_GAP;
    }
}
