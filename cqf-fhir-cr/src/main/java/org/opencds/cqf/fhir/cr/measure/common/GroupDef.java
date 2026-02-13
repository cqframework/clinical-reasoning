package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
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

    @Nullable
    private final MeasureScoring measureScoring;

    private final boolean isGroupImpNotation;
    private final CodeDef populationBasis;
    private final CodeDef improvementNotation;
    private final Map<MeasurePopulationType, List<PopulationDef>> populationIndex;

    // Added by Claude Sonnet 4.5 on 2025-12-03
    // Mutable score field for version-agnostic scoring
    private Double score;

    public GroupDef(
            String id,
            ConceptDef code,
            List<StratifierDef> stratifiers,
            List<PopulationDef> populations,
            @Nullable MeasureScoring measureScoring,
            boolean isGroupImprovementNotation,
            CodeDef improvementNotation,
            CodeDef populationBasis) {
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

    public boolean hasMeasureScoring() {
        return this.measureScoring != null;
    }

    /**
     * This method should only be called from production code that builds a MeasureReport group,
     * setting the scoring extension on that group.
     *
     * @return MeasureScoring associated directly with the group, if it exists at all.
     */
    @Nullable
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

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-02
     * Get the count for a specific population type in this group.
     * Moved from R4MeasureReportScorer to make it reusable across FHIR versions.
     *
     * @param populationType the MeasurePopulationType to find
     * @return the count for the population, or 0 if not found
     */
    public int getPopulationCount(MeasurePopulationType populationType) {
        return this.populations.stream()
                .filter(pop -> pop.type() == populationType)
                .findFirst()
                .map(PopulationDef::getCount)
                .orElse(0);
    }

    /**
     * Get the computed score for this group.
     * Used by version-agnostic MeasureDefScorer.
     *
     * @return the score, or null if not yet computed
     */
    public Double getScore() {
        return this.score;
    }

    public void setScoreAndAdaptToImprovementNotation(Double originalScore, MeasureScoring measureOrGroupScoring) {
        if ((MeasureScoring.RATIO == measureOrGroupScoring
                        && hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION))
                || MeasureScoring.PROPORTION == measureOrGroupScoring) {
            this.score = MeasureScoreCalculator.scoreGroupAccordingToIncreaseImprovementNotation(
                    originalScore, isIncreaseImprovementNotation());
        } else {
            this.score = originalScore;
        }
    }

    public MeasureScoring getMeasureOrGroupScoring(MeasureDef measureDef) {
        if (measureDef.hasMeasureScoring()) {
            return measureDef.measureScoring();
        }

        if (hasMeasureScoring()) {
            return measureScoring();
        }

        throw new InternalErrorException("Must have scoring either at the measure or group level for measure URL: %s"
                .formatted(measureDef.url()));
    }
}
