package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportPopulationExt extends Selected<Extension, SelectedMeasureReportPopulation> {

    public SelectedMeasureReportPopulationExt(Extension value, SelectedMeasureReportPopulation parent) {
        super(value, parent);
    }

    // -------------------------
    // resultBoolean (given)
    // -------------------------
    public SelectedMeasureReportPopulationExt hasBooleanResult(Boolean expected) {
        Boolean actual = this.value.getExtension().stream()
                .filter(e -> "resultBoolean".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(v -> v instanceof BooleanType)
                .map(v -> ((BooleanType) v).booleanValue())
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual);
        return this;
    }

    // -------------------------
    // resultString
    // -------------------------
    public SelectedMeasureReportPopulationExt hasStringResult(String expected) {
        String actual = this.value.getExtension().stream()
                .filter(e -> "resultString".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(v -> v instanceof StringType)
                .map(v -> ((StringType) v).getValue())
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasDecimalResult(Double expected) {
        BigDecimal actual = this.value.getExtension().stream()
                .filter(e -> "resultDecimal".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(v -> v instanceof DecimalType)
                .map(v -> ((DecimalType) v).getValue())
                .findFirst()
                .orElse(null);

        assertEquals(BigDecimal.valueOf(expected), actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasIntegerResult(int expected) {
        Integer actual = this.value.getExtension().stream()
                .filter(e -> "resultInteger".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(v -> v instanceof IntegerType)
                .map(v -> ((IntegerType) v).getValue())
                .findFirst()
                .orElse(null);

        assertEquals(Integer.valueOf(expected), actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasIntervalResult(Period expected) {

        Period actual = this.value.getExtension().stream()
                .filter(e -> "resultPeriod".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(v -> v instanceof Period)
                .map(v -> (Period) v)
                .findFirst()
                .orElse(null);

        assertNotNull(actual, "Expected resultPeriod extension but none was found");

        // Compare start/end explicitly to avoid equals() pitfalls
        assertEquals(
                expected.getStartElement().getValueAsString(),
                actual.getStartElement().getValueAsString(),
                "Period.start does not match");

        assertEquals(
                expected.getEndElement().getValueAsString(),
                actual.getEndElement().getValueAsString(),
                "Period.end does not match");

        return this;
    }

    // -------------------------
    // resultResourceId (stored as valueString)
    // -------------------------
    public SelectedMeasureReportPopulationExt hasResourceIdResult(String expectedResourceId) {
        String actual = this.value.getExtension().stream()
                .filter(e -> "resultList".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resultList"))
                .getExtension()
                .stream()
                .filter(item -> "itemResourceId".equals(item.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .filter(expectedResourceId::equals)
                .findFirst()
                .orElse(null);

        assertEquals(expectedResourceId, actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListBooleanResult(Boolean expectedResult) {
        Boolean actual = this.value.getExtension().stream()
                .filter(e -> "resultList".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resultList"))
                .getExtension()
                .stream()
                .filter(item -> "itemBoolean".equals(item.getUrl()))
                .map(Extension::getValue)
                .filter(BooleanType.class::isInstance)
                .map(BooleanType.class::cast)
                .map(BooleanType::getValue)
                .filter(expectedResult::equals)
                .findFirst()
                .orElse(null);

        assertEquals(expectedResult, actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListStringResult(String expectedResult) {
        String actual = this.value.getExtension().stream()
                .filter(e -> "resultList".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resultList"))
                .getExtension()
                .stream()
                .filter(item -> "itemString".equals(item.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .filter(expectedResult::equals)
                .findFirst()
                .orElse(null);

        assertEquals(expectedResult, actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListIntegerResult(int expectedResult) {

        boolean found = this.value.getExtension().stream()
                .filter(e -> "resultList".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resultList"))
                .getExtension()
                .stream()
                .filter(item -> "itemInteger".equals(item.getUrl()))
                .map(Extension::getValue)
                .filter(IntegerType.class::isInstance)
                .map(IntegerType.class::cast)
                .map(IntegerType::getValue)
                .anyMatch(v -> v != null && v == expectedResult);

        assertTrue(found, "Expected integer " + expectedResult + " not found in resultList");

        return this;
    }

    // -------------------------
    // Core tuple navigation
    // -------------------------

    private Extension getTupleField(String fieldName) {
        Extension tuple = this.value.getExtension().stream()
                .filter(e -> "resultTuple".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resultTuple"));

        return tuple.getExtension().stream()
                .filter(f -> fieldName.equals(f.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing tuple field: " + fieldName));
    }

    // -------------------------
    // Scalar result assertions
    // -------------------------

    public SelectedMeasureReportPopulationExt hasTupleBoolean(String fieldName, boolean expected) {
        Extension field = getTupleField(fieldName);

        Boolean actual = field.getExtension().stream()
                .filter(e -> "resultBoolean".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(BooleanType.class::isInstance)
                .map(BooleanType.class::cast)
                .map(BooleanType::booleanValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Tuple field '" + fieldName + "' boolean mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleInteger(String fieldName, int expected) {
        Extension field = getTupleField(fieldName);

        Integer actual = field.getExtension().stream()
                .filter(e -> "resultInteger".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(IntegerType.class::isInstance)
                .map(IntegerType.class::cast)
                .map(IntegerType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(Integer.valueOf(expected), actual, "Tuple field '" + fieldName + "' integer mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleDecimal(String fieldName, BigDecimal expected) {
        Extension field = getTupleField(fieldName);

        BigDecimal actual = field.getExtension().stream()
                .filter(e -> "resultDecimal".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(DecimalType.class::isInstance)
                .map(DecimalType.class::cast)
                .map(DecimalType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Tuple field '" + fieldName + "' decimal mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleString(String fieldName, String expected) {
        Extension field = getTupleField(fieldName);

        String actual = field.getExtension().stream()
                .filter(e -> "resultString".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Tuple field '" + fieldName + "' string mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleResourceId(String fieldName, String expected) {
        Extension field = getTupleField(fieldName);

        String actual = field.getExtension().stream()
                .filter(e -> "resultResourceId".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Tuple field '" + fieldName + "' resourceId mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTuplePeriod(String fieldName, Period expected) {
        Extension field = getTupleField(fieldName);

        Period actual = field.getExtension().stream()
                .filter(e -> "resultPeriod".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(Period.class::isInstance)
                .map(Period.class::cast)
                .findFirst()
                .orElse(null);

        assertNotNull(actual, "Tuple field '" + fieldName + "' missing Period");

        // Compare explicitly (FHIR Period equality can be tricky)
        assertEquals(
                expected.hasStart() ? expected.getStartElement().getValueAsString() : null,
                actual.hasStart() ? actual.getStartElement().getValueAsString() : null,
                "Tuple field '" + fieldName + "' period.start mismatch");
        assertEquals(
                expected.hasEnd() ? expected.getEndElement().getValueAsString() : null,
                actual.hasEnd() ? actual.getEndElement().getValueAsString() : null,
                "Tuple field '" + fieldName + "' period.end mismatch");

        return this;
    }

    // -------------------------
    // List assertions inside tuple fields
    // (expects field -> resultList -> itemX...)
    // -------------------------

    private List<Extension> getTupleResultListItems(String fieldName) {
        Extension field = getTupleField(fieldName);

        Extension list = field.getExtension().stream()
                .filter(e -> "resultList".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tuple field '" + fieldName + "' missing resultList"));

        return list.getExtension();
    }

    public SelectedMeasureReportPopulationExt hasTupleListStringItem(String fieldName, String expectedItem) {
        boolean found = getTupleResultListItems(fieldName).stream()
                .filter(e -> "itemString".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .anyMatch(expectedItem::equals);

        assertTrue(found, "Tuple field '" + fieldName + "' missing list itemString=" + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleListIntegerItem(String fieldName, int expectedItem) {
        boolean found = getTupleResultListItems(fieldName).stream()
                .filter(e -> "itemInteger".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(IntegerType.class::isInstance)
                .map(IntegerType.class::cast)
                .map(IntegerType::getValue)
                .anyMatch(v -> v != null && v == expectedItem);

        assertTrue(found, "Tuple field '" + fieldName + "' missing list itemInteger=" + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleListBooleanItem(String fieldName, boolean expectedItem) {
        boolean found = getTupleResultListItems(fieldName).stream()
                .filter(e -> "itemBoolean".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(BooleanType.class::isInstance)
                .map(BooleanType.class::cast)
                .map(BooleanType::booleanValue)
                .anyMatch(v -> v == expectedItem);

        assertTrue(found, "Tuple field '" + fieldName + "' missing list itemBoolean=" + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleListResourceIdItem(String fieldName, String expectedItem) {
        boolean found = getTupleResultListItems(fieldName).stream()
                .filter(e -> "itemResourceId".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .anyMatch(expectedItem::equals);

        assertTrue(found, "Tuple field '" + fieldName + "' missing list itemResourceId=" + expectedItem);
        return this;
    }
}
