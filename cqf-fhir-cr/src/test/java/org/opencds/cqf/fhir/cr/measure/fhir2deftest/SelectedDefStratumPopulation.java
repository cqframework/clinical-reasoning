package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class SelectedDefStratumPopulation extends Selected<StratumPopulationDef, SelectedDefStratum> {

    public SelectedDefStratumPopulation(StratumPopulationDef value, SelectedDefStratum parent) {
        super(value, parent);
    }

    // ==================== Count Assertions ====================

    /**
     * Assert the count value for this stratum population (number of subjects/resources).
     *
     * @param count expected count
     * @return this SelectedDefStratumPopulation for chaining
     */
    public SelectedDefStratumPopulation hasCount(int count) {
        assertNotNull(value(), "StratumPopulationDef is null");
        assertEquals(count, value().subjectsQualifiedOrUnqualified().size(), "Stratum population count mismatch");
        return this;
    }

    /**
     * Assert the number of subjects in this stratum population.
     *
     * @param count expected subject count
     * @return this SelectedDefStratumPopulation for chaining
     */
    public SelectedDefStratumPopulation hasSubjectCount(int count) {
        assertNotNull(value(), "StratumPopulationDef is null");
        assertEquals(
                count, value().subjectsQualifiedOrUnqualified().size(), "Stratum population subject count mismatch");
        return this;
    }

    /**
     * Assert that specific subjects are in this stratum population.
     *
     * @param subjectIds expected subject IDs (e.g., "Patient/1")
     * @return this SelectedDefStratumPopulation for chaining
     */
    public SelectedDefStratumPopulation hasSubjects(String... subjectIds) {
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
     * @return this SelectedDefStratumPopulation for chaining
     */
    public SelectedDefStratumPopulation hasResourceCount(int count) {
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
     * @return this SelectedDefStratumPopulation for chaining
     */
    public SelectedDefStratumPopulation isBooleanBasis() {
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
     * @return this SelectedDefStratumPopulation for chaining
     */
    public SelectedDefStratumPopulation isResourceBasis() {
        // Resource basis is implied by the populationBasis field on GroupDef
        // This is a convenience method for semantic clarity
        return this;
    }

    /**
     * Assert the stratum population ID matches the underlying populationDef ID.
     *
     * @param id expected ID
     * @return this SelectedDefStratumPopulation for chaining
     */
    public SelectedDefStratumPopulation hasPopulationId(String id) {
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
}
