package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;

/**
 * Fluent assertion API for MeasureDef objects.
 * <p>
 * Provides assertions and navigation for captured Def state at the measure level,
 * including group navigation, error checking, and measure metadata validation.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * Fhir2DefUnifiedMeasureTestHandler.given()
 *     .repositoryFor("MinimalMeasureEvaluation")
 * .when()
 *     .measureId("MinimalProportionMeasure")
 *     .captureDef()
 *     .evaluate()
 * .then()
 *     .def()
 *         .hasNoErrors()
 *         .hasMeasureId("MinimalProportionMeasure")
 *         .hasGroupCount(1)
 *         .firstGroup()
 *             .hasNullScore()  // Pre-scoring
 *             .population("numerator")
 *                 .hasSubjectCount(7);
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class SelectedDef extends Selected<MeasureDef, Fhir2DefUnifiedMeasureTestHandler.Then> {

    public SelectedDef(MeasureDef value, Fhir2DefUnifiedMeasureTestHandler.Then parent) {
        super(value, parent);
    }

    // ==================== Navigation Methods ====================

    /**
     * Navigate to the first group in the measure.
     *
     * @return SelectedDefGroup for the first group
     * @throws AssertionError if no groups exist
     */
    public SelectedDefGroup firstGroup() {
        assertNotNull(value(), "MeasureDef is null");
        assertFalse(value().groups().isEmpty(), "No groups found in MeasureDef");
        return new SelectedDefGroup(value().groups().get(0), this);
    }

    /**
     * Navigate to a group by ID.
     *
     * @param id the group ID
     * @return SelectedDefGroup for the matching group
     * @throws AssertionError if no group with the given ID is found
     */
    public SelectedDefGroup group(String id) {
        assertNotNull(value(), "MeasureDef is null");
        GroupDef group = value().groups().stream()
                .filter(g -> id.equals(g.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(group, "No group found with ID: " + id);
        return new SelectedDefGroup(group, this);
    }

    /**
     * Navigate to a group by index (0-based).
     *
     * @param index the group index
     * @return SelectedDefGroup for the group at the given index
     * @throws AssertionError if index is out of bounds
     */
    public SelectedDefGroup group(int index) {
        assertNotNull(value(), "MeasureDef is null");
        assertTrue(
                index >= 0 && index < value().groups().size(),
                "Group index out of bounds: " + index + ", size: "
                        + value().groups().size());
        return new SelectedDefGroup(value().groups().get(index), this);
    }

    // ==================== Assertion Methods ====================

    /**
     * Assert the number of groups in the measure.
     *
     * @param count expected group count
     * @return this SelectedDef for chaining
     */
    public SelectedDef hasGroupCount(int count) {
        assertNotNull(value(), "MeasureDef is null");
        assertEquals(count, value().groups().size(), "Group count mismatch");
        return this;
    }

    /**
     * Assert the number of errors in the measure.
     *
     * @param count expected error count
     * @return this SelectedDef for chaining
     */
    public SelectedDef hasErrors(int count) {
        assertNotNull(value(), "MeasureDef is null");
        assertEquals(count, value().errors().size(), "Error count mismatch");
        return this;
    }

    /**
     * Assert that an error message containing the given substring exists.
     *
     * @param errorSubstring substring to search for in error messages
     * @return this SelectedDef for chaining
     */
    public SelectedDef hasError(String errorSubstring) {
        assertNotNull(value(), "MeasureDef is null");
        boolean found = value().errors().stream().anyMatch(error -> error.contains(errorSubstring));
        assertTrue(found, "No error found containing: " + errorSubstring + ", errors: " + value().errors());
        return this;
    }

    /**
     * Assert that the measure has no errors.
     *
     * @return this SelectedDef for chaining
     */
    public SelectedDef hasNoErrors() {
        assertNotNull(value(), "MeasureDef is null");
        assertTrue(value().errors().isEmpty(), "Expected no errors, but found: " + value().errors());
        return this;
    }

    /**
     * Assert the measure ID.
     *
     * @param id expected measure ID
     * @return this SelectedDef for chaining
     */
    public SelectedDef hasMeasureId(String id) {
        assertNotNull(value(), "MeasureDef is null");
        assertEquals(id, value().id(), "Measure ID mismatch");
        return this;
    }

    /**
     * Assert the measure URL.
     *
     * @param url expected measure canonical URL
     * @return this SelectedDef for chaining
     */
    public SelectedDef hasMeasureUrl(String url) {
        assertNotNull(value(), "MeasureDef is null");
        assertEquals(url, value().url(), "Measure URL mismatch");
        return this;
    }

    /**
     * Assert the measure version.
     *
     * @param version expected measure version
     * @return this SelectedDef for chaining
     */
    public SelectedDef hasMeasureVersion(String version) {
        assertNotNull(value(), "MeasureDef is null");
        assertEquals(version, value().version(), "Measure version mismatch");
        return this;
    }

    /**
     * Get the underlying MeasureDef for advanced assertions.
     *
     * @return the MeasureDef instance
     */
    public MeasureDef measureDef() {
        return value();
    }
}
