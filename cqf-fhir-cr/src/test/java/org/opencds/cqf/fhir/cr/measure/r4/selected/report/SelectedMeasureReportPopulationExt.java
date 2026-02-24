package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportPopulationExt extends Selected<Extension, SelectedMeasureReportPopulation> {

    private static final BigDecimal DECIMAL_TOLERANCE = new BigDecimal("0.00001");
    private static final String EXT_DATA_ABSENT_REASON = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";

    private static final String EXT_CQF_IS_EMPTY_LIST = "http://hl7.org/fhir/StructureDefinition/cqf-isEmptyList";

    private static final String EXT_CQF_CQL_TYPE = "http://hl7.org/fhir/StructureDefinition/cqf-cqlType";

    public SelectedMeasureReportPopulationExt(Extension value, SelectedMeasureReportPopulation parent) {
        super(value, parent);
    }

    // ============================================================
    // Helpers: slice access
    // ============================================================

    /** Returns all nested extensions for a given slice url (e.g., "value", "name", "description", "code"). */
    private List<Extension> slices(String sliceUrl) {
        return this.value.getExtension().stream()
                .filter(e -> sliceUrl.equals(e.getUrl()))
                .toList();
    }

    /** Returns the first slice (or null) for a given slice url. */
    private Extension firstSlice(String sliceUrl) {
        return this.value.getExtension().stream()
                .filter(e -> sliceUrl.equals(e.getUrl()))
                .findFirst()
                .orElse(null);
    }

    /** Returns all "value" slices under this supportingEvidence extension. */
    private List<Extension> valueSlices() {
        return slices("value");
    }

    // ============================================================
    // Optional metadata assertions (name/description/code)
    // ============================================================

    public SelectedMeasureReportPopulationExt hasName(String expected) {
        Extension nameExt = firstSlice("name");
        assertNotNull(nameExt, "Missing 'name' slice");

        // Some profiles use valueCode, some use valueString. Support both.
        String actual = (nameExt.getValue() instanceof CodeType ct)
                ? ct.getValue()
                : (nameExt.getValue() instanceof StringType st) ? st.getValue() : null;

        assertEquals(expected, actual, "SupportingEvidence.name mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasDescription(String expected) {
        Extension descExt = firstSlice("description");
        assertNotNull(descExt, "Missing 'description' slice");

        String actual = descExt.getValue() instanceof StringType st ? st.getValue() : null;
        assertEquals(expected, actual, "SupportingEvidence.description mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasCode(CodeableConcept expected) {
        Extension codeExt = firstSlice("code");
        assertNotNull(codeExt, "Missing 'code' slice");

        CodeableConcept actual = codeExt.getValue() instanceof CodeableConcept cc ? cc : null;
        assertNotNull(actual, "SupportingEvidence.code is not a CodeableConcept");
        assertNotNull(expected, "Expected CodeableConcept must not be null");

        // If expected has text, require it to match
        if (expected.hasText()) {
            assertEquals(expected.getText(), actual.getText(), "SupportingEvidence.code.text mismatch");
        }

        // Require that for each expected coding, there exists a matching actual coding
        for (Coding exp : expected.getCoding()) {
            boolean found = actual.getCoding().stream()
                    .anyMatch(act -> Objects.equals(exp.getSystem(), act.getSystem())
                            && Objects.equals(exp.getCode(), act.getCode())
                            &&
                            // only compare display if expected display is present
                            (!exp.hasDisplay() || Objects.equals(exp.getDisplay(), act.getDisplay())));

            assertTrue(
                    found,
                    "SupportingEvidence.code missing expected coding: system=" + exp.getSystem()
                            + " code=" + exp.getCode()
                            + (exp.hasDisplay() ? " display=" + exp.getDisplay() : ""));
        }

        return this;
    }

    // ============================================================
    // Scalar assertions (value[x] stored directly on each "value" slice)
    // ============================================================

    public SelectedMeasureReportPopulationExt hasBooleanValue(boolean expected) {
        Boolean actual = valueSlices().stream()
                .map(Extension::getValue)
                .filter(BooleanType.class::isInstance)
                .map(BooleanType.class::cast)
                .map(BooleanType::booleanValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Boolean value mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasIntegerValue(int expected) {
        Integer actual = valueSlices().stream()
                .map(Extension::getValue)
                .filter(IntegerType.class::isInstance)
                .map(IntegerType.class::cast)
                .map(IntegerType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(Integer.valueOf(expected), actual, "Integer value mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasStringValue(String expected) {
        String actual = valueSlices().stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "String value mismatch");
        return this;
    }

    /** ResourceId stored as valueString. */
    public SelectedMeasureReportPopulationExt hasResourceIdValue(String expectedResourceId) {
        String actual = valueSlices().stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(expectedResourceId, actual, "ResourceId value mismatch");
        return this;
    }

    public SelectedMeasureReportPopulationExt hasDecimalValue(Double expected) {
        BigDecimal expectedBd = BigDecimal.valueOf(expected);

        BigDecimal actual = valueSlices().stream()
                .map(Extension::getValue)
                .filter(DecimalType.class::isInstance)
                .map(DecimalType.class::cast)
                .map(DecimalType::getValue)
                .findFirst()
                .orElse(null);

        assertNotNull(actual, "Expected decimal value but none was found");
        assertTrue(
                actual.subtract(expectedBd).abs().compareTo(DECIMAL_TOLERANCE) <= 0,
                "Decimal not within tolerance (±0.00001). expected=" + expectedBd + ", actual=" + actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasPeriodValue(ZonedDateTime expectedStart, ZonedDateTime expectedEnd) {

        Period actual = this.value.getExtension().stream()
                .filter(e -> "value".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(Period.class::isInstance)
                .map(Period.class::cast)
                .findFirst()
                .orElse(null);

        assertNotNull(actual, "Expected resultPeriod extension but none was found");

        assertEquals(expectedStart.toInstant(), actual.getStart().toInstant(), "Period.start mismatch");

        assertEquals(expectedEnd.toInstant(), actual.getEnd().toInstant(), "Period.end mismatch");

        return this;
    }

    // ============================================================
    // List assertions (multiple repeated "value" slices)
    // ============================================================

    public SelectedMeasureReportPopulationExt hasListBooleanItem(boolean expectedItem) {
        boolean found = valueSlices().stream()
                .map(Extension::getValue)
                .filter(BooleanType.class::isInstance)
                .map(BooleanType.class::cast)
                .map(BooleanType::booleanValue)
                .anyMatch(v -> v == expectedItem);

        assertTrue(found, "Expected boolean item not found: " + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListIntegerItem(int expectedItem) {
        boolean found = valueSlices().stream()
                .map(Extension::getValue)
                .filter(IntegerType.class::isInstance)
                .map(IntegerType.class::cast)
                .map(IntegerType::getValue)
                .filter(Objects::nonNull)
                .anyMatch(v -> v == expectedItem);

        assertTrue(found, "Expected integer item not found: " + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListStringItem(String expectedItem) {
        boolean found = valueSlices().stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .anyMatch(expectedItem::equals);

        assertTrue(found, "Expected string item not found: " + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListResourceIdItem(String expectedResourceId) {
        boolean found = valueSlices().stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .anyMatch(expectedResourceId::equals);

        assertTrue(found, "Expected resourceId item not found: " + expectedResourceId);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListDecimalItem(Double expectedValue) {
        BigDecimal expected = BigDecimal.valueOf(expectedValue);

        boolean found = valueSlices().stream()
                .map(Extension::getValue)
                .filter(DecimalType.class::isInstance)
                .map(DecimalType.class::cast)
                .map(DecimalType::getValue)
                .anyMatch(actual ->
                        actual != null && actual.subtract(expected).abs().compareTo(DECIMAL_TOLERANCE) <= 0);

        assertTrue(found, "Expected decimal item not found within tolerance (±0.00001): " + expected);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListPeriodItem(
            ZonedDateTime expectedStart, ZonedDateTime expectedEnd) {

        Period actual = this.value.getExtension().stream()
                // value slices
                .filter(e -> "value".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(Period.class::isInstance)
                .map(Period.class::cast)
                .filter(p -> p.hasStart()
                        && p.hasEnd()
                        && expectedStart.toInstant().equals(p.getStart().toInstant())
                        && expectedEnd.toInstant().equals(p.getEnd().toInstant()))
                .findFirst()
                .orElse(null);

        assertNotNull(
                actual,
                "Expected Period not found in repeated valuePeriod slices. " + "Looked for start=" + expectedStart
                        + " end=" + expectedEnd);

        return this;
    }

    // ============================================================
    // Tuple assertions
    //
    // Tuple encoding assumed:
    // - one "value" slice has no value[x] but HAS nested extensions
    // - those nested extensions are tuple fields (url = fieldName)
    // - each field has repeated nested extensions[url="value"] for the field values
    // ============================================================

    private Extension tupleValueSlice() {
        return valueSlices().stream()
                .filter(v -> v.getValue() == null && v.hasExtension())
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError("Missing tuple value slice (value with nested field extensions)"));
    }

    private Extension tupleField(String fieldName) {
        Extension tupleValue = tupleValueSlice();
        return tupleValue.getExtension().stream()
                .filter(f -> fieldName.equals(f.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing tuple field: " + fieldName));
    }

    private List<Extension> tupleFieldValueSlices(String fieldName) {
        return tupleField(fieldName).getExtension().stream()
                .filter(e -> "value".equals(e.getUrl()))
                .toList();
    }

    public SelectedMeasureReportPopulationExt hasTupleBoolean(String fieldName, boolean expected) {
        Boolean actual = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(BooleanType.class::isInstance)
                .map(BooleanType.class::cast)
                .map(BooleanType::booleanValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Tuple boolean mismatch for field=" + fieldName);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleInteger(String fieldName, int expected) {
        Integer actual = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(IntegerType.class::isInstance)
                .map(IntegerType.class::cast)
                .map(IntegerType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(Integer.valueOf(expected), actual, "Tuple integer mismatch for field=" + fieldName);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleString(String fieldName, String expected) {
        String actual = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Tuple string mismatch for field=" + fieldName);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleResourceId(String fieldName, String expected) {
        String actual = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual, "Tuple resourceId mismatch for field=" + fieldName);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleDecimal(String fieldName, Double expected) {
        BigDecimal expectedBd = BigDecimal.valueOf(expected);

        BigDecimal actual = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(DecimalType.class::isInstance)
                .map(DecimalType.class::cast)
                .map(DecimalType::getValue)
                .findFirst()
                .orElse(null);

        assertNotNull(actual, "Tuple decimal missing for field=" + fieldName);
        assertTrue(
                actual.subtract(expectedBd).abs().compareTo(DECIMAL_TOLERANCE) <= 0,
                "Tuple decimal not within tolerance (±0.00001). expected=" + expectedBd + ", actual=" + actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTuplePeriod(String fieldName, Period expected) {
        Period actual = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(Period.class::isInstance)
                .map(Period.class::cast)
                .findFirst()
                .orElse(null);

        assertNotNull(actual, "Tuple Period missing for field=" + fieldName);

        assertEquals(
                expected.hasStart() ? expected.getStartElement().getValueAsString() : null,
                actual.hasStart() ? actual.getStartElement().getValueAsString() : null,
                "Tuple Period.start mismatch for field=" + fieldName);
        assertEquals(
                expected.hasEnd() ? expected.getEndElement().getValueAsString() : null,
                actual.hasEnd() ? actual.getEndElement().getValueAsString() : null,
                "Tuple Period.end mismatch for field=" + fieldName);

        return this;
    }

    // Tuple “list of values” assertions (field has repeated value slices)
    public SelectedMeasureReportPopulationExt hasTupleListStringItem(String fieldName, String expectedItem) {
        boolean found = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .anyMatch(expectedItem::equals);

        assertTrue(found, "Tuple list missing string item for field=" + fieldName + " item=" + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleListIntegerItem(String fieldName, int expectedItem) {
        boolean found = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(IntegerType.class::isInstance)
                .map(IntegerType.class::cast)
                .map(IntegerType::getValue)
                .filter(Objects::nonNull)
                .anyMatch(v -> v == expectedItem);

        assertTrue(found, "Tuple list missing integer item for field=" + fieldName + " item=" + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleListBooleanItem(String fieldName, boolean expectedItem) {
        boolean found = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(BooleanType.class::isInstance)
                .map(BooleanType.class::cast)
                .map(BooleanType::booleanValue)
                .anyMatch(v -> v == expectedItem);

        assertTrue(found, "Tuple list missing boolean item for field=" + fieldName + " item=" + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasTupleListResourceIdItem(String fieldName, String expectedItem) {
        boolean found = tupleFieldValueSlices(fieldName).stream()
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .anyMatch(expectedItem::equals);

        assertTrue(found, "Tuple list missing resourceId item for field=" + fieldName + " item=" + expectedItem);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasNullResult() {

        Extension valueSlice =
                valueSlices().stream().findFirst().orElseThrow(() -> new AssertionError("Missing value slice"));

        assertInstanceOf(
                BooleanType.class,
                valueSlice.getValue(),
                "Null result must be encoded as BooleanType with primitive extensions");

        BooleanType prim = (BooleanType) valueSlice.getValue();

        // No boolean value should be present
        assertNull(prim.getValue(), "Boolean value must be null for null result");

        Extension dar = prim.getExtension().stream()
                .filter(e -> EXT_DATA_ABSENT_REASON.equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing data-absent-reason extension"));

        assertEquals("unknown", dar.getValue().primitiveValue(), "data-absent-reason must be 'unknown'");

        Extension type = prim.getExtension().stream()
                .filter(e -> EXT_CQF_CQL_TYPE.equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cqf-cqlType extension"));

        assertEquals("System.Any", type.getValue().primitiveValue(), "cqf-cqlType must be System.Any");

        return this;
    }

    public SelectedMeasureReportPopulationExt hasEmptyListResult() {

        Extension valueSlice =
                valueSlices().stream().findFirst().orElseThrow(() -> new AssertionError("Missing value slice"));

        assertInstanceOf(
                BooleanType.class,
                valueSlice.getValue(),
                "Empty list result must be encoded as BooleanType with primitive extensions");

        BooleanType prim = (BooleanType) valueSlice.getValue();

        // Boolean value must be null
        assertNull(prim.getValue(), "Boolean value must be null for empty list marker");

        Extension empty = prim.getExtension().stream()
                .filter(e -> EXT_CQF_IS_EMPTY_LIST.equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cqf-isEmptyList extension"));

        BooleanType isEmpty = (BooleanType) empty.getValue();
        assertTrue(isEmpty.booleanValue(), "cqf-isEmptyList must be true");

        Extension type = prim.getExtension().stream()
                .filter(e -> EXT_CQF_CQL_TYPE.equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cqf-cqlType extension"));

        assertEquals("List<System.Any>", type.getValue().primitiveValue(), "cqf-cqlType must be List<System.Any>");

        return this;
    }
}
