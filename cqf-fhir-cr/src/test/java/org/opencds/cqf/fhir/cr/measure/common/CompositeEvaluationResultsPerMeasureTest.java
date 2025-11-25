package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

class CompositeEvaluationResultsPerMeasureTest {

    @Test
    void gettersContainExpectedData() {
        // Arrange
        var measureDef1 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureOne"), "http://example.com/Measure/one");
        var measureDef2 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureTwo"), "http://example.com/Measure/two");
        var measureDef3 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureThree"), "http://example.com/Measure/three");

        // Create a non-empty EvaluationResult without depending on ExpressionResult constructors
        EvaluationResult er = new EvaluationResult();
        er.getExpressionResults().put("subject-123", null); // non-empty map is all the Builder checks

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef1, "subject-123", er, List.of());
        builder.addError(measureDef1, "oops-1");
        builder.addError(List.of(measureDef2, measureDef3), "oops-2");
        builder.addError(measureDef3, "oops-3");
        builder.addWarning(List.of(measureDef1, measureDef2), "warn-1");
        builder.addWarning(measureDef2, "warn-2");
        builder.addWarning(measureDef3, "warn-3");

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        Map<MeasureDef, Map<String, EvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<MeasureDef, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();
        Map<MeasureDef, List<String>> warningsPerMeasure = composite.getWarningsPerMeasure();

        // Assert: results present for m1, none for m2
        assertTrue(resultsPerMeasure.containsKey(measureDef1));
        assertFalse(resultsPerMeasure.containsKey(measureDef2));
        Map<String, EvaluationResult> m1Results = resultsPerMeasure.get(measureDef1);
        assertNotNull(m1Results);
        assertTrue(m1Results.containsKey("subject-123"));

        // Assert: errors present for both measures
        assertEquals(List.of("oops-1"), errorsPerMeasure.get(measureDef1));
        assertEquals(List.of("oops-2"), errorsPerMeasure.get(measureDef2));
        assertEquals(List.of("oops-2", "oops-3"), errorsPerMeasure.get(measureDef3));

        assertEquals(List.of("warn-1"), warningsPerMeasure.get(measureDef1));
        assertEquals(List.of("warn-1", "warn-2"), warningsPerMeasure.get(measureDef2));
        assertEquals(List.of("warn-3"), warningsPerMeasure.get(measureDef3));
    }

    @Test
    void gettersReturnImmutableViews() {
        var measureDef1 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureimmutable"), "http://example.com/Measure/immutable");

        EvaluationResult er = new EvaluationResult();
        er.getExpressionResults().put("s", null);

        CompositeEvaluationResultsPerMeasure composite =
                CompositeEvaluationResultsPerMeasure.builder().build(); // empty instance to test top-level immutability

        // Top-level maps should be unmodifiable
        Map<MeasureDef, Map<String, EvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<MeasureDef, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();

        final Map<String, EvaluationResult> evalMap = Map.of("s", er);

        assertThrows(UnsupportedOperationException.class, () -> resultsPerMeasure.put(measureDef1, evalMap));

        final List<String> evalList = List.of("err");

        assertThrows(UnsupportedOperationException.class, () -> errorsPerMeasure.put(measureDef1, evalList));
    }
}
