package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.EvaluationResultFormatter;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fluent API for asserting on collections of MeasureDefs from multi-measure evaluation.
 */
public class SelectedMeasureDefCollection<P> extends Measure.Selected<List<MeasureDef>, P> {

    private static final Logger logger = LoggerFactory.getLogger(SelectedMeasureDefCollection.class);

    private final Map<MeasureDef, Map<String, EvaluationResult>> evaluationResultsPerMeasure;

    public SelectedMeasureDefCollection(List<MeasureDef> measureDefs, P parent) {
        this(measureDefs, parent, Map.of());
    }

    public SelectedMeasureDefCollection(
            List<MeasureDef> measureDefs,
            P parent,
            Map<MeasureDef, Map<String, EvaluationResult>> evaluationResultsPerMeasure) {
        super(measureDefs, parent);
        this.evaluationResultsPerMeasure = evaluationResultsPerMeasure;
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
        MeasureDef def = value.get(index);
        return new SelectedMeasureDef<>(def, this, evaluationResultsPerMeasure.getOrDefault(def, Map.of()));
    }

    // Access first
    public SelectedMeasureDef<SelectedMeasureDefCollection<P>> first() {
        return get(0);
    }

    /**
     * Log evaluation results for all measures in this collection at once.
     * Each measure's results are formatted with separator lines between subjects and between measures.
     *
     * @return this SelectedMeasureDefCollection for chaining
     */
    public SelectedMeasureDefCollection<P> logAllMeasureEvaluationResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (MeasureDef def : value) {
            Map<String, EvaluationResult> results = evaluationResultsPerMeasure.getOrDefault(def, Map.of());
            sb.append(EvaluationResultFormatter.formatMeasureEvaluationResults(def.id(), results));
        }
        logger.info(sb.toString());
        return this;
    }

    // Access by measure URL - returns collection (can be multiple in subject evaluation)
    public SelectedMeasureDefCollection<SelectedMeasureDefCollection<P>> byMeasureUrl(String measureUrl) {
        List<MeasureDef> found =
                value.stream().filter(def -> measureUrl.equals(def.url())).toList();
        assertFalse(found.isEmpty(), "No MeasureDefs found for measure URL: " + measureUrl);
        return new SelectedMeasureDefCollection<>(found, this, evaluationResultsPerMeasure);
    }

    // Access by measure ID - returns collection (can be multiple in subject evaluation)
    public SelectedMeasureDefCollection<SelectedMeasureDefCollection<P>> byMeasureId(String measureId) {
        List<MeasureDef> found =
                value.stream().filter(def -> measureId.equals(def.id())).toList();
        assertFalse(found.isEmpty(), "No MeasureDefs found for measure ID: " + measureId);
        return new SelectedMeasureDefCollection<>(found, this, evaluationResultsPerMeasure);
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
    public SelectedMeasureDefCollection<P> allSatisfy(java.util.function.Consumer<MeasureDef> assertion) {
        value.forEach(assertion);
        return this;
    }

    // Get raw list for custom assertions
    public List<MeasureDef> list() {
        return value;
    }
}
