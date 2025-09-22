package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

class CompositeEvaluationResultsPerMeasureTest {

    @Test
    void gettersContainExpectedData() {
        // Arrange
        IIdType m1 = new IdType("Measure/one");
        IIdType m2 = new IdType("Measure/two");

        // Create a non-empty EvaluationResult without depending on ExpressionResult constructors
        EvaluationResult er = new EvaluationResult();
        er.expressionResults.put("subject-123", null); // non-empty map is all the Builder checks

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(m1, "subject-123", er);
        builder.addErrors(List.of(m1), "oops-1");
        builder.addErrors(List.of(m2), "oops-2");

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        Map<IIdType, Map<String, EvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<IIdType, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();

        // Assert: results present for m1, none for m2
        assertTrue(resultsPerMeasure.containsKey(m1.toUnqualifiedVersionless()));
        assertFalse(resultsPerMeasure.containsKey(m2.toUnqualifiedVersionless()));
        Map<String, EvaluationResult> m1Results = resultsPerMeasure.get(m1.toUnqualifiedVersionless());
        assertNotNull(m1Results);
        assertTrue(m1Results.containsKey("subject-123"));

        // Assert: errors present for both measures
        assertEquals(List.of("oops-1"), errorsPerMeasure.get(m1.toUnqualifiedVersionless()));
        assertEquals(List.of("oops-2"), errorsPerMeasure.get(m2.toUnqualifiedVersionless()));
    }

    @Test
    void gettersReturnImmutableViews() {
        IIdType m = new IdType("Measure/immutable");

        EvaluationResult er = new EvaluationResult();
        er.expressionResults.put("s", null);

        CompositeEvaluationResultsPerMeasure composite =
                CompositeEvaluationResultsPerMeasure.builder().build(); // empty instance to test top-level immutability

        // Top-level maps should be unmodifiable
        Map<IIdType, Map<String, EvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<IIdType, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();

        assertThrows(UnsupportedOperationException.class, () -> resultsPerMeasure.put(m, Map.of("s", er)));

        assertThrows(UnsupportedOperationException.class, () -> errorsPerMeasure.put(m, List.of("err")));
    }
}
