package org.opencds.cqf.fhir.cr.measure.dstu3.selected.def;

import static org.junit.jupiter.api.Assertions.*;

import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;

/**
 * Fluent API for asserting on MeasureDef (pre-scoring internal state) from DSTU3 measure evaluation.
 * <p>
 * Provides methods to assert on the MeasureDef hierarchy built during measure evaluation before
 * scoring is applied by the MeasureReportBuilder.
 * </p>
 *
 * @param <P> parent type for up() navigation
 */
public class SelectedMeasureDef<P> {
    protected final MeasureDef measureDef;
    protected final P parent;

    public SelectedMeasureDef(MeasureDef measureDef, P parent) {
        this.measureDef = measureDef;
        this.parent = parent;
    }

    public MeasureDef value() {
        return measureDef;
    }

    public P up() {
        return parent;
    }

    // Assert no errors
    public SelectedMeasureDef<P> hasNoErrors() {
        assertTrue(measureDef.errors().isEmpty(), "Expected no errors in MeasureDef but found: " + measureDef.errors());
        return this;
    }

    // Assert has errors
    public SelectedMeasureDef<P> hasErrors(int count) {
        assertEquals(count, measureDef.errors().size(), "Error count mismatch");
        return this;
    }

    // Assert has any errors
    public SelectedMeasureDef<P> hasErrors() {
        assertFalse(measureDef.errors().isEmpty(), "Expected errors in MeasureDef but found none");
        return this;
    }

    // Access first group
    public SelectedMeasureDefGroup<SelectedMeasureDef<P>> firstGroup() {
        assertFalse(measureDef.groups().isEmpty(), "Expected at least one group but found none");
        return new SelectedMeasureDefGroup<>(measureDef.groups().get(0), this);
    }

    // Access group by index
    public SelectedMeasureDefGroup<SelectedMeasureDef<P>> group(int index) {
        assertTrue(
                index >= 0 && index < measureDef.groups().size(),
                "Index " + index + " out of bounds for " + measureDef.groups().size() + " groups");
        return new SelectedMeasureDefGroup<>(measureDef.groups().get(index), this);
    }

    // Assert group count
    public SelectedMeasureDef<P> hasGroupCount(int expected) {
        assertEquals(
                expected,
                measureDef.groups().size(),
                "Expected " + expected + " groups but found "
                        + measureDef.groups().size());
        return this;
    }

    // Assert measure URL
    public SelectedMeasureDef<P> hasMeasureUrl(String expectedUrl) {
        assertEquals(expectedUrl, measureDef.url(), "Measure URL mismatch");
        return this;
    }

    // Assert measure ID
    public SelectedMeasureDef<P> hasMeasureId(String expectedId) {
        assertEquals(expectedId, measureDef.id(), "Measure ID mismatch");
        return this;
    }
}
