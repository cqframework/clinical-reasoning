package org.opencds.cqf.fhir.cr.measure.r4.utils;

import jakarta.annotation.Nullable;
import java.util.Objects;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

/**
 * Utility class for extracting data from R4 FHIR MeasureReport resources.
 *
 * <p>This class provides static helper methods for working with MeasureReport resources, including:
 * <ul>
 *   <li>Extracting population counts from groups and stratifiers</li>
 *   <li>Extracting population types</li>
 *   <li>Matching stratum values</li>
 * </ul>
 *
 * <p>All methods are static and designed to be reusable across different measure evaluation contexts.
 */
public class R4MeasureReportUtils {

    private R4MeasureReportUtils() {
        // Utility class
    }

    /**
     * Extract text representation from StratumDef for R4 MeasureReport matching.
     * This method handles both CodeableConcept and primitive value types, and supports
     * both component and non-component stratifiers.
     *
     * <p>Based on logic from R4MeasureReportScorer and R4MeasureReportBuilder.
     *
     * @param stratifierDef the StratifierDef containing type information
     * @param stratumDef the StratumDef containing value information
     * @return text representation of the stratum value, or null if not determinable
     */
    public static String getStratumDefText(StratifierDef stratifierDef, StratumDef stratumDef) {
        String stratumText = null;

        for (StratumValueDef valuePair : stratumDef.valueDefs()) {
            var value = valuePair.value();
            var componentDef = valuePair.def();

            // Handle CodeableConcept values
            if (value.getValueClass().equals(CodeableConcept.class)) {
                if (stratumDef.isComponent()) {
                    // component stratifier: use code text
                    stratumText = componentDef != null && componentDef.code() != null
                            ? componentDef.code().text()
                            : null;
                } else {
                    // non-component: extract text from CodeableConcept value
                    if (value.getValue() instanceof CodeableConcept codeableConcept) {
                        stratumText = codeableConcept.getText();
                    }
                }
            } else if (stratumDef.isComponent()) {
                // Component with non-CodeableConcept value: convert to string
                stratumText = value.getValueAsString();
            } else if (MeasureStratifierType.VALUE == stratifierDef.getStratifierType()) {
                // VALUE-type stratifiers with non-CodeableConcept values
                stratumText = value.getValueAsString();
            } else if (MeasureStratifierType.NON_SUBJECT_VALUE == stratifierDef.getStratifierType()) {
                // NON_SUBJECT_VALUE-type stratifiers with non-CodeableConcept values
                stratumText = value.getValueAsString();
            } else if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
                // CRITERIA-type stratifiers with non-CodeableConcept values
                stratumText = value.getValueAsString();
            }
        }

        return stratumText;
    }

    /**
     * Check if a MeasureReport stratum matches a StratumDef by comparing text representations.
     *
     * <p>Uses text-based comparison to match the stratum value from the MeasureReport
     * with the text extracted from the StratumDef. This approach prevents test failures
     * in RATIO and CONTINUOUSVARIABLE measures with stratifiers.
     *
     * @param reportStratum the MeasureReport StratifierGroupComponent (stratum)
     * @param stratumDef the StratumDef to match against
     * @param stratifierDef the parent StratifierDef (for context)
     * @return true if the stratum values match, false otherwise
     */
    public static boolean matchesStratumValue(
            StratifierGroupComponent reportStratum, StratumDef stratumDef, StratifierDef stratifierDef) {
        // Use the same logic as R4MeasureReportScorer: compare CodeableConcept.text
        String reportText = reportStratum.hasValue() ? reportStratum.getValue().getText() : null;
        String defText = getStratumDefText(stratifierDef, stratumDef);
        return Objects.equals(reportText, defText);
    }

    public static void addAggregationResultMethodAndCriteriaRef(
            MeasureReportGroupPopulationComponent reportPopulation, PopulationDef populationDef) {

        addAggregationResultMethodAndCriteriaRef(
                reportPopulation,
                populationDef.getAggregateMethod(),
                populationDef.getAggregationResult(),
                populationDef.getCriteriaReference());
    }

    /**
     * We need to capture all 3 data points:
     * <ul>
     *     <li>aggregation method: Downstream clients need this to combine scores among reports</li>
     *     <li>aggregate result: Numeric value needed for downstream clients to combine scores</li>
     *     <li>criteriaReference: Downstream clients need this to associate MEASUREOBSERVATION
     *     populations with NUMERATOR and DENOMINATOR populations to calculate RCV scores</li>
     * </ul>
     */
    public static void addAggregationResultMethodAndCriteriaRef(
            MeasureReportGroupPopulationComponent measurePopulation,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod,
            @Nullable Double aggregationResult,
            @Nullable String criteriaReference) {

        // We always copy the criteriaReference, even if there are no results
        if (criteriaReference != null) {
            addCriteriaReferenceInner(measurePopulation, criteriaReference);
        }

        if (aggregateMethod != null
                && ContinuousVariableObservationAggregateMethod.N_A != aggregateMethod
                && aggregationResult != null) {

            addAggregateMethodInner(measurePopulation, aggregateMethod);
            addAggregationResultInner(measurePopulation, aggregationResult);
        }
    }

    private static void addAggregateMethodInner(
            MeasureReportGroupPopulationComponent measurePopulation,
            ContinuousVariableObservationAggregateMethod aggregateMethod) {

        addExtensionValueInner(
                measurePopulation,
                MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL,
                new StringType(aggregateMethod.getText()));
    }

    private static void addAggregationResultInner(
            MeasureReportGroupPopulationComponent measurePopulation, double aggregationResult) {

        addExtensionValueInner(
                measurePopulation, MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(aggregationResult));
    }

    private static void addCriteriaReferenceInner(
            MeasureReportGroupPopulationComponent measurePopulation, String criteriaReference) {

        addExtensionValueInner(
                measurePopulation, MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE, new StringType(criteriaReference));
    }

    private static void addExtensionValueInner(
            MeasureReportGroupPopulationComponent measurePopulation, String extensionUrl, Type extensionValue) {

        if (measurePopulation.hasExtension(extensionUrl)) {
            measurePopulation.getExtensionByUrl(extensionUrl).setValue(extensionValue);
        } else {
            measurePopulation.addExtension(extensionUrl, extensionValue);
        }
    }
}
