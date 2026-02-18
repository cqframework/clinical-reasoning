package org.opencds.cqf.fhir.cr.measure.r4.utils;

import jakarta.annotation.Nullable;
import java.util.Objects;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants;

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
        var valueDefs = stratumDef.valueDefs();

        // Early return if no values
        if (valueDefs.isEmpty()) {
            return null;
        }

        // Fast path for non-component stratifiers (single value)
        if (!stratumDef.isComponent()) {
            var valuePair = valueDefs.iterator().next();
            var value = valuePair.value();

            // Handle CodeableConcept values for non-component
            if (value.getValueClass().equals(CodeableConcept.class)
                    && value.getValue() instanceof CodeableConcept codeableConcept) {
                return codeableConcept.getText();
            }

            // Handle VALUE or CRITERIA type stratifiers with non-CodeableConcept values
            var stratifierType = stratifierDef.getStratifierType();

            if (MeasureStratifierType.VALUE == stratifierType) {
                // VALUE-type stratifiers with non-CodeableConcept values
                return value.getValueAsString();
            } else if (MeasureStratifierType.NON_SUBJECT_VALUE == stratifierType) {
                // NON_SUBJECT_VALUE-type stratifiers with non-CodeableConcept values
                return value.getValueAsString();
            } else if (MeasureStratifierType.CRITERIA == stratifierType) {
                // CRITERIA-type stratifiers with non-CodeableConcept values
                return value.getValueAsString();
            }

            return null;
        }

        // Process component stratifiers (multiple values)
        String stratumText = null;
        for (var valuePair : valueDefs) {
            var value = valuePair.value();

            if (value.getValueClass().equals(CodeableConcept.class)) {
                // component stratifier with CodeableConcept: use code text
                var componentDef = valuePair.def();
                var text = componentDef != null && componentDef.code() != null
                        ? componentDef.code().text()
                        : null;
                if (text != null) {
                    stratumText = text;
                }
            } else {
                // Component with non-CodeableConcept value: convert to string and return immediately
                return value.getValueAsString();
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

    public static void addExtensionImprovementNotation(MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
        // if already set on Measure, don't set on groups too
        if (groupDef.isGroupImprovementNotation()) {
            if (groupDef.isIncreaseImprovementNotation()) {
                reportGroup.addExtension(
                        MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION,
                        new CodeableConcept(new Coding(
                                MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE_DISPLAY)));
            } else {
                reportGroup.addExtension(
                        MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION,
                        new CodeableConcept(new Coding(
                                MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE_DISPLAY)));
            }
        }
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

        addAggregationResultMethodAndCriteriaRefInner(
                measurePopulation, aggregateMethod, aggregationResult, criteriaReference);
    }

    /**
     * Add aggregation result, method, and criteria reference extensions to a stratum population.
     * Mirrors the group-level population overload for use with stratifier strata.
     */
    public static void addAggregationResultMethodAndCriteriaRef(
            StratifierGroupPopulationComponent stratumPopulation,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod,
            @Nullable Double aggregationResult,
            @Nullable String criteriaReference) {

        addAggregationResultMethodAndCriteriaRefInner(
                stratumPopulation, aggregateMethod, aggregationResult, criteriaReference);
    }

    private static void addAggregationResultMethodAndCriteriaRefInner(
            Element element,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod,
            @Nullable Double aggregationResult,
            @Nullable String criteriaReference) {

        // We always copy the criteriaReference, even if there are no results
        if (criteriaReference != null) {
            addCriteriaReferenceInner(element, criteriaReference);
        }

        if (aggregateMethod != null
                && ContinuousVariableObservationAggregateMethod.N_A != aggregateMethod
                && aggregationResult != null) {

            addAggregateMethodInner(element, aggregateMethod);
            addAggregationResultInner(element, aggregationResult);
        }
    }

    private static void addAggregateMethodInner(
            Element element, ContinuousVariableObservationAggregateMethod aggregateMethod) {

        addExtensionValueInner(
                element, MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL, new StringType(aggregateMethod.getText()));
    }

    private static void addAggregationResultInner(Element element, double aggregationResult) {

        addExtensionValueInner(
                element, MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(aggregationResult));
    }

    private static void addCriteriaReferenceInner(Element element, String criteriaReference) {

        addExtensionValueInner(
                element, MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE, new StringType(criteriaReference));
    }

    private static void addExtensionValueInner(Element element, String extensionUrl, Type extensionValue) {

        if (element.hasExtension(extensionUrl)) {
            element.getExtensionByUrl(extensionUrl).setValue(extensionValue);
        } else {
            element.addExtension(extensionUrl, extensionValue);
        }
    }
}
