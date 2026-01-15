package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
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
     * Get population count from a list of MeasureReportGroupPopulationComponents by population type.
     * Expect a single population only or will throw.
     *
     * @param populations the list of population components
     * @param measurePopulationType MeasurePopulationType to search for
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromGroupPopulationByPopulationType(
            List<MeasureReportGroupPopulationComponent> populations, MeasurePopulationType measurePopulationType) {

        final List<MeasureReportGroupPopulationComponent> filteredPopulations = populations.stream()
                .filter(population -> measurePopulationType
                        .toCode()
                        .equals(population.getCode().getCodingFirstRep().getCode()))
                .toList();

        if (filteredPopulations.isEmpty()) {
            return 0;
        }

        if (filteredPopulations.size() > 1) {
            throw new InvalidRequestException(
                    "Expected only a single population for this type, but found more than one for population type: %s"
                            .formatted(measurePopulationType));
        }

        return filteredPopulations.get(0).getCount();
    }

    /**
     * Get population count from a list of MeasureReportGroupPopulationComponents by population ID.
     * Expect a single population only or will throw.
     *
     * @param populations the list of population components
     * @param populationId ID of the specific population
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromGroupPopulationByPopulationId(
            List<MeasureReportGroupPopulationComponent> populations, String populationId) {

        final List<MeasureReportGroupPopulationComponent> filteredPopulations = populations.stream()
                .filter(population -> populationId.equals(population.getId()))
                .toList();

        if (filteredPopulations.isEmpty()) {
            return 0;
        }

        if (filteredPopulations.size() > 1) {
            throw new InvalidRequestException(
                    "Expected only a single population for this ID, but found more than one for population ID: %s"
                            .formatted(populationId));
        }

        return filteredPopulations.get(0).getCount();
    }

    /**
     * Get population count from a MeasureReportGroupComponent by population type.
     *
     * @param group the MeasureReportGroupComponent
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromGroupPopulationByPopulationType(
            MeasureReportGroupComponent group, MeasurePopulationType populationType) {
        return getCountFromGroupPopulationByPopulationType(group.getPopulation(), populationType);
    }

    public static int getCountFromGroupPopulationById(MeasureReportGroupComponent group, String populationId) {
        return getCountFromGroupPopulationByPopulationId(group.getPopulation(), populationId);
    }

    /**
     * Get population count from a list of StratifierGroupPopulationComponents by population code.
     *
     * @param populations the list of stratifier population components
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromStratumPopulationByType(
            List<StratifierGroupPopulationComponent> populations, MeasurePopulationType populationType) {

        final List<StratifierGroupPopulationComponent> matchingStratumPopulations = populations.stream()
                .filter(population -> populationType
                        .toCode()
                        .equals(population.getCode().getCodingFirstRep().getCode()))
                .toList();

        if (matchingStratumPopulations.isEmpty()) {
            return 0;
        }

        if (matchingStratumPopulations.size() > 1) {
            throw new InvalidRequestException(
                    "Got back more than one stratum population for population type: %s".formatted(populationType));
        }

        return matchingStratumPopulations.get(0).getCount();
    }

    /**
     * Get population count from a StratifierGroupComponent (stratum) by population type.
     *
     * @param stratum the StratifierGroupComponent (stratum)
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromStratumPopulationByType(
            StratifierGroupComponent stratum, MeasurePopulationType populationType) {
        return getCountFromStratumPopulationByType(stratum.getPopulation(), populationType);
    }

    public static int getCountFromStratumPopulationById(StratifierGroupComponent stratum, String populationId) {
        return getCountFromStratumPopulationById(stratum.getPopulation(), populationId);
    }

    public static int getCountFromStratumPopulationById(
            List<StratifierGroupPopulationComponent> populations, String populationId) {
        return populations.stream()
                .filter(population -> populationId.equals(population.getId()))
                .map(StratifierGroupPopulationComponent::getCount)
                .findAny()
                .orElse(0);
    }

    /**
     * Extract all population types from a MeasureReportGroupComponent.
     *
     * @param reportGroup the MeasureReportGroupComponent
     * @return an unmodifiable Set of MeasurePopulationTypes present in the group
     */
    public static Set<MeasurePopulationType> getPopulationTypes(MeasureReportGroupComponent reportGroup) {
        return reportGroup.getPopulation().stream()
                .map(MeasureReportGroupPopulationComponent::getCode)
                .map(CodeableConcept::getCodingFirstRep)
                .map(Coding::getCode)
                .map(MeasurePopulationType::fromCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Check if a MeasureReportGroupPopulationComponent matches a specific population type.
     *
     * @param groupPopulation the MeasureReportGroupPopulationComponent
     * @param populationType the MeasurePopulationType to match
     * @return true if the population type matches, false otherwise
     */
    public static boolean doesPopulationTypeMatch(
            MeasureReportGroupPopulationComponent groupPopulation, MeasurePopulationType populationType) {
        return populationType
                .toCode()
                .equals(groupPopulation.getCode().getCodingFirstRep().getCode());
    }

    /**
     * Check if a StratifierGroupPopulationComponent matches a specific population type.
     *
     * @param stratifierPopulation the StratifierGroupPopulationComponent
     * @param populationType the MeasurePopulationType to match
     * @return true if the population type matches, false otherwise
     */
    public static boolean doesPopulationTypeMatch(
            StratifierGroupPopulationComponent stratifierPopulation, MeasurePopulationType populationType) {
        return populationType
                .toCode()
                .equals(stratifierPopulation.getCode().getCodingFirstRep().getCode());
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

    public static boolean hasAggregateMethod(
            String measureUrl,
            MeasureReportGroupPopulationComponent groupPopulation,
            ContinuousVariableObservationAggregateMethod aggregateMethod) {
        return aggregateMethod == getAggregateMethod(measureUrl, groupPopulation);
    }

    public static ContinuousVariableObservationAggregateMethod getAggregateMethod(
            String measureUrl, @Nullable MeasureReportGroupPopulationComponent groupPopulation) {

        if (groupPopulation == null) {
            return ContinuousVariableObservationAggregateMethod.N_A;
        }

        var aggMethodExt = groupPopulation.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        if (aggMethodExt != null) {
            // this method is only required if scoringType = continuous-variable or Ratio Continuous variable
            var aggregateMethodString = aggMethodExt.getValue().toString();

            var aggregateMethod = ContinuousVariableObservationAggregateMethod.fromString(aggregateMethodString);

            // check that method is accepted
            if (aggregateMethod == null) {
                throw new InvalidRequestException("Aggregation method: %s is not a valid value for Measure: %s"
                        .formatted(aggregateMethodString, measureUrl));
            }

            return aggregateMethod;
        }

        return ContinuousVariableObservationAggregateMethod.N_A;
    }

    public static BigDecimal getAggregationResult(MeasureReportGroupPopulationComponent reportPopulation) {
        return Optional.ofNullable(reportPopulation.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT))
                .map(Extension::getValue)
                .filter(DecimalType.class::isInstance)
                .map(DecimalType.class::cast)
                .map(DecimalType::getValue)
                .orElse(BigDecimal.ZERO);
    }

    public static void addAggregationResultAndMethod(
            MeasureReportGroupPopulationComponent reportPopulation, PopulationDef populationDef) {

        addAggregationResultAndMethod(
                reportPopulation, populationDef.getAggregateMethod(), populationDef.getAggregationResult());
    }

    public static void addAggregationResultAndMethod(
            MeasureReportGroupPopulationComponent measurePopulation,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod,
            @Nullable BigDecimal aggregationResult) {

        addAggregationResultAndMethod(
                measurePopulation,
                aggregateMethod,
                Optional.ofNullable(aggregationResult)
                        .map(BigDecimal::doubleValue)
                        .orElse(null));
    }

    public static void addAggregationResultAndMethod(
            MeasureReportGroupPopulationComponent measurePopulation,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod,
            @Nullable Double aggregationResult) {

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

    private static void addExtensionValueInner(
            MeasureReportGroupPopulationComponent measurePopulation, String extensionUrl, Type extensionValue) {

        if (measurePopulation.hasExtension(extensionUrl)) {
            measurePopulation.getExtensionByUrl(extensionUrl).setValue(extensionValue);
        } else {
            measurePopulation.addExtension(extensionUrl, extensionValue);
        }
    }
}
