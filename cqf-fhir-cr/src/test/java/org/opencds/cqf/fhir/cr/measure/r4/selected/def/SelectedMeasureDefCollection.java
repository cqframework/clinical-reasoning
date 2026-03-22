package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;

/**
 * Fluent API for asserting on collections of MeasureDefs from multi-measure evaluation.
 */
public class SelectedMeasureDefCollection<P> extends Measure.Selected<List<MeasureDef>, P> {

    private final List<MeasureEvaluationState> states;

    public SelectedMeasureDefCollection(List<MeasureDef> measureDefs, List<MeasureEvaluationState> states, P parent) {
        super(measureDefs, parent);
        this.states = states;
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
        return new SelectedMeasureDef<>(value.get(index), states.get(index), this);
    }

    // Access first
    public SelectedMeasureDef<SelectedMeasureDefCollection<P>> first() {
        return get(0);
    }

    // Access by measure URL - returns collection (can be multiple in subject evaluation)
    public SelectedMeasureDefCollection<SelectedMeasureDefCollection<P>> byMeasureUrl(String measureUrl) {
        var indices = IntStream.range(0, value.size())
                .filter(i -> measureUrl.equals(value.get(i).url()))
                .boxed()
                .toList();
        assertFalse(indices.isEmpty(), "No MeasureDefs found for measure URL: " + measureUrl);
        List<MeasureDef> foundDefs = indices.stream().map(value::get).toList();
        List<MeasureEvaluationState> foundStates =
                indices.stream().map(states::get).toList();
        return new SelectedMeasureDefCollection<>(foundDefs, foundStates, this);
    }

    // Access by measure ID - returns collection (can be multiple in subject evaluation)
    public SelectedMeasureDefCollection<SelectedMeasureDefCollection<P>> byMeasureId(String measureId) {
        var indices = IntStream.range(0, value.size())
                .filter(i -> measureId.equals(value.get(i).id()))
                .boxed()
                .toList();
        assertFalse(indices.isEmpty(), "No MeasureDefs found for measure ID: " + measureId);
        List<MeasureDef> foundDefs = indices.stream().map(value::get).toList();
        List<MeasureEvaluationState> foundStates =
                indices.stream().map(states::get).toList();
        return new SelectedMeasureDefCollection<>(foundDefs, foundStates, this);
    }

    // Assert all satisfy condition
    public SelectedMeasureDefCollection<P> allSatisfy(java.util.function.Consumer<MeasureDef> assertion) {
        value.forEach(assertion);
        return this;
    }

    // Get raw list for custom assertions
    public List<MeasureDef> list() {
        return value;
    }
}
