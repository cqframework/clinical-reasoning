package org.opencds.cqf.fhir.cr.measure.common.def.report;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.def.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.def.measure.GroupDef;

@SuppressWarnings("squid:S107")
public class GroupReportDef {

    private final GroupDef groupDef; // Reference to immutable structure
    private final List<StratifierReportDef> stratifiers; // Converted from GroupDef for evaluation
    private final List<PopulationReportDef> populations; // Converted from GroupDef for evaluation
    private final Map<MeasurePopulationType, List<PopulationReportDef>> populationIndex; // Evaluation-only state

    // Added by Claude Sonnet 4.5 on 2025-12-03
    // Mutable score field for version-agnostic scoring
    private Double score; // Evaluation-only state

    /**
     * Factory method to create GroupReportDef from immutable GroupDef.
     * The GroupReportDef will have empty mutable state (score initially null).
     */
    public static GroupReportDef fromGroupDef(GroupDef groupDef) {
        List<StratifierReportDef> stratifierReportDefs = groupDef.stratifiers().stream()
                .map(StratifierReportDef::fromStratifierDef)
                .toList();
        List<PopulationReportDef> populationReportDefs = groupDef.populations().stream()
                .map(PopulationReportDef::fromPopulationDef)
                .toList();
        return new GroupReportDef(groupDef, stratifierReportDefs, populationReportDefs);
    }

    /**
     * Constructor for creating GroupReportDef with a GroupDef reference.
     * This is the primary constructor for production use.
     */
    public GroupReportDef(
            GroupDef groupDef, List<StratifierReportDef> stratifiers, List<PopulationReportDef> populations) {
        this.groupDef = groupDef;
        this.stratifiers = stratifiers;
        this.populations = populations;
        this.populationIndex = index(populations);
    }

    /**
     * Test-only constructor for creating GroupReportDef with explicit structural data.
     * Creates a minimal GroupDef internally. Use fromGroupDef() for production code.
     */
    public GroupReportDef(
            String id,
            ConceptDef code,
            List<StratifierReportDef> stratifiers,
            List<PopulationReportDef> populations,
            MeasureScoring measureScoring,
            boolean isGroupImprovementNotation,
            CodeDef improvementNotation,
            CodeDef populationBasis) {
        this.groupDef = new GroupDef(
                id,
                code,
                Collections.emptyList(),
                Collections.emptyList(),
                measureScoring,
                isGroupImprovementNotation,
                improvementNotation,
                populationBasis);
        this.stratifiers = stratifiers;
        this.populations = populations;
        this.populationIndex = index(populations);
    }

    /**
     * Accessor for the immutable structural definition.
     */
    public GroupDef groupDef() {
        return this.groupDef;
    }

    // Delegate structural queries to groupDef
    public String id() {
        return groupDef.id();
    }

    public ConceptDef code() {
        return groupDef.code();
    }

    public List<StratifierReportDef> stratifiers() {
        return this.stratifiers;
    }

    public List<PopulationReportDef> populations() {
        return this.populations;
    }

    public boolean hasPopulationType(MeasurePopulationType populationType) {
        var populationDefType = this.populations.stream()
                .map(PopulationReportDef::type)
                .filter(x -> x.equals(populationType))
                .findFirst()
                .orElse(null);
        return populationDefType != null && populationDefType.equals(populationType);
    }

    public PopulationReportDef getSingle(MeasurePopulationType type) {
        if (!populationIndex.containsKey(type)) {
            return null;
        }

        List<PopulationReportDef> defs = this.populationIndex.get(type);
        if (defs.size() > 1) {
            throw new IllegalStateException("There is more than one PopulationDef of type: " + type.toCode());
        }

        return defs.get(0);
    }

    public List<PopulationReportDef> getPopulationDefs(MeasurePopulationType type) {
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
    public PopulationReportDef getFirstWithId(MeasurePopulationType type) {
        return this.getPopulationDefs(type).stream()
                .findFirst()
                .filter(pop -> pop.id() != null)
                .orElse(null);
    }

    // Extracted from R4MeasureReportBuilder.getReportPopulation() by Claude Sonnet 4.5
    public PopulationReportDef findPopulationByType(MeasurePopulationType type) {
        return this.populations.stream()
                .filter(e -> e.code().first().code().equals(type.toCode()))
                .findAny()
                .orElse(null);
    }

    // Extracted from R4MeasureReportBuilder.buildGroup() loop by Claude Sonnet 4.5
    public PopulationReportDef findPopulationById(String id) {
        return this.populations.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private Map<MeasurePopulationType, List<PopulationReportDef>> index(List<PopulationReportDef> populations) {
        return populations.stream().collect(Collectors.groupingBy(PopulationReportDef::type));
    }

    public MeasureScoring measureScoring() {
        return groupDef.measureScoring();
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
        return groupDef.isGroupImprovementNotation();
    }

    public boolean isBooleanBasis() {
        return getPopulationBasis().code().equals("boolean");
    }

    public CodeDef getPopulationBasis() {
        return groupDef.getPopulationBasis();
    }

    public CodeDef getImprovementNotation() {
        return groupDef.getImprovementNotation();
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
                .map(PopulationReportDef::getCount)
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
}
