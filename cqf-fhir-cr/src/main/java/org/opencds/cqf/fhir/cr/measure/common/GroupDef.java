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

    // Added by Claude Sonnet 4.5 on 2025-12-03
    // Mutable score field for version-agnostic scoring
    private Double score;

    public GroupDef(
            String id,
            ConceptDef code,
            List<StratifierDef> stratifiers,
            List<PopulationDef> populations,
            MeasureScoring measureScoring,
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
    public PopulationDef getFirstWithId(MeasurePopulationType type) {
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
     * Added by Claude Sonnet 4.5 on 2025-12-03
     * Get the computed score for this group.
     * Used by version-agnostic MeasureDefScorer.
     *
     * @return the score, or null if not yet computed
     */
    public Double getScore() {
        return this.score;
    }

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-03
     * Set the computed score for this group.
     * Used by version-agnostic MeasureDefScorer to store computed scores.
     *
     * @param score the computed score
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-03
     * Get the measure score adjusted for improvement notation.
     * <p>
     * Similar to R4MeasureReportScorer#scoreGroup(Double, boolean, MeasureReportGroupComponent),
     * this method:
     * <ul>
     *   <li>Returns null if score is null</li>
     *   <li>Returns null if score is negative (invalid score scenario)</li>
     *   <li>Returns score as-is if improvement notation is "increase"</li>
     *   <li>Returns (1 - score) if improvement notation is "decrease"</li>
     * </ul>
     *
     * @return the adjusted score, or null if score is null or negative
     */
    public Double getMeasureScore() {
        // When applySetMembership=false, this value can receive strange values
        // This should prevent scoring in certain scenarios like <0
        if (this.score != null && this.score >= 0) {
            if (isIncreaseImprovementNotation()) {
                return this.score;
            } else {
                return 1 - this.score;
            }
        }
        return null;
    }

    /**
     * Creates a deep copy snapshot of this GroupDef including mutable score field.
     * <p>
     * Recursively snapshots all stratifiers and populations while preserving the
     * current score value. The populationIndex is automatically rebuilt from the
     * snapshotted populations in the constructor.
     *
     * @return A new GroupDef instance with deep copied collections and copied score
     */
    public GroupDef createSnapshot() {
        // Deep copy stratifiers (each recursively snapshots stratum)
        List<StratifierDef> snapshotStratifiers =
                stratifiers.stream().map(StratifierDef::createSnapshot).toList();

        // Deep copy populations (includes Maps/Sets of subject resources)
        List<PopulationDef> snapshotPopulations =
                populations.stream().map(PopulationDef::createSnapshot).toList();

        // Create new GroupDef with snapshot collections
        // populationIndex will be automatically created from snapshotPopulations
        GroupDef snapshot = new GroupDef(
                id,
                code,
                snapshotStratifiers,
                snapshotPopulations,
                measureScoring,
                isGroupImpNotation,
                improvementNotation,
                populationBasis);

        // Copy mutable score field (added 2025-12-03 for version-agnostic scoring)
        snapshot.score = this.score;

        return snapshot;
    }
}
