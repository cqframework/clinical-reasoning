package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.def.report.GroupReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.PopulationReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratifierReportDef;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;

/**
 * Fluent assertion API for GroupDef objects.
 * <p>
 * Provides assertions and navigation for measure groups, including population
 * and stratifier navigation, score validation, and group metadata checks.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * .def()
 *     .firstGroup()
 *         .hasNullScore()  // Pre-scoring
 *         .hasPopulationCount(4)
 *         .hasMeasureScoring(MeasureScoring.PROPORTION)
 *         .hasPopulationBasis("boolean")
 *         .population("numerator")
 *             .hasSubjectCount(7)
 *         .up()
 *         .stratifier("gender-stratifier")
 *             .hasStratumCount(2);
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class SelectedMeasureDefGroup<P> extends Measure.Selected<GroupReportDef, P> {

    public SelectedMeasureDefGroup(GroupReportDef value, P parent) {
        super(value, parent);
    }

    // ==================== Navigation Methods ====================

    /**
     * Navigate to a population by code (e.g., "numerator", "denominator").
     *
     * @param populationCode the population code
     * @return SelectedMeasureDefPopulation for the matching population
     * @throws AssertionError if no population with the given code is found
     */
    public SelectedMeasureDefPopulation<SelectedMeasureDefGroup<P>> population(String populationCode) {
        assertNotNull(value(), "GroupDef is null");
        PopulationReportDef population = value().populations().stream()
                .filter(p -> p.code() != null
                        && !p.code().isEmpty()
                        && p.code().first().code().equals(populationCode))
                .findFirst()
                .orElse(null);
        assertNotNull(population, "No population found with code: " + populationCode);
        return new SelectedMeasureDefPopulation<>(population, this);
    }

    /**
     * Navigate to a population by ID.
     *
     * @param id the population ID
     * @return SelectedMeasureDefPopulation for the matching population
     * @throws AssertionError if no population with the given ID is found
     */
    public SelectedMeasureDefPopulation<SelectedMeasureDefGroup<P>> populationById(String id) {
        assertNotNull(value(), "GroupDef is null");
        PopulationReportDef population = value().populations().stream()
                .filter(p -> id.equals(p.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(population, "No population found with ID: " + id);
        return new SelectedMeasureDefPopulation<>(population, this);
    }

    /**
     * Navigate to the first population in the group.
     *
     * @return SelectedMeasureDefPopulation for the first population
     * @throws AssertionError if no populations exist
     */
    public SelectedMeasureDefPopulation<SelectedMeasureDefGroup<P>> firstPopulation() {
        assertNotNull(value(), "GroupDef is null");
        assertFalse(value().populations().isEmpty(), "No populations found in GroupDef");
        return new SelectedMeasureDefPopulation<>(value().populations().get(0), this);
    }

    /**
     * Navigate to a stratifier by ID.
     *
     * @param stratifierId the stratifier ID
     * @return SelectedMeasureDefStratifier for the matching stratifier
     * @throws AssertionError if no stratifier with the given ID is found
     */
    public SelectedMeasureDefStratifier<SelectedMeasureDefGroup<P>> stratifier(String stratifierId) {
        assertNotNull(value(), "GroupDef is null");
        StratifierReportDef stratifier = value().stratifiers().stream()
                .filter(s -> stratifierId.equals(s.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(stratifier, "No stratifier found with ID: " + stratifierId);
        return new SelectedMeasureDefStratifier<>(stratifier, this);
    }

    /**
     * Navigate to the first stratifier in the group.
     *
     * @return SelectedMeasureDefStratifier for the first stratifier
     * @throws AssertionError if no stratifiers exist
     */
    public SelectedMeasureDefStratifier<SelectedMeasureDefGroup<P>> firstStratifier() {
        assertNotNull(value(), "GroupDef is null");
        assertFalse(value().stratifiers().isEmpty(), "No stratifiers found in GroupDef");
        return new SelectedMeasureDefStratifier<>(value().stratifiers().get(0), this);
    }

    // ==================== Assertion Methods ====================

    /**
     * Assert the number of populations in the group.
     *
     * @param count expected population count
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> hasPopulationCount(int count) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(count, value().populations().size(), "Population count mismatch");
        return this;
    }

    /**
     * Assert the number of stratifiers in the group.
     *
     * @param count expected stratifier count
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> hasStratifierCount(int count) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(count, value().stratifiers().size(), "Stratifier count mismatch");
        return this;
    }

    /**
     * Assert the group score value.
     *
     * @param score expected score
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> hasScore(Double score) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(score, value().getScore(), "Group score mismatch");
        return this;
    }

    /**
     * Assert that the group score is null (pre-scoring state).
     *
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> hasNullScore() {
        assertNotNull(value(), "GroupDef is null");
        assertNull(value().getScore(), "Expected null score (pre-scoring), but found: " + value().getScore());
        return this;
    }

    /**
     * Assert the measure scoring type.
     *
     * @param scoring expected MeasureScoring type
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> hasMeasureScoring(MeasureScoring scoring) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(scoring, value().measureScoring(), "MeasureScoring mismatch");
        return this;
    }

    /**
     * Assert the population basis.
     *
     * @param basis expected population basis ("boolean" or resource type)
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> hasPopulationBasis(String basis) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(basis, value().getPopulationBasis().code(), "Population basis mismatch");
        return this;
    }

    /**
     * Assert that the group has boolean basis.
     *
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> isBooleanBasis() {
        return hasPopulationBasis("boolean");
    }

    /**
     * Assert the group ID.
     *
     * @param id expected group ID
     * @return this SelectedMeasureDefGroup for chaining
     */
    public SelectedMeasureDefGroup<P> hasGroupId(String id) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(id, value().id(), "Group ID mismatch");
        return this;
    }

    /**
     * Get the underlying GroupDef for advanced assertions.
     *
     * @return the GroupDef instance
     */
    public GroupReportDef groupDef() {
        return value();
    }
}
