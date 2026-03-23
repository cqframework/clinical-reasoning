package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S107")
public class GroupDef {

    private final String id;
    private final ConceptDef code;
    private final List<StratifierDef> stratifiers;
    private final List<PopulationDef> populations;
    private final MeasureScoring measureScoring;
    private final boolean isGroupImpNotation;
    private final CodeDef populationBasis;
    private final CodeDef improvementNotation;
    private final Map<MeasurePopulationType, List<PopulationDef>> populationIndex;

    @Nullable
    private final String description;

    public GroupDef(
            String id,
            ConceptDef code,
            List<StratifierDef> stratifiers,
            List<PopulationDef> populations,
            MeasureScoring measureScoring,
            boolean isGroupImprovementNotation,
            CodeDef improvementNotation,
            CodeDef populationBasis) {
        this(
                id,
                code,
                stratifiers,
                populations,
                measureScoring,
                isGroupImprovementNotation,
                improvementNotation,
                populationBasis,
                null);
    }

    public GroupDef(
            String id,
            ConceptDef code,
            List<StratifierDef> stratifiers,
            List<PopulationDef> populations,
            MeasureScoring measureScoring,
            boolean isGroupImprovementNotation,
            CodeDef improvementNotation,
            CodeDef populationBasis,
            @Nullable String description) {
        //
        this.id = id;
        this.code = code;
        this.stratifiers = stratifiers;
        this.populations = populations;
        this.populationIndex = index(populations);
        this.measureScoring = measureScoring;
        this.isGroupImpNotation = isGroupImprovementNotation;
        this.improvementNotation = improvementNotation;
        this.populationBasis = populationBasis;
        this.description = description;
    }

    public String id() {
        return this.id;
    }

    public ConceptDef code() {
        return this.code;
    }

    public List<StratifierDef> stratifiers() {
        return this.stratifiers;
    }

    public List<PopulationDef> populations() {
        return this.populations;
    }

    public boolean hasPopulationType(MeasurePopulationType populationType) {
        var populationDefType = this.populations.stream()
                .map(PopulationDef::type)
                .filter(x -> x.equals(populationType))
                .findFirst()
                .orElse(null);
        return populationDefType != null && populationDefType.equals(populationType);
    }

    public PopulationDef getSingle(MeasurePopulationType type) {
        if (!populationIndex.containsKey(type)) {
            return null;
        }

        List<PopulationDef> defs = this.populationIndex.get(type);
        if (defs.size() > 1) {
            throw new IllegalStateException("There is more than one PopulationDef of type: " + type.toCode());
        }

        return defs.get(0);
    }

    public List<PopulationDef> getPopulationDefs(MeasurePopulationType type) {
        return this.populationIndex.computeIfAbsent(type, x -> Collections.emptyList());
    }

    /**
     * Get the first population of the specified type, but only if it has a non-null ID.
     * Returns null if the first population doesn't have an ID.
     * Used for finding populations that can be referenced by criteriaReference.
     *
     * @param type the population type to find
     * @return the first PopulationDef of the specified type if it has a non-null ID, or null otherwise
     */
    @Nullable
    public PopulationDef getFirstWithTypeAndNonNullId(MeasurePopulationType type) {
        return this.getPopulationDefs(type).stream()
                .findFirst()
                .filter(pop -> pop.id() != null)
                .orElse(null);
    }

    // Extracted from R4MeasureReportBuilder.getReportPopulation() by Claude Sonnet 4.5
    public PopulationDef findPopulationByType(MeasurePopulationType type) {
        return this.populations.stream()
                .filter(e -> e.code().first().code().equals(type.toCode()))
                .findAny()
                .orElse(null);
    }

    // Extracted from R4MeasureReportBuilder.buildGroup() loop by Claude Sonnet 4.5
    public PopulationDef findPopulationById(String id) {
        return this.populations.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find MEASUREOBSERVATION PopulationDef that references the specified population type.
     * Used for ratio observation scoring to locate numerator/denominator observations.
     * <p>
     * Added by Claude Sonnet 4.5 on 2025-12-05 for ratio observation optimization.
     *
     * @param measureObservationPopulationDefs list of MEASUREOBSERVATION populations
     * @param targetType the population type (NUMERATOR or DENOMINATOR) to find
     * @return matching PopulationDef or null
     */
    @Nullable
    public PopulationDef findRatioObservationPopulationDef(
            List<PopulationDef> measureObservationPopulationDefs, MeasurePopulationType targetType) {

        PopulationDef referencedPopulation = this.getFirstWithTypeAndNonNullId(targetType);
        if (referencedPopulation == null) {
            return null;
        }

        String targetId = referencedPopulation.id();
        return measureObservationPopulationDefs.stream()
                .filter(obs -> obs.getCriteriaReference() != null
                        && obs.getCriteriaReference().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    private Map<MeasurePopulationType, List<PopulationDef>> index(List<PopulationDef> populations) {
        return populations.stream().collect(Collectors.groupingBy(PopulationDef::type));
    }

    public MeasureScoring measureScoring() {
        return this.measureScoring;
    }

    public boolean isIncreaseImprovementNotation() {
        if (getImprovementNotation() != null) {
            return getImprovementNotation().code().equals("increase");
        } else {
            // default response if null
            return true;
        }
    }

    public boolean isGroupImprovementNotation() {
        return this.isGroupImpNotation;
    }

    public boolean isBooleanBasis() {
        return getPopulationBasis().code().equals("boolean");
    }

    public CodeDef getPopulationBasis() {
        return this.populationBasis;
    }

    public CodeDef getImprovementNotation() {
        return this.improvementNotation;
    }

    @Nullable
    public String description() {
        return this.description;
    }
}
