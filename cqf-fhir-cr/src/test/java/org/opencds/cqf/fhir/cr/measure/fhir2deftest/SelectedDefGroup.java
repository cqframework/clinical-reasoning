package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;

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
public class SelectedDefGroup extends Selected<GroupDef, SelectedDef> {

    public SelectedDefGroup(GroupDef value, SelectedDef parent) {
        super(value, parent);
    }

    // ==================== Navigation Methods ====================

    /**
     * Navigate to a population by code (e.g., "numerator", "denominator").
     *
     * @param populationCode the population code
     * @return SelectedDefPopulation for the matching population
     * @throws AssertionError if no population with the given code is found
     */
    public SelectedDefPopulation population(String populationCode) {
        assertNotNull(value(), "GroupDef is null");
        PopulationDef population = value().populations().stream()
                .filter(p -> p.code() != null
                        && !p.code().isEmpty()
                        && p.code().first().code().equals(populationCode))
                .findFirst()
                .orElse(null);
        assertNotNull(population, "No population found with code: " + populationCode);
        return new SelectedDefPopulation(population, this);
    }

    /**
     * Navigate to a population by ID.
     *
     * @param id the population ID
     * @return SelectedDefPopulation for the matching population
     * @throws AssertionError if no population with the given ID is found
     */
    public SelectedDefPopulation populationById(String id) {
        assertNotNull(value(), "GroupDef is null");
        PopulationDef population = value().populations().stream()
                .filter(p -> id.equals(p.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(population, "No population found with ID: " + id);
        return new SelectedDefPopulation(population, this);
    }

    /**
     * Navigate to the first population in the group.
     *
     * @return SelectedDefPopulation for the first population
     * @throws AssertionError if no populations exist
     */
    public SelectedDefPopulation firstPopulation() {
        assertNotNull(value(), "GroupDef is null");
        assertFalse(value().populations().isEmpty(), "No populations found in GroupDef");
        return new SelectedDefPopulation(value().populations().get(0), this);
    }

    /**
     * Navigate to a stratifier by ID.
     *
     * @param stratifierId the stratifier ID
     * @return SelectedDefStratifier for the matching stratifier
     * @throws AssertionError if no stratifier with the given ID is found
     */
    public SelectedDefStratifier stratifier(String stratifierId) {
        assertNotNull(value(), "GroupDef is null");
        StratifierDef stratifier = value().stratifiers().stream()
                .filter(s -> stratifierId.equals(s.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(stratifier, "No stratifier found with ID: " + stratifierId);
        return new SelectedDefStratifier(stratifier, this);
    }

    /**
     * Navigate to the first stratifier in the group.
     *
     * @return SelectedDefStratifier for the first stratifier
     * @throws AssertionError if no stratifiers exist
     */
    public SelectedDefStratifier firstStratifier() {
        assertNotNull(value(), "GroupDef is null");
        assertFalse(value().stratifiers().isEmpty(), "No stratifiers found in GroupDef");
        return new SelectedDefStratifier(value().stratifiers().get(0), this);
    }

    // ==================== Assertion Methods ====================

    /**
     * Assert the number of populations in the group.
     *
     * @param count expected population count
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup hasPopulationCount(int count) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(count, value().populations().size(), "Population count mismatch");
        return this;
    }

    /**
     * Assert the number of stratifiers in the group.
     *
     * @param count expected stratifier count
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup hasStratifierCount(int count) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(count, value().stratifiers().size(), "Stratifier count mismatch");
        return this;
    }

    /**
     * Assert the group score value.
     *
     * @param score expected score
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup hasScore(Double score) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(score, value().getScore(), "Group score mismatch");
        return this;
    }

    /**
     * Assert that the group score is null (pre-scoring state).
     *
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup hasNullScore() {
        assertNotNull(value(), "GroupDef is null");
        assertNull(value().getScore(), "Expected null score (pre-scoring), but found: " + value().getScore());
        return this;
    }

    /**
     * Assert the measure scoring type.
     *
     * @param scoring expected MeasureScoring type
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup hasMeasureScoring(MeasureScoring scoring) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(scoring, value().measureScoring(), "MeasureScoring mismatch");
        return this;
    }

    /**
     * Assert the population basis.
     *
     * @param basis expected population basis ("boolean" or resource type)
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup hasPopulationBasis(String basis) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(basis, value().getPopulationBasis().code(), "Population basis mismatch");
        return this;
    }

    /**
     * Assert that the group has boolean basis.
     *
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup isBooleanBasis() {
        return hasPopulationBasis("boolean");
    }

    /**
     * Assert the group ID.
     *
     * @param id expected group ID
     * @return this SelectedDefGroup for chaining
     */
    public SelectedDefGroup hasGroupId(String id) {
        assertNotNull(value(), "GroupDef is null");
        assertEquals(id, value().id(), "Group ID mismatch");
        return this;
    }

    /**
     * Get the underlying GroupDef for advanced assertions.
     *
     * @return the GroupDef instance
     */
    public GroupDef groupDef() {
        return value();
    }
}
