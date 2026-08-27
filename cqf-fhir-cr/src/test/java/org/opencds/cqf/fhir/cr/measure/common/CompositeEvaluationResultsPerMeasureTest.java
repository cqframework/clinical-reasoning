package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.debug.DebugResult;
import org.opencds.cqf.cql.engine.execution.EvaluationExpressionRef;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.execution.trace.Trace;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;

class CompositeEvaluationResultsPerMeasureTest {
    static final FhirModelResolver<?, ?, ?, ?, ?, ?, ?, ?> modelResolver = new R4FhirModelResolver();

    @Test
    void gettersContainExpectedData() {
        // Arrange
        var measureDef1 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureOne"), "http://example.com/Measure/one");
        var measureDef2 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureTwo"), "http://example.com/Measure/two");

        // Create a non-empty EvaluationResult without depending on ExpressionResult constructors
        EvaluationResult er = new EvaluationResult();
        er.set(new EvaluationExpressionRef("subject-123"), new ExpressionResult(null, null));

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef1, "subject-123", er, List.of());
        builder.addError(measureDef1, "oops-1");
        builder.addError(measureDef2, "oops-2");

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        Map<MeasureDef, Map<String, CqlEvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<MeasureDef, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();

        // Assert: results present for m1, none for m2
        assertTrue(resultsPerMeasure.containsKey(measureDef1));
        assertFalse(resultsPerMeasure.containsKey(measureDef2));
        Map<String, CqlEvaluationResult> m1Results = resultsPerMeasure.get(measureDef1);
        assertNotNull(m1Results);
        assertTrue(m1Results.containsKey("subject-123"));

