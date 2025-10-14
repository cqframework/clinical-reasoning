package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.r4.model.CanonicalType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Canonicals.CanonicalParts;

class CanonicalsTest {
    @Test
    void fullCanonicalUrl() {
        String testUrl = "http://fhir.acme.com/Questionnaire/example|1.0#vs1";

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals("http://fhir.acme.com", Canonicals.getCanonicalBase(testUrl, Canonicals.getResourceType(testUrl)));
        assertEquals("example", Canonicals.getTail(testUrl));
        assertEquals("1.0", Canonicals.getVersion(testUrl));
        assertEquals("vs1", Canonicals.getFragment(testUrl));
    }

    @Test
    void fullCanonicalUrlWithoutHash() {
        String testUrl = "http://fhir.acme.com/Questionnaire/example|1.0";

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals("http://fhir.acme.com", Canonicals.getCanonicalBase(testUrl, Canonicals.getResourceType(testUrl)));
        assertEquals("example", Canonicals.getTail(testUrl));
        assertEquals("1.0", Canonicals.getVersion(testUrl));
    }

    @Test
    void partialCanonicalUrl() {
        String testUrl = "http://fhir.acme.com/Questionnaire/example";

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals("http://fhir.acme.com", Canonicals.getCanonicalBase(testUrl, Canonicals.getResourceType(testUrl)));
        assertEquals("example", Canonicals.getTail(testUrl));
        assertNull(Canonicals.getVersion(testUrl));
        assertNull(Canonicals.getFragment(testUrl));
    }

    @Test
    void fullCanonicalType() {
        CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example|1.0#vs1");

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals(
                "http://fhir.acme.com",
                Canonicals.getCanonicalBase(testUrl.asStringValue(), Canonicals.getResourceType(testUrl)));
        assertEquals("example", Canonicals.getTail(testUrl));
        assertEquals("1.0", Canonicals.getVersion(testUrl));
        assertEquals("vs1", Canonicals.getFragment(testUrl));
    }

    @Test
    void partialCanonicalType() {
        CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example");

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals(
                "http://fhir.acme.com",
                Canonicals.getCanonicalBase(testUrl.asStringValue(), Canonicals.getResourceType(testUrl)));
        assertEquals("Questionnaire", Canonicals.getResourceType(testUrl));
        assertEquals("example", Canonicals.getTail(testUrl));
        assertNull(Canonicals.getVersion(testUrl));
        assertNull(Canonicals.getFragment(testUrl));
    }

    @Test
    void canonicalParts() {
        CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example|1.0#vs1");

        CanonicalParts parts = Canonicals.getParts(testUrl);

        assertEquals("http://fhir.acme.com/Questionnaire/example", parts.url());
        assertEquals(
                "http://fhir.acme.com",
                Canonicals.getCanonicalBase(testUrl.asStringValue(), Canonicals.getResourceType(testUrl)));
        assertEquals("Questionnaire", parts.resourceType());
        assertEquals("example", parts.tail());
        assertEquals("1.0", parts.version());
        assertEquals("vs1", parts.fragment());
    }
}
