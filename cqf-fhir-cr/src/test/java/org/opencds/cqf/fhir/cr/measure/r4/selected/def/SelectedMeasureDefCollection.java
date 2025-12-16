package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;

/**
 * Fluent API for asserting on collections of MeasureDefs from multi-measure evaluation.
 */
public class SelectedMeasureDefCollection<P> extends Measure.Selected<List<MeasureReportDef>, P> {

    public SelectedMeasureDefCollection(List<MeasureReportDef> measureDefs, P parent) {
        super(measureDefs, parent);
    }

    // Assert count
    public SelectedMeasureDefCollection<P> hasCount(int expected) {
        assertEquals(expected, value.size(), "Expected " + expected + " MeasureDefs but found " + value.size());
        return this;
    }

    // Access by index
    public SelectedMeasureDef<SelectedMeasureDefCollection<P>> get(int index) {
        assertTrue(
                index >= 0 && index < value.size(),
                "Index " + index + " out of bounds for " + value.size() + " MeasureDefs");
        return new SelectedMeasureDef<>(value.get(index), this);
    }

    // Access first
    public SelectedMeasureDef<SelectedMeasureDefCollection<P>> first() {
        return get(0);
    }

    // Access by measure URL - returns collection (can be multiple in subject evaluation)
    public SelectedMeasureDefCollection<SelectedMeasureDefCollection<P>> byMeasureUrl(String measureUrl) {
        List<MeasureReportDef> found =
                value.stream().filter(def -> measureUrl.equals(def.url())).toList();
        assertFalse(found.isEmpty(), "No MeasureDefs found for measure URL: " + measureUrl);
        return new SelectedMeasureDefCollection<>(found, this);
    }

    // Access by measure ID - returns collection (can be multiple in subject evaluation)
    public SelectedMeasureDefCollection<SelectedMeasureDefCollection<P>> byMeasureId(String measureId) {
        List<MeasureReportDef> found =
                value.stream().filter(def -> measureId.equals(def.id())).toList();
        assertFalse(found.isEmpty(), "No MeasureDefs found for measure ID: " + measureId);
        return new SelectedMeasureDefCollection<>(found, this);
    }

    // TODO: Implement subject-level filtering once subject tracking API is clarified
    // Access by measure URL and subject - returns single MeasureDef
    // public SelectedMeasureDef<SelectedMeasureDefCollection<P>> byMeasureUrlAndSubject(
    //         String measureUrl, String subjectId) {
    //     // Need to determine how to access subject from MeasureDef
    //     // Subjects are tracked at population level, not measure level
    // }

    // TODO: Implement subject-level filtering once subject tracking API is clarified
    // Access by measure ID and subject - returns single MeasureDef
    // public SelectedMeasureDef<SelectedMeasureDefCollection<P>> byMeasureIdAndSubject(
    //         String measureId, String subjectId) {
    //     // Need to determine how to access subject from MeasureDef
    //     // Subjects are tracked at population level, not measure level
    // }

    // Assert all satisfy condition
    public SelectedMeasureDefCollection<P> allSatisfy(java.util.function.Consumer<MeasureReportDef> assertion) {
        value.forEach(assertion);
        return this;
    }

    // Get raw list for custom assertions
    public List<MeasureReportDef> list() {
        return value;
    }
}
