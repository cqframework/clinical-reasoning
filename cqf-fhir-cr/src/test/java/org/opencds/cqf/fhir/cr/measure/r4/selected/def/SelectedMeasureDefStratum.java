package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opencds.cqf.fhir.cr.measure.common.def.report.StratumPopulationReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratumReportDef;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;

/**
 * Fluent assertion API for StratumDef objects.
 * <p>
 * Provides assertions and navigation for individual strata within stratifiers,
 * including population navigation, subject counts, scores, and value definitions.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * .firstStratum()
 *     .hasSubjectCount(3)
 *     .hasNullScore()  // Pre-scoring
 *     .hasValueDef("male")
 *     .population("numerator")
 *         .hasCount(2)
 *         .hasSubjects("Patient/1", "Patient/2");
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class SelectedMeasureDefStratum<P> extends Measure.Selected<StratumReportDef, P> {

    public SelectedMeasureDefStratum(StratumReportDef value, P parent) {
        super(value, parent);
    }

    // ==================== Navigation Methods ====================

    /**
     * Navigate to a stratum population by code (e.g., "numerator").
     *
     * @param populationCode the population code
     * @return SelectedMeasureDefStratumPopulation for the matching population
     * @throws AssertionError if no population with the given code is found
     */
    public SelectedMeasureDefStratumPopulation<SelectedMeasureDefStratum<P>> population(String populationCode) {
        assertNotNull(value(), "StratumDef is null");
        StratumPopulationReportDef population = value().stratumPopulations().stream()
                .filter(p -> p.populationDef() != null
                        && p.populationDef().code() != null
                        && !p.populationDef().code().isEmpty()
                        && p.populationDef().code().first().code().equals(populationCode))
                .findFirst()
                .orElse(null);
        assertNotNull(population, "No stratum population found with code: " + populationCode);
        return new SelectedMeasureDefStratumPopulation<>(population, this);
    }

    /**
     * Navigate to a stratum population by ID.
     *
     * @param id the population ID
     * @return SelectedMeasureDefStratumPopulation for the matching population
     * @throws AssertionError if no population with the given ID is found
     */
    public SelectedMeasureDefStratumPopulation<SelectedMeasureDefStratum<P>> populationById(String id) {
        assertNotNull(value(), "StratumDef is null");
        StratumPopulationReportDef population = value().stratumPopulations().stream()
                .filter(p -> id.equals(p.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(population, "No stratum population found with ID: " + id);
        return new SelectedMeasureDefStratumPopulation<>(population, this);
    }

    /**
     * Navigate to the first stratum population.
     *
     * @return SelectedMeasureDefStratumPopulation for the first population
     * @throws AssertionError if no populations exist
     */
    public SelectedMeasureDefStratumPopulation<SelectedMeasureDefStratum<P>> firstPopulation() {
        assertNotNull(value(), "StratumDef is null");
        assertFalse(value().stratumPopulations().isEmpty(), "No populations found in StratumDef");
        return new SelectedMeasureDefStratumPopulation<>(
                value().stratumPopulations().get(0), this);
    }

    // ==================== Assertion Methods ====================

    /**
     * Assert the number of populations in this stratum.
     *
     * @param count expected population count
     * @return this SelectedMeasureDefStratum for chaining
     */
    public SelectedMeasureDefStratum<P> hasPopulationCount(int count) {
        assertNotNull(value(), "StratumDef is null");
        assertEquals(count, value().stratumPopulations().size(), "Stratum population count mismatch");
        return this;
    }

    /**
     * Assert the number of subjects in this stratum (across all populations).
     *
     * @param count expected subject count
     * @return this SelectedMeasureDefStratum for chaining
     */
    public SelectedMeasureDefStratum<P> hasSubjectCount(int count) {
        assertNotNull(value(), "StratumDef is null");
        assertEquals(count, value().subjectIds().size(), "Stratum subject count mismatch");
        return this;
    }

    /**
     * Assert the stratum score value.
     *
     * @param score expected score
     * @return this SelectedMeasureDefStratum for chaining
     */
    public SelectedMeasureDefStratum<P> hasScore(Double score) {
        assertNotNull(value(), "StratumDef is null");
        assertEquals(score, value().getScore(), "Stratum score mismatch");
        return this;
    }

    /**
     * Assert that the stratum score is null (pre-scoring state).
     *
     * @return this SelectedMeasureDefStratum for chaining
     */
    public SelectedMeasureDefStratum<P> hasNullScore() {
        assertNotNull(value(), "StratumDef is null");
        assertNull(value().getScore(), "Expected null score (pre-scoring), but found: " + value().getScore());
        return this;
    }

    /**
     * Assert the stratum value definition text (e.g., "male", "female").
     *
     * @param valueText expected value text
     * @return this SelectedMeasureDefStratum for chaining
     */
    public SelectedMeasureDefStratum<P> hasValueDef(String valueText) {
        assertNotNull(value(), "StratumDef is null");
        assertNotNull(value().valueDefs(), "No value definitions found");
        boolean found = value().valueDefs().stream()
                .anyMatch(vd -> valueText.equals(vd.value().getDescription()));
        assertTrue(found, "No value definition found with text: " + valueText);
        return this;
    }

    /**
     * Assert that this is a component stratum (has multiple value definitions).
     *
     * @return this SelectedMeasureDefStratum for chaining
     */
    public SelectedMeasureDefStratum<P> isComponentStratum() {
        assertNotNull(value(), "StratumDef is null");
        assertNotNull(value().valueDefs(), "No value definitions found");
        assertTrue(
                value().valueDefs().size() > 1,
                "Expected component stratum with multiple value defs, but found: "
                        + value().valueDefs().size());
        return this;
    }

    /**
     * Get the underlying StratumDef for advanced assertions.
     *
     * @return the StratumDef instance
     */
    public StratumReportDef stratumDef() {
        return value();
    }
}
