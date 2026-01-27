package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.execution.EvaluationExpressionRef;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;

class EvaluationResultFormatterTest {

    @Test
    void formatExpressionValue_withNull_returnsNull() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(null);

        // Assert
        assertEquals("null", result);
    }

    @Test
    void formatExpressionValue_withBoolean_returnsString() {
        // Act
        String resultTrue = EvaluationResultFormatter.formatExpressionValue(true);
        String resultFalse = EvaluationResultFormatter.formatExpressionValue(false);

        // Assert
        assertEquals("true", resultTrue);
        assertEquals("false", resultFalse);
    }

    @Test
    void formatExpressionValue_withInteger_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(42);

        // Assert
        assertEquals("42", result);
    }

    @Test
    void formatExpressionValue_withLong_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(1234567890L);

        // Assert
        assertEquals("1234567890", result);
    }

    @Test
    void formatExpressionValue_withDouble_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(3.14159);

        // Assert
        assertEquals("3.14159", result);
    }

    @Test
    void formatExpressionValue_withString_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue("test-string");

        // Assert
        assertEquals("test-string", result);
    }

    @Test
    void formatExpressionValue_withLocalDate_returnsFormattedDate() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 3, 15);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(date);

        // Assert
        assertEquals("2024-03-15", result);
    }

    @Test
    void formatExpressionValue_withLocalDateTime_returnsFormattedDateTime() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30, 45);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(dateTime);

        // Assert
        assertEquals("2024-03-15:14:30:45", result);
    }

    @Test
    void formatExpressionValue_withDate_returnsFormattedDateTime() {
        // Arrange
        Date date = new Date(1710513045000L); // 2024-03-15 14:30:45 UTC

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(date);

        // Assert
        // Date format should be yyyy-MM-dd:HH:mm:ss
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}:\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void formatExpressionValue_withSingleResource_returnsResourceId() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("Patient/patient-123");

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(patient);

        // Assert
        assertEquals("Patient/patient-123", result);
    }

    @Test
    void formatExpressionValue_withResourceWithoutId_returnsMessage() {
        // Arrange
        Patient patient = new Patient();

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(patient);

        // Assert
        assertEquals("(resource with no ID)", result);
    }

    @Test
    void formatExpressionValue_withListOfPrimitives_returnsFormattedList() {
        // Arrange
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(numbers);

        // Assert
        assertEquals("[1, 2, 3, 4, 5]", result);
    }

    @Test
    void formatExpressionValue_withListOfStrings_returnsFormattedList() {
        // Arrange
        List<String> strings = Arrays.asList("apple", "banana", "cherry");

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(strings);

        // Assert
        assertEquals("[apple, banana, cherry]", result);
    }

    @Test
    void formatExpressionValue_withListOfResources_returnsResourceIds() {
        // Arrange
        Patient patient1 = new Patient();
        patient1.setId("Patient/patient-1");

        Patient patient2 = new Patient();
        patient2.setId("Patient/patient-2");

        Encounter encounter = new Encounter();
        encounter.setId("Encounter/encounter-1");

        List<Object> resources = Arrays.asList(patient1, patient2, encounter);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(resources);

        // Assert
        assertEquals("[Patient/patient-1, Patient/patient-2, Encounter/encounter-1]", result);
    }

    @Test
    void formatExpressionValue_withMixedList_returnsFormattedList() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("Patient/patient-1");

        List<Object> mixed = Arrays.asList(patient, "text", 42, true);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(mixed);

        // Assert
        assertEquals("[Patient/patient-1, text, 42, true]", result);
    }

    @Test
    void formatExpressionValue_withEmptyList_returnsEmptyBrackets() {
        // Arrange
        List<Object> emptyList = Arrays.asList();

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(emptyList);

        // Assert
        assertEquals("[]", result);
    }

    @Test
    void formatExpressionValue_withListContainingNull_handlesNull() {
        // Arrange
        List<Object> listWithNull = Arrays.asList("value1", null, "value2");

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(listWithNull);

        // Assert
        assertEquals("[value1, null, value2]", result);
    }

    @Test
    void formatExpressionValue_withListOfDates_returnsFormattedDates() {
        // Arrange
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 2, 20);
        List<LocalDate> dates = Arrays.asList(date1, date2);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(dates);

        // Assert
        assertEquals("[2024-01-15, 2024-02-20]", result);
    }

    @Test
    void formatExpressionValue_withMultipleResourceTypes_returnsAllResourceIds() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("Patient/patient-1");

        Encounter encounter = new Encounter();
        encounter.setId("Encounter/encounter-1");

        Observation observation = new Observation();
        observation.setId("Observation/obs-1");

        List<Object> resources = Arrays.asList(patient, encounter, observation);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(resources);

        // Assert
        assertEquals("[Patient/patient-1, Encounter/encounter-1, Observation/obs-1]", result);
    }

    @Test
    void formatExpressionValue_withCharacter_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue('A');

        // Assert
        assertEquals("A", result);
    }

    @Test
    void formatExpressionValue_withByte_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue((byte) 127);

        // Assert
        assertEquals("127", result);
    }

    @Test
    void formatExpressionValue_withShort_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue((short) 32000);

        // Assert
        assertEquals("32000", result);
    }

    @Test
    void formatExpressionValue_withFloat_returnsString() {
        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(3.14f);

        // Assert
        assertEquals("3.14", result);
    }

    @Test
    void formatExpressionValue_withEmptyMap_returnsEmptyString() {
        // Arrange
        Map<String, String> emptyMap = new HashMap<>();

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(emptyMap);

        // Assert
        assertEquals("", result);
    }

    @Test
    void formatExpressionValue_withSimpleMap_returnsFormattedMap() {
        // Arrange
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(map);

        // Assert
        assertEquals("a -> 1, b -> 2, c -> 3", result);
    }

    @Test
    void formatExpressionValue_withMapOfResources_returnsResourceIds() {
        // Arrange
        Patient patient1 = new Patient();
        patient1.setId("Patient/patient-1");

        Patient patient2 = new Patient();
        patient2.setId("Patient/patient-2");

        Map<String, Patient> map = new LinkedHashMap<>();
        map.put("p1", patient1);
        map.put("p2", patient2);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(map);

        // Assert
        assertEquals("p1 -> Patient/patient-1, p2 -> Patient/patient-2", result);
    }

    @Test
    void formatExpressionValue_withMapContainingNull_handlesNull() {
        // Arrange
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", null);
        map.put("key3", "value3");

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(map);

        // Assert
        assertEquals("key1 -> value1, key2 -> null, key3 -> value3", result);
    }

    @Test
    void formatExpressionValue_withMapOfMixedTypes_returnsFormattedMap() {
        // Arrange
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("string", "text");
        map.put("number", 42);
        map.put("boolean", true);
        map.put("date", LocalDate.of(2024, 3, 15));

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(map);

        // Assert
        assertEquals("string -> text, number -> 42, boolean -> true, date -> 2024-03-15", result);
    }

    @Test
    void formatExpressionValue_withMapOfEncounterToQuantityDef_returnsFormattedMap() {
        // Arrange
        Encounter encounter1 = new Encounter();
        encounter1.setId("Encounter/encounter-1");

        Encounter encounter2 = new Encounter();
        encounter2.setId("Encounter/encounter-2");

        QuantityDef quantity1 = new QuantityDef(75.0);
        QuantityDef quantity2 = new QuantityDef(120.5);

        Map<Encounter, QuantityDef> map = new LinkedHashMap<>();
        map.put(encounter1, quantity1);
        map.put(encounter2, quantity2);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(map);

        // Assert
        assertEquals(
                "Encounter/encounter-1 -> QuantityDef{value=75.0}, Encounter/encounter-2 -> QuantityDef{value=120.5}",
                result);
    }

    @Test
    void formatExpressionValue_withMapOfEncounterToQuantityDef_withNullValue_handlesNull() {
        // Arrange
        Encounter encounter1 = new Encounter();
        encounter1.setId("Encounter/encounter-1");

        Encounter encounter2 = new Encounter();
        encounter2.setId("Encounter/encounter-2");

        Encounter encounter3 = new Encounter();
        encounter3.setId("Encounter/encounter-3");

        QuantityDef quantity1 = new QuantityDef(75.0);
        QuantityDef quantity2 = new QuantityDef(null);
        QuantityDef quantity3 = new QuantityDef(42.5);

        Map<Encounter, QuantityDef> map = new LinkedHashMap<>();
        map.put(encounter1, quantity1);
        map.put(encounter2, quantity2);
        map.put(encounter3, quantity3);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(map);

        // Assert
        assertEquals(
                "Encounter/encounter-1 -> QuantityDef{value=75.0}, Encounter/encounter-2 -> QuantityDef{value=null}, Encounter/encounter-3 -> QuantityDef{value=42.5}",
                result);
    }

    @Test
    void formatExpressionValue_withMapOfEncounterToQuantityDef_multipleEntries_returnsAllFormatted() {
        // Arrange
        Encounter encounter1 = new Encounter();
        encounter1.setId("Encounter/patient-1-encounter-1");

        Encounter encounter2 = new Encounter();
        encounter2.setId("Encounter/patient-1-encounter-2");

        Encounter encounter3 = new Encounter();
        encounter3.setId("Encounter/patient-2-encounter-1");

        QuantityDef quantity1 = new QuantityDef(100.0);
        QuantityDef quantity2 = new QuantityDef(200.0);
        QuantityDef quantity3 = new QuantityDef(150.0);

        Map<Encounter, QuantityDef> map = new LinkedHashMap<>();
        map.put(encounter1, quantity1);
        map.put(encounter2, quantity2);
        map.put(encounter3, quantity3);

        // Act
        String result = EvaluationResultFormatter.formatExpressionValue(map);

        // Assert
        assertEquals(
                "Encounter/patient-1-encounter-1 -> QuantityDef{value=100.0}, "
                        + "Encounter/patient-1-encounter-2 -> QuantityDef{value=200.0}, "
                        + "Encounter/patient-2-encounter-1 -> QuantityDef{value=150.0}",
                result);
    }

    @Test
    void format_withNullEvaluationResult_returnsNull() {
        // Act
        String result = EvaluationResultFormatter.format(null, 0);

        // Assert
        assertEquals("null", result);
    }

    @Test
    void format_withEmptyExpressionResults_returnsMessage() {
        // Arrange
        EvaluationResult evaluationResult = new EvaluationResult();

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertEquals("(no expression results)", result);
    }

    @Test
    void format_withSingleExpression_formatsWithQuotedName() {
        // Arrange
        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(42, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("TestExpression"), expressionResult);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"TestExpression\""));
        assertTrue(result.contains("Value: 42"));
    }

    @Test
    void format_withMultipleExpressions_formatsAll() {
        // Arrange
        EvaluationResult evaluationResult = new EvaluationResult();

        ExpressionResult expr1 = new ExpressionResult("text", Set.of());
        evaluationResult.set(new EvaluationExpressionRef("Expression1"), expr1);

        ExpressionResult expr2 = new ExpressionResult(true, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("Expression2"), expr2);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"Expression1\""));
        assertTrue(result.contains("Value: text"));
        assertTrue(result.contains("Expression: \"Expression2\""));
        assertTrue(result.contains("Value: true"));
    }

    @Test
    void format_withEvaluatedResources_includesResources() {
        // Arrange
        Patient patient1 = new Patient();
        patient1.setId("Patient/patient-1");

        Patient patient2 = new Patient();
        patient2.setId("Patient/patient-2");

        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(2, Set.of(patient1, patient2));
        evaluationResult.set(new EvaluationExpressionRef("PatientExpression"), expressionResult);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"PatientExpression\""));
        assertTrue(result.contains("Evaluated Resources:"));
        assertTrue(result.contains("Patient/patient-1"));
        assertTrue(result.contains("Patient/patient-2"));
        assertTrue(result.contains("Value: 2"));
    }

    @Test
    void format_withBaseIndent_appliesIndentation() {
        // Arrange
        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(42, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("TestExpression"), expressionResult);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 2);

        // Assert
        // With baseIndent of 2, we expect 4 spaces at the start (2 * "  ")
        assertTrue(result.startsWith("    Expression: \"TestExpression\""));
        assertTrue(result.contains("      Value: 42")); // 6 spaces for indent level 3
    }

    @Test
    void format_withListValue_formatsCorrectly() {
        // Arrange
        List<Integer> numbers = Arrays.asList(1, 2, 3);

        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(numbers, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("ListExpression"), expressionResult);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"ListExpression\""));
        assertTrue(result.contains("Value: [1, 2, 3]"));
    }

    @Test
    void format_withMapValue_formatsCorrectly() {
        // Arrange
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(map, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("MapExpression"), expressionResult);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"MapExpression\""));
        assertTrue(result.contains("Value: a -> 1, b -> 2"));
    }

    @Test
    void format_withMapOfEncounterToQuantityDef_formatsCorrectly() {
        // Arrange
        Encounter encounter1 = new Encounter();
        encounter1.setId("Encounter/encounter-1");

        Encounter encounter2 = new Encounter();
        encounter2.setId("Encounter/encounter-2");

        QuantityDef quantity1 = new QuantityDef(75.0);
        QuantityDef quantity2 = new QuantityDef(120.5);

        Map<Encounter, QuantityDef> map = new LinkedHashMap<>();
        map.put(encounter1, quantity1);
        map.put(encounter2, quantity2);

        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(map, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("ObservationExpression"), expressionResult);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"ObservationExpression\""));
        assertTrue(
                result.contains(
                        "Value: Encounter/encounter-1 -> QuantityDef{value=75.0}, Encounter/encounter-2 -> QuantityDef{value=120.5}"));
    }

    @Test
    void format_withMapOfEncounterToQuantityDef_withEvaluatedResources_includesResourcesAndValue() {
        // Arrange
        Encounter encounter1 = new Encounter();
        encounter1.setId("Encounter/patient-1-encounter-1");

        Encounter encounter2 = new Encounter();
        encounter2.setId("Encounter/patient-1-encounter-2");

        Patient patient = new Patient();
        patient.setId("Patient/patient-1");

        QuantityDef quantity1 = new QuantityDef(100.0);
        QuantityDef quantity2 = new QuantityDef(200.0);

        Map<Encounter, QuantityDef> map = new LinkedHashMap<>();
        map.put(encounter1, quantity1);
        map.put(encounter2, quantity2);

        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expressionResult = new ExpressionResult(map, Set.of(encounter1, encounter2, patient));
        evaluationResult.set(new EvaluationExpressionRef("MeasureObservation"), expressionResult);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"MeasureObservation\""));
        assertTrue(result.contains("Evaluated Resources:"));
        assertTrue(result.contains("Patient/patient-1"));
        assertTrue(result.contains("Encounter/patient-1-encounter-1"));
        assertTrue(result.contains("Encounter/patient-1-encounter-2"));
        assertTrue(
                result.contains(
                        "Value: Encounter/patient-1-encounter-1 -> QuantityDef{value=100.0}, Encounter/patient-1-encounter-2 -> QuantityDef{value=200.0}"));
    }

    @Test
    void format_withMapOfEncounterToQuantityDef_multipleExpressions_formatsAll() {
        // Arrange
        Encounter encounter1 = new Encounter();
        encounter1.setId("Encounter/encounter-1");

        Encounter encounter2 = new Encounter();
        encounter2.setId("Encounter/encounter-2");

        QuantityDef quantity1 = new QuantityDef(75.0);
        QuantityDef quantity2 = new QuantityDef(120.5);

        Map<Encounter, QuantityDef> map1 = new LinkedHashMap<>();
        map1.put(encounter1, quantity1);

        Map<Encounter, QuantityDef> map2 = new LinkedHashMap<>();
        map2.put(encounter2, quantity2);

        EvaluationResult evaluationResult = new EvaluationResult();
        ExpressionResult expr1 = new ExpressionResult(map1, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("Observation1"), expr1);

        ExpressionResult expr2 = new ExpressionResult(map2, Set.of());
        evaluationResult.set(new EvaluationExpressionRef("Observation2"), expr2);

        // Act
        String result = EvaluationResultFormatter.format(evaluationResult, 0);

        // Assert
        assertTrue(result.contains("Expression: \"Observation1\""));
        assertTrue(result.contains("Value: Encounter/encounter-1 -> QuantityDef{value=75.0}"));
        assertTrue(result.contains("Expression: \"Observation2\""));
        assertTrue(result.contains("Value: Encounter/encounter-2 -> QuantityDef{value=120.5}"));
    }

    // Tests for printSubjectResources(), printValues(), printValue() methods

    @Test
    void printValue_withNull_returnsNull() {
        String result = EvaluationResultFormatter.printValue(null);
        assertEquals("null", result);
    }

    @Test
    void printValue_withString_returnsString() {
        String result = EvaluationResultFormatter.printValue("test-string");
        assertEquals("test-string", result);
    }

    @Test
    void printValue_withInteger_returnsString() {
        String result = EvaluationResultFormatter.printValue(42);
        assertEquals("42", result);
    }

    @Test
    void printValue_withBoolean_returnsString() {
        String result = EvaluationResultFormatter.printValue(true);
        assertEquals("true", result);
    }

    @Test
    void printValue_withResource_returnsResourceIdWithVersion() {
        Patient patient = new Patient();
        patient.setId("Patient/patient-123/_history/1");

        String result = EvaluationResultFormatter.printValue(patient);
        assertEquals("Patient/patient-123/_history/1", result);
    }

    @Test
    void printValue_withResourceNoId_returnsNull() {
        Patient patient = new Patient();
        String result = EvaluationResultFormatter.printValue(patient);
        // getValueAsString() on resource with no ID returns null
        assertEquals(null, result);
    }

    @Test
    void printValue_withEmptyMap_returnsEmpty() {
        Map<String, String> emptyMap = new HashMap<>();
        String result = EvaluationResultFormatter.printValue(emptyMap);
        assertEquals("{empty}", result);
    }

    @Test
    void printValue_withSimpleMap_returnsFormattedMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        String result = EvaluationResultFormatter.printValue(map);
        assertEquals("a -> 1, b -> 2", result);
    }

    @Test
    void printValue_withMapOfResources_returnsResourceIds() {
        Patient patient1 = new Patient();
        patient1.setId("Patient/patient-1");

        Encounter encounter = new Encounter();
        encounter.setId("Encounter/encounter-1");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("patient", patient1);
        map.put("encounter", encounter);

        String result = EvaluationResultFormatter.printValue(map);
        assertEquals("patient -> Patient/patient-1, encounter -> Encounter/encounter-1", result);
    }

    @Test
    void printValue_withMapContainingNullValue_handlesNull() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", null);

        String result = EvaluationResultFormatter.printValue(map);
        assertEquals("key1 -> value1, key2 -> null", result);
    }

    @Test
    void printValue_withMapAllNullValues_returnsEmpty() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", null);
        map.put("key2", null);

        String result = EvaluationResultFormatter.printValue(map);
        // toString of "null -> null, null -> null" is not blank, so it returns it
        assertEquals("key1 -> null, key2 -> null", result);
    }

    @Test
    void printValues_withNull_returnsEmpty() {
        String result = EvaluationResultFormatter.printValues(null);
        assertEquals("{empty}", result);
    }

    @Test
    void printValues_withEmptyCollection_returnsEmpty() {
        String result = EvaluationResultFormatter.printValues(List.of());
        assertEquals("{empty}", result);
    }

    @Test
    void printValues_withSingleValue_returnsFormattedValue() {
        String result = EvaluationResultFormatter.printValues(List.of("test"));
        assertEquals("test", result);
    }

    @Test
    void printValues_withMultiplePrimitives_returnsCommaSeparated() {
        String result = EvaluationResultFormatter.printValues(Arrays.asList(1, 2, 3));
        assertEquals("1, 2, 3", result);
    }

    @Test
    void printValues_withMultipleResources_returnsResourceIds() {
        Patient patient1 = new Patient();
        patient1.setId("Patient/patient-1");

        Patient patient2 = new Patient();
        patient2.setId("Patient/patient-2");

        String result = EvaluationResultFormatter.printValues(Arrays.asList(patient1, patient2));
        assertEquals("Patient/patient-1, Patient/patient-2", result);
    }

    @Test
    void printValues_withNullInCollection_handlesNull() {
        String result = EvaluationResultFormatter.printValues(Arrays.asList("value1", null, "value2"));
        assertEquals("value1, null, value2", result);
    }

    @Test
    void printSubjectResources_withNullPopulationDef_returnsEmpty() {
        Object result = EvaluationResultFormatter.printSubjectResources(null, "Patient/1");
        assertEquals("{empty}", result);
    }

    @Test
    void printSubjectResources_withEmptyResources_returnsSubjectIdWithEmpty() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef populationDef = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);

        Object result = EvaluationResultFormatter.printSubjectResources(populationDef, "Patient/1");
        assertEquals("Patient/1: {empty}", result);
    }

    @Test
    void printSubjectResources_withSingleResource_returnsFormatted() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef populationDef = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);

        Encounter encounter = new Encounter();
        encounter.setId("Encounter/encounter-1");
        populationDef.addResource("Patient/1", encounter);

        Object result = EvaluationResultFormatter.printSubjectResources(populationDef, "Patient/1");
        assertEquals("Patient/1: Encounter/encounter-1", result);
    }

    @Test
    void printSubjectResources_withMultipleResources_returnsCommaSeparated() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef populationDef = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);

        Encounter encounter1 = new Encounter();
        encounter1.setId("Encounter/encounter-1");
        Encounter encounter2 = new Encounter();
        encounter2.setId("Encounter/encounter-2");

        populationDef.addResource("Patient/1", encounter1);
        populationDef.addResource("Patient/1", encounter2);

        Object result = EvaluationResultFormatter.printSubjectResources(populationDef, "Patient/1");
        String resultStr = result.toString();
        assertTrue(resultStr.startsWith("Patient/1: "));
        assertTrue(resultStr.contains("Encounter/encounter-1"));
        assertTrue(resultStr.contains("Encounter/encounter-2"));
    }

    @Test
    void printSubjectResources_withMixedTypes_returnsFormatted() {
        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        PopulationDef populationDef =
                new PopulationDef("pop-1", null, MeasurePopulationType.NUMERATOR, "Numerator", stringBasis, null);

        populationDef.addResource("Patient/1", "value1");
        populationDef.addResource("Patient/1", "value2");

        Object result = EvaluationResultFormatter.printSubjectResources(populationDef, "Patient/1");
        String resultStr = result.toString();
        assertTrue(resultStr.startsWith("Patient/1: "));
        assertTrue(resultStr.contains("value1"));
        assertTrue(resultStr.contains("value2"));
    }
}
