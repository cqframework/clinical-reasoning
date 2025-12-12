package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;

/**
 * Fluent assertion API for StratifierDef objects.
 * <p>
 * Provides assertions and navigation for stratifiers, including stratum navigation,
 * component checking, and result validation.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * .stratifier("gender-stratifier")
 *     .hasStratumCount(4)
 *     .hasExpression("Gender")
 *     .firstStratum()
 *         .hasSubjectCount(3)
 *         .hasValueDef("male")
 *         .population("numerator")
 *             .hasCount(2);
 * }</pre>
 *
 * <h3>Known Limitations:</h3>
 * <ul>
 * <li>TODO: Need to implement equivalent of SelectedStratifier#stratumByText().
 *     StratumDef objects don't have a 'text' field like their FHIR MeasureReport counterparts,
 *     making it difficult to select strata by human-readable text values. We need to determine
 *     the best way to identify and select specific strata within the Def model.</li>
 * </ul>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class SelectedMeasureDefStratifier<P>
        extends org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected<StratifierDef, P> {

    public SelectedMeasureDefStratifier(StratifierDef value, P parent) {
        super(value, parent);
    }

    // ==================== Navigation Methods ====================

    /**
     * Navigate to a stratum by index (0-based).
     *
     * @param index the stratum index
     * @return SelectedMeasureDefStratum for the stratum at the given index
     * @throws AssertionError if index is out of bounds
     */
    public SelectedMeasureDefStratum<SelectedMeasureDefStratifier<P>> stratum(int index) {
        assertNotNull(value(), "StratifierDef is null");
        assertTrue(
                index >= 0 && index < value().getStratum().size(),
                "Stratum index out of bounds: " + index + ", size: "
                        + value().getStratum().size());
        return new SelectedMeasureDefStratum<>(value().getStratum().get(index), this);
    }

    /**
     * Navigate to the first stratum.
     *
     * @return SelectedMeasureDefStratum for the first stratum
     * @throws AssertionError if no strata exist
     */
    public SelectedMeasureDefStratum<SelectedMeasureDefStratifier<P>> firstStratum() {
        assertNotNull(value(), "StratifierDef is null");
        assertFalse(value().getStratum().isEmpty(), "No strata found in StratifierDef");
        return new SelectedMeasureDefStratum<>(value().getStratum().get(0), this);
    }

    /**
     * Navigate to a stratum by its value text.
     *
     * @param valueText the stratum value text (e.g., "male", "female")
     * @return SelectedMeasureDefStratum for the matching stratum
     * @throws AssertionError if no stratum with the given value is found
     */
    public SelectedMeasureDefStratum<SelectedMeasureDefStratifier<P>> stratumByValue(String valueText) {
        assertNotNull(value(), "StratifierDef is null");
        StratumDef stratum = value().getStratum().stream()
                .filter(s -> s.valueDefs() != null
                        && s.valueDefs().stream()
                                .anyMatch(vd -> valueText.equals(vd.value().getDescription())))
                .findFirst()
                .orElse(null);
        assertNotNull(stratum, "No stratum found with value: " + valueText);
        return new SelectedMeasureDefStratum<>(stratum, this);
    }

    // ==================== Assertion Methods ====================

    /**
     * Assert the number of strata in this stratifier.
     *
     * @param count expected stratum count
     * @return this SelectedMeasureDefStratifier for chaining
     */
    public SelectedMeasureDefStratifier<P> hasStratumCount(int count) {
        assertNotNull(value(), "StratifierDef is null");
        assertEquals(count, value().getStratum().size(), "Stratum count mismatch");
        return this;
    }

    /**
     * Assert the number of stratifier components.
     *
     * @param count expected component count
     * @return this SelectedMeasureDefStratifier for chaining
     */
    public SelectedMeasureDefStratifier<P> hasComponentCount(int count) {
        assertNotNull(value(), "StratifierDef is null");
        assertEquals(count, value().components().size(), "Component count mismatch");
        return this;
    }

    /**
     * Assert the stratifier expression name.
     *
     * @param expression expected expression name
     * @return this SelectedMeasureDefStratifier for chaining
     */
    public SelectedMeasureDefStratifier<P> hasExpression(String expression) {
        assertNotNull(value(), "StratifierDef is null");
        assertEquals(expression, value().expression(), "Stratifier expression mismatch");
        return this;
    }

    /**
     * Assert that this is a component stratifier.
     *
     * @return this SelectedMeasureDefStratifier for chaining
     */
    public SelectedMeasureDefStratifier<P> isComponentStratifier() {
        assertNotNull(value(), "StratifierDef is null");
        assertFalse(value().components().isEmpty(), "Expected component stratifier, but no components found");
        return this;
    }

    /**
     * Assert that the stratifier has results for a specific subject.
     *
     * @param subjectId the subject ID
     * @return this SelectedMeasureDefStratifier for chaining
     */
    public SelectedMeasureDefStratifier<P> hasResultForSubject(String subjectId) {
        assertNotNull(value(), "StratifierDef is null");
        assertTrue(value().getResults().containsKey(subjectId), "No stratifier result found for subject: " + subjectId);
        return this;
    }

    /**
     * Assert the number of subjects with stratifier results.
     *
     * @param count expected result count
     * @return this SelectedMeasureDefStratifier for chaining
     */
    public SelectedMeasureDefStratifier<P> hasResultCount(int count) {
        assertNotNull(value(), "StratifierDef is null");
        assertEquals(count, value().getResults().size(), "Stratifier result count mismatch");
        return this;
    }

    /**
     * Assert the stratifier ID.
     *
     * @param id expected stratifier ID
     * @return this SelectedMeasureDefStratifier for chaining
     */
    public SelectedMeasureDefStratifier<P> hasStratifierId(String id) {
        assertNotNull(value(), "StratifierDef is null");
        assertEquals(id, value().id(), "Stratifier ID mismatch");
        return this;
    }

    /**
     * Get the underlying StratifierDef for advanced assertions.
     *
     * @return the StratifierDef instance
     */
    public StratifierDef stratifierDef() {
        return value();
    }
}
