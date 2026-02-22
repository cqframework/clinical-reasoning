package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

class ParametersCreationTest {

    private static final FhirContext CTX = FhirContext.forR4Cached();

    @Test
    void newParametersWithParts() {
        var part = Parameters.newPart(CTX, "testParam");
        var params = Parameters.newParameters(CTX, part);
        assertNotNull(params);
    }

    @Test
    void newParametersWithIdType() {
        var part = Parameters.newPart(CTX, "testParam");
        var params = Parameters.newParameters(CTX, new IdType("test-id"), part);
        assertNotNull(params);
        assertEquals("test-id", params.getIdElement().getIdPart());
    }

    @Test
    void newParametersWithStringId() {
        var params = Parameters.newParameters(CTX, "test-id");
        assertNotNull(params);
    }

    @Test
    void newPartWithTypeName() {
        var part = Parameters.newPart(CTX, "string", "myParam", "myValue");
        assertNotNull(part);
    }

    @Test
    void newPartWithTypeClass() {
        var part = Parameters.newPart(CTX, StringType.class, "myParam", "myValue");
        assertNotNull(part);
    }

    @Test
    void newPartWithResource() {
        var patient = new Patient();
        patient.setId("test");
        var part = Parameters.newPart(CTX, "myParam", patient);
        assertNotNull(part);
    }

    @Test
    void newStringPart() {
        var part = Parameters.newStringPart(CTX, "name", "value");
        assertNotNull(part);
    }

    @Test
    void newBooleanPart() {
        var part = Parameters.newBooleanPart(CTX, "flag", true);
        assertNotNull(part);
    }

    @Test
    void newIntegerPart() {
        var part = Parameters.newIntegerPart(CTX, "count", 42);
        assertNotNull(part);
    }

    @Test
    void newCodePart() {
        var part = Parameters.newCodePart(CTX, "status", "active");
        assertNotNull(part);
    }

    @Test
    void newUriPart() {
        var part = Parameters.newUriPart(CTX, "url", "http://example.org");
        assertNotNull(part);
    }

    @Test
    void newDatePart() {
        var part = Parameters.newDatePart(CTX, "date", "2024-01-01");
        assertNotNull(part);
    }

    @Test
    void newDecimalPart() {
        var part = Parameters.newDecimalPart(CTX, "value", 3.14);
        assertNotNull(part);
    }

    @Test
    void newCanonicalPart() {
        var part = Parameters.newCanonicalPart(CTX, "ref", "http://example.org/Library/test");
        assertNotNull(part);
    }

    @Test
    void newBase64BinaryPart() {
        var part = Parameters.newBase64BinaryPart(CTX, "data", "dGVzdA==");
        assertNotNull(part);
    }

    @Test
    void newDateTimePart() {
        var part = Parameters.newDateTimePart(CTX, "timestamp", "2024-01-01T00:00:00Z");
        assertNotNull(part);
    }

    @Test
    void newIdPart() {
        var part = Parameters.newIdPart(CTX, "id", "test-123");
        assertNotNull(part);
    }

    @Test
    void newMarkdownPart() {
        var part = Parameters.newMarkdownPart(CTX, "text", "# Hello");
        assertNotNull(part);
    }

    @Test
    void newOidPart() {
        var part = Parameters.newOidPart(CTX, "oid", "urn:oid:1.2.3");
        assertNotNull(part);
    }

    @Test
    void newPositiveIntPart() {
        var part = Parameters.newPositiveIntPart(CTX, "count", 1);
        assertNotNull(part);
    }

    @Test
    void newUnsignedIntPart() {
        var part = Parameters.newUnsignedIntPart(CTX, "count", 0);
        assertNotNull(part);
    }

    @Test
    void newUrlPart() {
        var part = Parameters.newUrlPart(CTX, "endpoint", "http://example.org");
        assertNotNull(part);
    }

    @Test
    void newTimePart() {
        var part = Parameters.newTimePart(CTX, "time", "12:00:00");
        assertNotNull(part);
    }

    @Test
    void newUuidPart() {
        var part = Parameters.newUuidPart(CTX, "uuid", "urn:uuid:12345");
        assertNotNull(part);
    }

    @Test
    void getSingularStringPart() {
        var params = new org.hl7.fhir.r4.model.Parameters();
        params.addParameter().setName("test").setValue(new StringType("hello"));
        var result = Parameters.getSingularStringPart(CTX, params, "test");
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    void getSingularStringPartNotFound() {
        var params = new org.hl7.fhir.r4.model.Parameters();
        var result = Parameters.getSingularStringPart(CTX, params, "missing");
        assertTrue(result.isEmpty());
    }

    @Test
    void getPartsByName() {
        var params = new org.hl7.fhir.r4.model.Parameters();
        params.addParameter().setName("test").setValue(new StringType("hello"));
        var result = Parameters.getPartsByName(CTX, params, "test");
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
