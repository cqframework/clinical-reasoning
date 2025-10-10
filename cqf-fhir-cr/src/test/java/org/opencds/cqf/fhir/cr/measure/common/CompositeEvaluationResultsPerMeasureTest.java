package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

class CompositeEvaluationResultsPerMeasureTest {

    @Test
    void gettersContainExpectedData() {
        // Arrange
        var measureDef1 = buildMeasureDef("Measure/one");
        var measureDef2 = buildMeasureDef("Measure/two");

        // Create a non-empty EvaluationResult without depending on ExpressionResult constructors
        EvaluationResult er = new EvaluationResult();
        er.expressionResults.put("subject-123", null); // non-empty map is all the Builder checks

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef1, "subject-123", er);
        builder.addError(measureDef1, "oops-1");
        builder.addError(measureDef2, "oops-2");

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        Map<MeasureDef, Map<String, EvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<MeasureDef, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();

        // Assert: results present for m1, none for m2
        assertTrue(resultsPerMeasure.containsKey(measureDef1));
        assertFalse(resultsPerMeasure.containsKey(measureDef2));
        Map<String, EvaluationResult> m1Results = resultsPerMeasure.get(measureDef1);
        assertNotNull(m1Results);
        assertTrue(m1Results.containsKey("subject-123"));

        // Assert: errors present for both measures
        assertEquals(List.of("oops-1"), errorsPerMeasure.get(measureDef1));
        assertEquals(List.of("oops-2"), errorsPerMeasure.get(measureDef2));
    }

    @Test
    void gettersReturnImmutableViews() {
        var measureDef1 = buildMeasureDef("Measure/immutable");

        EvaluationResult er = new EvaluationResult();
        er.expressionResults.put("s", null);

        CompositeEvaluationResultsPerMeasure composite =
                CompositeEvaluationResultsPerMeasure.builder().build(); // empty instance to test top-level immutability

        // Top-level maps should be unmodifiable
        Map<MeasureDef, Map<String, EvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<MeasureDef, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();

        assertThrows(UnsupportedOperationException.class, () -> resultsPerMeasure.put(measureDef1, Map.of("s", er)));

        assertThrows(UnsupportedOperationException.class, () -> errorsPerMeasure.put(measureDef1, List.of("err")));
    }

    // LUKETODO:  util
    private static MeasureDef buildMeasureDef(String id) {
        return new MeasureDef(id, null, null, List.of(), List.of());
    }
}
