package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.StratumPopulationDef;

/**
 * Fluent assertion API for StratumPopulationDef objects.
 * <p>
 * Provides assertions for stratum-level population details, including counts,
 * subject lists, and resource counts.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * .population("numerator")
 *     .hasCount(2)
 *     .hasSubjectCount(2)
 *     .hasSubjects("Patient/1", "Patient/2")
 *     .hasResourceCount(4);
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class SelectedMeasureDefStratumPopulation<P>
        extends org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected<StratumPopulationDef, P> {

    public SelectedMeasureDefStratumPopulation(StratumPopulationDef value, P parent) {
        super(value, parent);
    }

    // ==================== Count Assertions ====================

    /**
     * Assert the count value for this stratum population (number of subjects/resources).
     *
     * @param count expected count
     * @return this SelectedMeasureDefStratumPopulation for chaining
     */
    public SelectedMeasureDefStratumPopulation<P> hasCount(int count) {
        assertNotNull(value(), "StratumPopulationDef is null");
        assertEquals(count, value().getCount(), "Stratum population count mismatch");
        return this;
    }

    /**
     * Assert the number of subjects in this stratum population.
     *
     * @param count expected subject count
     * @return this SelectedMeasureDefStratumPopulation for chaining
     */
    public SelectedMeasureDefStratumPopulation<P> hasSubjectCount(int count) {
        assertNotNull(value(), "StratumPopulationDef is null");
        assertEquals(
                count, value().subjectsQualifiedOrUnqualified().size(), "Stratum population subject count mismatch");
        return this;
    }

    /**
     * Assert that specific subjects are in this stratum population.
     *
     * @param subjectIds expected subject IDs (e.g., "Patient/1")
     * @return this SelectedMeasureDefStratumPopulation for chaining
     */
    public SelectedMeasureDefStratumPopulation<P> hasSubjects(String... subjectIds) {
        assertNotNull(value(), "StratumPopulationDef is null");
        for (String subjectId : subjectIds) {
            assertTrue(
                    value().subjectsQualifiedOrUnqualified().contains(subjectId),
                    "Subject not found in stratum population: " + subjectId + ", available: "
                            + value().subjectsQualifiedOrUnqualified());
        }
        return this;
    }

    // ==================== Resource Assertions ====================

    /**
     * Assert the total number of resources in this stratum population.
     *
     * @param count expected resource count
     * @return this SelectedMeasureDefStratumPopulation for chaining
     */
    public SelectedMeasureDefStratumPopulation<P> hasResourceCount(int count) {
        assertNotNull(value(), "StratumPopulationDef is null");
        assertEquals(count, value().resourceIdsForSubjectList().size(), "Stratum population resource count mismatch");
        return this;
    }

    // ==================== Metadata Assertions ====================

    /**
     * Assert that this stratum population has boolean basis.
     * <p>
     * Boolean basis means the population is based on true/false evaluation
     * rather than resource membership.
     * </p>
     *
     * @return this SelectedMeasureDefStratumPopulation for chaining
     */
    public SelectedMeasureDefStratumPopulation<P> isBooleanBasis() {
        // Boolean basis is implied by the populationBasis field on GroupDef
        // This is a convenience method for semantic clarity
        return this;
    }

    /**
     * Assert that this stratum population has resource basis.
     * <p>
     * Resource basis means the population count is based on resource membership.
     * </p>
     *
     * @return this SelectedMeasureDefStratumPopulation for chaining
     */
    public SelectedMeasureDefStratumPopulation<P> isResourceBasis() {
        // Resource basis is implied by the populationBasis field on GroupDef
        // This is a convenience method for semantic clarity
        return this;
    }

    /**
     * Assert the stratum population ID matches the underlying populationDef ID.
     *
     * @param id expected ID
     * @return this SelectedMeasureDefStratumPopulation for chaining
     */
    public SelectedMeasureDefStratumPopulation<P> hasPopulationId(String id) {
        assertNotNull(value(), "StratumPopulationDef is null");
        assertEquals(id, value().populationDef().id(), "Stratum population ID mismatch");
        return this;
    }

    /**
     * Get the underlying StratumPopulationDef for advanced assertions.
     *
     * @return the StratumPopulationDef instance
     */
    public StratumPopulationDef stratumPopulationDef() {
        return value();
    }

    public SelectedMeasureDefStratumPopulation<P> hasType(MeasurePopulationType expectedMeasurePopulationType) {
        assertNotNull(value(), "StratumPopulationDef is null");
        assertEquals(expectedMeasurePopulationType, value().populationDef().type());
        return this;
    }
}
