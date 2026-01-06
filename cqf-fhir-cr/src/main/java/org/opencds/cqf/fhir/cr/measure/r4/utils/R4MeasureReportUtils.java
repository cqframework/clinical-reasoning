package org.opencds.cqf.fhir.cr.measure.r4.utils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueWrapper;

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
     * Get population count from a list of MeasureReportGroupPopulationComponents by population code.
     *
     * @param populations the list of population components
     * @param populationCode the population code to find (e.g., "numerator", "denominator")
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromGroupPopulation(
            List<MeasureReportGroupPopulationComponent> populations, String populationCode) {
        return populations.stream()
                .filter(population -> populationCode.equals(
                        population.getCode().getCodingFirstRep().getCode()))
                .map(MeasureReportGroupPopulationComponent::getCount)
                .findAny()
                .orElse(0);
    }

    /**
     * Get population count from a list of MeasureReportGroupPopulationComponents by population type.
     *
     * @param populations the list of population components
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromGroupPopulation(
            List<MeasureReportGroupPopulationComponent> populations, MeasurePopulationType populationType) {
        return getCountFromGroupPopulation(populations, populationType.toCode());
    }

    /**
     * Get population count from a MeasureReportGroupComponent by population type.
     *
     * @param group the MeasureReportGroupComponent
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromGroupPopulation(
            MeasureReportGroupComponent group, MeasurePopulationType populationType) {
        return getCountFromGroupPopulation(group.getPopulation(), populationType);
    }

    /**
     * Get population count from a list of StratifierGroupPopulationComponents by population code.
     *
     * @param populations the list of stratifier population components
     * @param populationCode the population code to find (e.g., "numerator", "denominator")
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromStratifierPopulation(
            List<StratifierGroupPopulationComponent> populations, String populationCode) {
        return populations.stream()
                .filter(population -> populationCode.equals(
                        population.getCode().getCodingFirstRep().getCode()))
                .map(StratifierGroupPopulationComponent::getCount)
                .findAny()
                .orElse(0);
    }

    /**
     * Get population count from a list of StratifierGroupPopulationComponents by population type.
     *
     * @param populations the list of stratifier population components
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromStratifierPopulation(
            List<StratifierGroupPopulationComponent> populations, MeasurePopulationType populationType) {
        return getCountFromStratifierPopulation(populations, populationType.toCode());
    }

    /**
     * Get population count from a StratifierGroupComponent (stratum) by population type.
     *
     * @param stratum the StratifierGroupComponent (stratum)
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public static int getCountFromStratifierPopulation(
            StratifierGroupComponent stratum, MeasurePopulationType populationType) {
        return getCountFromStratifierPopulation(stratum.getPopulation(), populationType);
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

    /**
     * Helper method to convert expression result to CodeableConcept for stratum text extraction.
     * Used internally by getStratumDefText for non-CodeableConcept values.
     *
     * @param value the StratumValueWrapper to convert
     * @return a CodeableConcept with the text set to the string value
     */
    private static CodeableConcept expressionResultToCodableConcept(StratumValueWrapper value) {
        return new CodeableConcept().setText(value.getValueAsString());
    }
}
