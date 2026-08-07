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
        assertEquals("example", Canonicals.getIdPart(testUrl));
        assertEquals("1.0", Canonicals.getVersion(testUrl));
        assertEquals("vs1", Canonicals.getFragment(testUrl));
    }

    @Test
    void fullCanonicalUrlWithoutHash() {
        String testUrl = "http://fhir.acme.com/Questionnaire/example|1.0";

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals("example", Canonicals.getIdPart(testUrl));
        assertEquals("1.0", Canonicals.getVersion(testUrl));
    }

    @Test
    void partialCanonicalUrl() {
        String testUrl = "http://fhir.acme.com/Questionnaire/example";

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals("example", Canonicals.getIdPart(testUrl));
        assertNull(Canonicals.getVersion(testUrl));
        assertNull(Canonicals.getFragment(testUrl));
    }

    @Test
    void fullCanonicalType() {
        CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example|1.0#vs1");

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals("example", Canonicals.getIdPart(testUrl));
        assertEquals("1.0", Canonicals.getVersion(testUrl));
        assertEquals("vs1", Canonicals.getFragment(testUrl));
    }

    @Test
    void partialCanonicalType() {
        CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example");

        assertEquals("http://fhir.acme.com/Questionnaire/example", Canonicals.getUrl(testUrl));
        assertEquals("Questionnaire", Canonicals.getResourceType(testUrl));
        assertEquals("example", Canonicals.getIdPart(testUrl));
        assertNull(Canonicals.getVersion(testUrl));
        assertNull(Canonicals.getFragment(testUrl));
    }

    @Test
    void selfReferentialUrlResolvesResourceType() {
        // Regression: previously String.replace() stripped both occurrences of
        // "/StructureDefinition" and returned "fhir" for this self-referential URL.
        String selfRef = "http://hl7.org/fhir/StructureDefinition/StructureDefinition";

        assertEquals("StructureDefinition", Canonicals.getResourceType(selfRef));
        assertEquals("StructureDefinition", Canonicals.getIdPart(selfRef));
        assertEquals(selfRef, Canonicals.getUrl(selfRef));
    }

    @Test
    void versionContainingSlashesResolvesIdPart() {
        // SNOMED CT versions are URIs. Previously, searching the full canonical for the last "/"
        // found one inside the version and produced a start index past the end of the url part,
        // throwing StringIndexOutOfBoundsException.
        String snomed = "http://snomed.info/sct|http://snomed.info/sct/731000124108/version/20250901";

        assertEquals("sct", Canonicals.getIdPart(snomed));
        assertEquals("http://snomed.info/sct", Canonicals.getUrl(snomed));
        assertEquals("http://snomed.info/sct/731000124108/version/20250901", Canonicals.getVersion(snomed));
    }

    @Test
    void versionContainingSlashesDoesNotResolveIdPartForUrn() {
        // The url part has no "/" at all, so there is no id to resolve. The slashes belong purely
        // to the version and must not be mistaken for a path separator.
        String urn = "urn:oid:2.16.840.1.113762.1.4.1146.6|http://example.org/v/1";

        assertNull(Canonicals.getIdPart(urn));
        assertEquals("urn:oid:2.16.840.1.113762.1.4.1146.6", Canonicals.getUrl(urn));
    }

    @Test
    void canonicalParts() {
        CanonicalType testUrl = new CanonicalType("http://fhir.acme.com/Questionnaire/example|1.0#vs1");

        CanonicalParts parts = Canonicals.getParts(testUrl);

        assertEquals("http://fhir.acme.com/Questionnaire/example", parts.url());
        assertEquals("Questionnaire", parts.resourceType());
        assertEquals("example", parts.idPart());
        assertEquals("1.0", parts.version());
        assertEquals("vs1", parts.fragment());
    }
}
