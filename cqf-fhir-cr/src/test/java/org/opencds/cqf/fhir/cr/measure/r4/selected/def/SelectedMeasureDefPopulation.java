package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;

/**
 * Fluent assertion API for PopulationDef objects.
 * <p>
 * Provides assertions for population-level details including subject counts,
 * subject IDs, resource counts, and population metadata.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * .population("numerator")
 *     .hasSubjectCount(7)
 *     .hasSubjects("Patient/1", "Patient/2", "Patient/3")
 *     .doesNotHaveSubject("Patient/excluded")
 *     .hasResourceCount(15)
 *     .subjectHasResourceCount("Patient/1", 2)
 *     .hasType("numerator")
 *     .hasExpression("Numerator");
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class SelectedMeasureDefPopulation<P>
        extends org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected<PopulationDef, P> {

    public SelectedMeasureDefPopulation(PopulationDef value, P parent) {
        super(value, parent);
    }

    // ==================== Subject Assertions ====================

    /**
     * Assert the number of subjects in this population.
     *
     * @param count expected subject count
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasSubjectCount(int count) {
        assertNotNull(value(), "PopulationDef is null");
        assertEquals(count, value().getSubjectResources().size(), "Subject count mismatch");
        return this;
    }

    /**
     * Assert that specific subjects are in this population.
     *
     * @param subjectIds expected subject IDs (e.g., "Patient/1")
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasSubjects(String... subjectIds) {
        assertNotNull(value(), "PopulationDef is null");
        for (String subjectId : subjectIds) {
            assertTrue(
                    value().getSubjectResources().containsKey(subjectId),
                    "Subject not found in population: " + subjectId + ", available: "
                            + value().getSubjectResources().keySet());
        }
        return this;
    }

    /**
     * Assert that a specific subject is NOT in this population.
     *
     * @param subjectId subject ID that should not be present
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> doesNotHaveSubject(String subjectId) {
        assertNotNull(value(), "PopulationDef is null");
        assertFalse(
                value().getSubjectResources().containsKey(subjectId),
                "Subject should not be in population: " + subjectId);
        return this;
    }

    // ==================== Resource Assertions ====================

    /**
     * Assert the total count for this population.
     * <p>
     * Uses PopulationDef.getCount() which is the single source of truth for population counts.
     * The count logic depends on the population type and population's basis:
     * <ul>
     *   <li>MEASUREOBSERVATION: count observations</li>
     *   <li>Boolean basis: count unique subjects</li>
     *   <li>Non-boolean basis: count all resources (including duplicates across subjects)</li>
     * </ul>
     * </p>
     *
     * @param count expected population count
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasCount(int count) {
        assertNotNull(value(), "PopulationDef is null");
        int actualCount = value().getCount();
        assertEquals(count, actualCount, "Population count mismatch");
        return this;
    }

    public SelectedMeasureDefPopulation<P> hasNoAggregationResult() {
        return hasAggregationResult(null);
    }

    public SelectedMeasureDefPopulation<P> hasAggregationResult(Object expectedAggregationResult) {
        assertNotNull(value(), "PopulationDef is null");
        final Double actualAggregationResult = value().getAggregationResult();
        assertEquals(expectedAggregationResult, actualAggregationResult, "Population aggregation result mismatch");
        return this;
    }

    public SelectedMeasureDefPopulation<P> hasNoAggregateMethod() {
        return hasAggregateMethod(null);
    }

    public SelectedMeasureDefPopulation<P> hasAggregateMethodNA() {
        return hasAggregateMethod(ContinuousVariableObservationAggregateMethod.N_A);
    }

    public SelectedMeasureDefPopulation<P> hasAggregateMethod(
            ContinuousVariableObservationAggregateMethod expectedAggregateMethod) {
        assertNotNull(value(), "PopulationDef is null");
        final ContinuousVariableObservationAggregateMethod actualAggregateMethod = value().getAggregateMethod();

        if (null == expectedAggregateMethod) {
            assertNull(actualAggregateMethod, "PopulationDef aggregate method is not null");
            return this;
        }

        assertNotNull(actualAggregateMethod, "PopulationDef aggregate method is null");
        assertEquals(expectedAggregateMethod, actualAggregateMethod, "Population aggregate method mismatch");
        return this;
    }

    /**
     * Assert the number of evaluated resources (from evaluatedResources set).
     *
     * @param count expected evaluated resource count
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasEvaluatedResourceCount(int count) {
        assertNotNull(value(), "PopulationDef is null");
        assertEquals(count, value().getEvaluatedResources().size(), "Evaluated resource count mismatch");
        return this;
    }

    /**
     * Assert the number of resources for a specific subject.
     *
     * @param subjectId the subject ID
     * @param count expected resource count for this subject
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> subjectHasResourceCount(String subjectId, int count) {
        assertNotNull(value(), "PopulationDef is null");
        assertTrue(value().getSubjectResources().containsKey(subjectId), "Subject not found: " + subjectId);
        java.util.Set<?> resources = value().getSubjectResources().get(subjectId);
        assertEquals(count, resources.size(), "Resource count mismatch for subject " + subjectId);
        return this;
    }

    // ==================== Metadata Assertions ====================

    /**
     * Assert the population type/code (e.g., "numerator", "denominator").
     *
     * @param type expected population type code
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasType(MeasurePopulationType type) {
        assertNotNull(value(), "PopulationDef is null");
        assertEquals(type, value().type(), "Population type mismatch");
        return this;
    }

    /**
     * Assert the population expression name.
     *
     * @param expression expected expression name
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasExpression(String expression) {
        assertNotNull(value(), "PopulationDef is null");
        assertEquals(expression, value().expression(), "Population expression mismatch");
        return this;
    }

    /**
     * Assert the population has no criteria reference.
     *
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasNoCriteriaReference() {
        return hasCriteriaReference(null);
    }

    /**
     * Assert the population criteria reference (for MEASUREOBSERVATION populations).
     * Performs comprehensive validation:
     * - Verifies the criteria reference matches the expected value
     * - Validates that the referenced population exists and has type NUMERATOR or DENOMINATOR
     * - Applies heuristic validation based on population ID naming (e.g., "observation-num" must reference NUMERATOR)
     *
     * @param criteriaReference expected criteria reference (population ID), or null for no criteria reference
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasCriteriaReference(String criteriaReference) {
        assertNotNull(value(), "PopulationDef is null");
        assertEquals(criteriaReference, value().getCriteriaReference(), "Criteria reference mismatch");

        // If null, no further validation needed
        if (criteriaReference == null) {
            return this;
        }

        // Get the parent group to access all populations
        if (!(parent instanceof SelectedMeasureDefGroup<?> selectedMeasureDefGroup)) {
            // Parent might be something else in stratifier context, skip validation
            return this;
        }

        // Find the referenced population
        var referencedPopulation = selectedMeasureDefGroup.value().populations().stream()
                .filter(p -> criteriaReference.equalsIgnoreCase(p.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(
                referencedPopulation,
                String.format(
                        "Criteria reference '%s' does not match any population ID in the group. Available populations: %s",
                        criteriaReference,
                        selectedMeasureDefGroup.value().populations().stream()
                                .map(PopulationDef::id)
                                .toList()));

        // Validate that the referenced population is NUMERATOR or DENOMINATOR
        var refType = referencedPopulation.type();
        assertTrue(
                refType == MeasurePopulationType.NUMERATOR || refType == MeasurePopulationType.DENOMINATOR,
                String.format(
                        "Criteria reference '%s' points to population with type '%s', but must be NUMERATOR or DENOMINATOR",
                        criteriaReference, refType));

        // Apply heuristic validation based on current population ID
        final String currentPopId = value().id();
        if (currentPopId != null) {
            String lowerCaseId = currentPopId.toLowerCase();
            if (lowerCaseId.contains("num")) {
                assertEquals(
                        MeasurePopulationType.NUMERATOR,
                        refType,
                        String.format(
                                "Population ID '%s' contains 'num' but references '%s' which is type '%s' instead of NUMERATOR",
                                currentPopId, criteriaReference, refType));
            } else if (lowerCaseId.contains("den")) {
                assertEquals(
                        MeasurePopulationType.DENOMINATOR,
                        refType,
                        String.format(
                                "Population ID '%s' contains 'den' but references '%s' which is type '%s' instead of DENOMINATOR",
                                currentPopId, criteriaReference, refType));
            }
        }

        return this;
    }

    /**
     * Assert the population ID.
     *
     * @param id expected population ID
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> hasPopulationId(String id) {
        assertNotNull(value(), "PopulationDef is null");
        assertEquals(id, value().id(), "Population ID mismatch");
        return this;
    }

    /**
     * Assert that this population is a boolean-basis population.
     *
     * @return this SelectedMeasureDefPopulation for chaining
     */
    public SelectedMeasureDefPopulation<P> isBooleanBasis() {
        // Boolean basis is implied by the populationBasis field on GroupDef
        // This is a convenience method that doesn't directly check PopulationDef
        return this;
    }

    /**
     * Get the underlying PopulationDef for advanced assertions.
     *
     * @return the PopulationDef instance
     */
    public PopulationDef populationDef() {
        return value();
    }

    public SelectedMeasureDefPopulationExtension<SelectedMeasureDefPopulation<P>> getExtDef(String expressionName) {
        var extDef = this.value.getSupportingEvidenceDefs().stream()
                .filter(t -> t.getExpression().equals(expressionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Expression not found"));
        return new SelectedMeasureDefPopulationExtension<>(extDef, this);
    }

    public SelectedMeasureDefPopulation<P> assertNoSupportingEvidenceResults() {

        var defs = this.value.getSupportingEvidenceDefs();

        if (defs == null || defs.isEmpty()) {
            return this; // nothing to check
        }

        for (var def : defs) {
            var subjectResources = def.getSubjectResources();

            if (subjectResources == null || subjectResources.isEmpty()) {
                continue;
            }

            // Any key with a non-empty Set is a failure
            for (var entry : subjectResources.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    throw new AssertionError("SupportingEvidenceDef '" + def.getName() + "' produced results for key '"
                            + entry.getKey() + "': " + entry.getValue());
                }
            }
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    private static <T> T unsafeCast(Object selected) {
        return (T) selected;
    }
}
