package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

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
}
