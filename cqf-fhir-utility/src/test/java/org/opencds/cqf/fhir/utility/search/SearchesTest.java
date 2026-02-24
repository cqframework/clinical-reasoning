package org.opencds.cqf.fhir.utility.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.param.UriParam;
import org.junit.jupiter.api.Test;

class SearchesTest {

    @Test
    void allIsEmptyMap() {
        assertTrue(Searches.ALL.isEmpty());
    }

    @Test
    void byIdSingle() {
        var result = Searches.byId("123");
        assertEquals(1, result.size());
        assertTrue(result.containsKey("_id"));
        assertEquals(1, result.get("_id").size());
        assertTrue(result.get("_id").get(0) instanceof TokenParam);
    }

    @Test
    void byIdMultiple() {
        var result = Searches.byId("1", "2", "3");
        assertEquals(1, result.size());
        assertEquals(3, result.get("_id").size());
    }

    @Test
    void byProfile() {
        var result = Searches.byProfile("http://example.org/profile");
        assertEquals(1, result.size());
        assertTrue(result.containsKey("_profile"));
        assertTrue(result.get("_profile").get(0) instanceof UriParam);
    }

    @Test
    void byCanonicalWithVersion() {
        var result = Searches.byCanonical("http://example.org/Library/test|1.0");
        assertTrue(result.containsKey("url"));
        assertTrue(result.containsKey("version"));
    }

    @Test
    void byCanonicalWithoutVersion() {
        var result = Searches.byCanonical("http://example.org/Library/test");
        assertTrue(result.containsKey("url"));
        assertEquals(1, result.size());
    }

    @Test
    void byCodeAndSystem() {
        var result = Searches.byCodeAndSystem("test-code", "http://example.org");
        assertTrue(result.containsKey("code"));
        var param = (TokenParam) result.get("code").get(0);
        assertEquals("test-code", param.getValue());
        assertEquals("http://example.org", param.getSystem());
    }

    @Test
    void byUrl() {
        var result = Searches.byUrl("http://example.org/Library/test");
        assertTrue(result.containsKey("url"));
        assertTrue(result.get("url").get(0) instanceof UriParam);
    }

    @Test
    void byUrlAndVersion() {
        var result = Searches.byUrlAndVersion("http://example.org/Library/test", "1.0");
        assertTrue(result.containsKey("url"));
        assertTrue(result.containsKey("version"));
    }

    @Test
    void byName() {
        var result = Searches.byName("TestLibrary");
        assertTrue(result.containsKey("name"));
        assertTrue(result.get("name").get(0) instanceof StringParam);
    }

    @Test
    void byStatus() {
        var result = Searches.byStatus("active");
        assertTrue(result.containsKey("status"));
        var param = (TokenParam) result.get("status").get(0);
        assertEquals("active", param.getValue());
    }

    @Test
    void exceptStatus() {
        var result = Searches.exceptStatus("retired");
        assertTrue(result.containsKey("status"));
        var param = (TokenParam) result.get("status").get(0);
        assertEquals("retired", param.getValue());
        assertEquals(TokenParamModifier.NOT, param.getModifier());
    }

    @Test
    void byNameAndVersion() {
        var result = Searches.byNameAndVersion("TestLib", "2.0");
        assertTrue(result.containsKey("name"));
        assertTrue(result.containsKey("version"));
    }

    @Test
    void byNameAndVersionNullVersion() {
        var result = Searches.byNameAndVersion("TestLib", null);
        assertTrue(result.containsKey("name"));
        assertEquals(1, result.size());
    }

    @Test
    void byNameAndVersionEmptyVersion() {
        var result = Searches.byNameAndVersion("TestLib", "");
        assertTrue(result.containsKey("name"));
        assertEquals(1, result.size());
    }

    @Test
    void builderWithReferenceParam() {
        var result =
                Searches.builder().withReferenceParam("patient", "Patient/123").build();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPatientSearchParamDstu3() {
        assertEquals("_id", Searches.getPatientSearchParam(FhirVersionEnum.DSTU3, "Patient"));
        assertEquals("member", Searches.getPatientSearchParam(FhirVersionEnum.DSTU3, "Group"));
        assertEquals("individual", Searches.getPatientSearchParam(FhirVersionEnum.DSTU3, "ResearchSubject"));
        assertEquals("actor", Searches.getPatientSearchParam(FhirVersionEnum.DSTU3, "Appointment"));
        assertEquals("subject", Searches.getPatientSearchParam(FhirVersionEnum.DSTU3, "Observation"));
        assertEquals("patient", Searches.getPatientSearchParam(FhirVersionEnum.DSTU3, "Encounter"));
        assertNull(Searches.getPatientSearchParam(FhirVersionEnum.DSTU3, "Organization"));
    }

    @Test
    void getPatientSearchParamR4() {
        assertEquals("_id", Searches.getPatientSearchParam(FhirVersionEnum.R4, "Patient"));
        assertEquals("member", Searches.getPatientSearchParam(FhirVersionEnum.R4, "Group"));
        assertEquals("individual", Searches.getPatientSearchParam(FhirVersionEnum.R4, "ResearchSubject"));
        assertEquals("actor", Searches.getPatientSearchParam(FhirVersionEnum.R4, "Appointment"));
        assertEquals("subject", Searches.getPatientSearchParam(FhirVersionEnum.R4, "Observation"));
        assertEquals("patient", Searches.getPatientSearchParam(FhirVersionEnum.R4, "Encounter"));
        assertEquals("policy-holder", Searches.getPatientSearchParam(FhirVersionEnum.R4, "Coverage"));
        assertNull(Searches.getPatientSearchParam(FhirVersionEnum.R4, "Organization"));
    }

    @Test
    void getPatientSearchParamR5() {
        assertEquals("_id", Searches.getPatientSearchParam(FhirVersionEnum.R5, "Patient"));
        assertEquals("subject", Searches.getPatientSearchParam(FhirVersionEnum.R5, "RequestOrchestration"));
    }
}