        // Assert: errors present for both measures
        assertEquals(List.of("oops-1"), errorsPerMeasure.get(measureDef1));
        assertEquals(List.of("oops-2"), errorsPerMeasure.get(measureDef2));
    }

    @Test
    void gettersReturnImmutableViews() {
        var measureDef1 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureimmutable"), "http://example.com/Measure/immutable");

        var er = new CqlEvaluationResult();
        er.addExpressionResult(CqlExpressionValue.ofRaw("s", null, null));

        CompositeEvaluationResultsPerMeasure composite =
                CompositeEvaluationResultsPerMeasure.builder().build(); // empty instance to test top-level immutability

        // Top-level maps should be unmodifiable
        Map<MeasureDef, Map<String, CqlEvaluationResult>> resultsPerMeasure = composite.getResultsPerMeasure();
        Map<MeasureDef, List<String>> errorsPerMeasure = composite.getErrorsPerMeasure();

        final Map<String, CqlEvaluationResult> evalMap = Map.of("s", er);

        assertThrows(UnsupportedOperationException.class, () -> resultsPerMeasure.put(measureDef1, evalMap));

        final List<String> evalList = List.of("err");

        assertThrows(UnsupportedOperationException.class, () -> errorsPerMeasure.put(measureDef1, evalList));
    }

    @Test
    void testToStringWithEmptyComposite() {
        // Arrange
        CompositeEvaluationResultsPerMeasure composite =
                CompositeEvaluationResultsPerMeasure.builder().build();

        // Act
        String result = composite.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("CompositeEvaluationResultsPerMeasure:"));
        assertTrue(result.contains("Results Per Measure:"));
        assertTrue(result.contains("(none)"));
        assertTrue(result.contains("Errors Per Measure:"));
    }

    @Test
    void testToStringWithResultsOnly() {
        // Arrange
        var measureDef = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measure123"), "http://example.com/Measure/test");

        // Create a patient resource with ID
        var patient = modelResolver.toCqlValue(new Patient().setId("Patient/patient-1"), false);

        // Create an EvaluationResult with expression results
        EvaluationResult er = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(
                new org.opencds.cqf.cql.engine.runtime.Boolean(true), new HashSet<>(List.of(patient)));
        er.set(new EvaluationExpressionRef("Initial Population"), expressionResult);

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef, "patient-1", er, List.of());

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        String result = composite.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Measure ID: measure123"));
        assertTrue(result.contains("Measure URL: http://example.com/Measure/test"));
        assertTrue(result.contains("Subject: patient-1"));
        assertTrue(result.contains("Expression: \"Initial Population\""));
        assertTrue(result.contains("Evaluated Resources:"));
        assertTrue(result.contains("Patient/patient-1"));
        assertTrue(result.contains("Value: true"));
    }

    @Test
    void testToStringWithErrorsOnly() {
        // Arrange
        var measureDef = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measure456"), "http://example.com/Measure/error-test");

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addError(measureDef, "This is a very long error message that should be truncated");
        builder.addError(measureDef, "Short error");

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        String result = composite.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Measure ID: measure456"));
        assertTrue(result.contains("Measure URL: http://example.com/Measure/error-test"));
        assertTrue(result.contains("This is a very long ...")); // Truncated to 20 chars + "..."
        assertTrue(result.contains("Short error"));
        assertFalse(result.contains("that should be truncated")); // Should not appear
    }

    @Test
    void testToStringWithResultsAndErrors() {
        // Arrange
        var measureDef1 = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measure789"), "http://example.com/Measure/combo");
        var measureDef2 = MeasureDef.fromIdAndUrl(new IdType(ResourceType.Measure.name(), "measure-null-url"), null);

        // Create resources
        var patient = modelResolver.toCqlValue(new Patient().setId("Patient/patient-2"), false);

        var encounter = modelResolver.toCqlValue(new Encounter().setId("Encounter/patient-2-encounter-1"), false);

        // Create EvaluationResult with multiple expression results
        EvaluationResult er = new EvaluationResult();
        ExpressionResult popResult = new ExpressionResult(
                new org.opencds.cqf.cql.engine.runtime.Integer(5), new HashSet<>(List.of(patient, encounter)));
        er.set(new EvaluationExpressionRef("Initial Population"), popResult);

        ExpressionResult numResult =
                new ExpressionResult(new org.opencds.cqf.cql.engine.runtime.String("test-string"), Set.of());
        er.set(new EvaluationExpressionRef("Numerator"), numResult);

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef1, "patient-2", er, List.of());
        builder.addError(measureDef1, "Error for measure 1");
        builder.addError(measureDef2, "Another error message");

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        String result = composite.toString();

        // Assert
        assertNotNull(result);

        // Check results section
        assertTrue(result.contains("Measure ID: measure789"));
        assertTrue(result.contains("Measure URL: http://example.com/Measure/combo"));
        assertTrue(result.contains("Subject: patient-2"));
        assertTrue(result.contains("Expression: \"Initial Population\""));
        assertTrue(result.contains("Patient/patient-2"));
        assertTrue(result.contains("Encounter/patient-2-encounter-1"));
        assertTrue(result.contains("Value: 5"));
        assertTrue(result.contains("Expression: \"Numerator\""));
        assertTrue(result.contains("Value: test-string"));

        // Check errors section
        assertTrue(result.contains("Measure ID: measure-null-url"));
        assertTrue(result.contains("Measure URL: (none)"));
        assertTrue(result.contains("Error for measure 1"));
        assertTrue(result.contains("Another error messag...")); // Truncated to 20 chars + "..."
    }

    @Test
    void testToStringWithDateValues() {
        // Arrange
        var measureDef = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measure-dates"), "http://example.com/Measure/dates");

        // Create EvaluationResult with date values
        EvaluationResult er = new EvaluationResult();

        var localDate =
                modelResolver.toCqlValue(new DateType(LocalDate.of(2024, 1, 15).toString()), false);
        ExpressionResult dateResult = new ExpressionResult(localDate, Set.of());
        er.set(new EvaluationExpressionRef("Date Expression"), dateResult);

        var localDateTime = modelResolver.toCqlValue(
                new DateTimeType(LocalDateTime.of(2024, 1, 15, 14, 30, 45).toString()), false);
        ExpressionResult dateTimeResult = new ExpressionResult(localDateTime, Set.of());
        er.set(new EvaluationExpressionRef("DateTime Expression"), dateTimeResult);

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef, "patient-3", er, List.of());

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        String result = composite.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Expression: \"Date Expression\""));
        assertTrue(result.contains("2024-01-15"));
        assertTrue(result.contains("Expression: \"DateTime Expression\""));
        assertTrue(result.contains("2024-01-15T14:30:45"));
    }

    @Test
    void testToStringWithCollectionValues() {
        // Arrange
        var measureDef = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measure-list"), "http://example.com/Measure/collections");

        // Create resources
        var patient1 = modelResolver.toCqlValue(new Patient().setId("Patient/patient-1"), false);

        var patient2 = modelResolver.toCqlValue(new Patient().setId("Patient/patient-2"), false);

        // Create EvaluationResult with collection value
        EvaluationResult er = new EvaluationResult();
        var patientList = new org.opencds.cqf.cql.engine.runtime.List(List.of(patient1, patient2));
        ExpressionResult listResult = new ExpressionResult(patientList, Set.of());
        er.set(new EvaluationExpressionRef("Patient List"), listResult);

        CompositeEvaluationResultsPerMeasure.Builder builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef, "subject-list", er, List.of());

        CompositeEvaluationResultsPerMeasure composite = builder.build();

        // Act
        String result = composite.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Expression: \"Patient List\""));
        assertTrue(result.contains("Patient/patient-1"));
        assertTrue(result.contains("Patient/patient-2"));
        assertTrue(result.contains("[")); // Collection bracket
    }

    @Test
    void mergePreservesDebugResult() {
        var measureDef = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureDebug"), "http://example.com/Measure/debug");

        EvaluationResult er = new EvaluationResult();
        er.set(
                new EvaluationExpressionRef("expr1"),
                new ExpressionResult(new org.opencds.cqf.cql.engine.runtime.Boolean(true), Set.of()));

        var debugResult = new DebugResult();
        er.setDebugResult(debugResult);

        var observationResult = new CqlEvaluationResult();
        observationResult.setExpressionResults(List.of(
                CqlExpressionValue.ofRaw("obs1", new org.opencds.cqf.cql.engine.runtime.Integer(42), Set.of())));

        var builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef, "patient-1", er, List.of(observationResult));

        var composite = builder.build();
        var results = composite.getResultsPerMeasure().get(measureDef);
        var mergedResult = results.get("patient-1");

        assertNotNull(mergedResult);
        assertNotNull(mergedResult.get("expr1"));
        assertNotNull(mergedResult.get("obs1"));
        assertEquals(debugResult, mergedResult.getResult().getDebugResult());
    }

    @Test
    void mergePreservesTrace() {
        var measureDef = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureTrace"), "http://example.com/Measure/trace");

        EvaluationResult er = new EvaluationResult();
        er.set(
                new EvaluationExpressionRef("expr1"),
                new ExpressionResult(new org.opencds.cqf.cql.engine.runtime.Boolean(true), Set.of()));

        var trace = new Trace(List.of());
        er.setTrace(trace);

        var builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef, "patient-1", er, List.of());

        var composite = builder.build();
        var results = composite.getResultsPerMeasure().get(measureDef);
        var mergedResult = results.get("patient-1");

        assertNotNull(mergedResult);
        assertEquals(trace, mergedResult.getResult().getTrace());
    }

    @Test
    void mergeWithNoDebugInfoLeavesFieldsNull() {
        var measureDef = MeasureDef.fromIdAndUrl(
                new IdType(ResourceType.Measure.name(), "measureNoDebug"), "http://example.com/Measure/nodebug");

        EvaluationResult er = new EvaluationResult();
        er.set(
                new EvaluationExpressionRef("expr1"),
                new ExpressionResult(new org.opencds.cqf.cql.engine.runtime.Boolean(true), Set.of()));

        var builder = CompositeEvaluationResultsPerMeasure.builder();
        builder.addResult(measureDef, "patient-1", er, List.of());

        var composite = builder.build();
        var results = composite.getResultsPerMeasure().get(measureDef);
        var mergedResult = results.get("patient-1");

        assertNotNull(mergedResult);
        assertNull(mergedResult.getResult().getDebugResult());
        assertNull(mergedResult.getResult().getTrace());
    }
}
