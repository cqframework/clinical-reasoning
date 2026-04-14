package org.opencds.cqf.fhir.utility.terminology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodeSystemsTest {

    @Test
    void returnsFalseForNull() {
        assertFalse(CodeSystems.isKnownCodeSystem(null));
    }

    @Test
    void returnsFalseForEmpty() {
        assertFalse(CodeSystems.isKnownCodeSystem(""));
    }

    @Test
    void returnsFalseForUnknownUrl() {
        assertFalse(CodeSystems.isKnownCodeSystem("http://example.org/CodeSystem/unknown"));
    }

    @Test
    void matchesExternalCodeSystems() {
        assertTrue(CodeSystems.isKnownCodeSystem("https://www.usps.com/"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://www.nubc.org/patient-discharge"));
        assertTrue(CodeSystems.isKnownCodeSystem("https://nahdo.org/sopt"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://www.ada.org/snodent"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://loinc.org"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://snomed.info/sct"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://www.nlm.nih.gov/research/umls/rxnorm"));
    }

    @Test
    void matchesFhirSidCodeSystems() {
        assertTrue(CodeSystems.isKnownCodeSystem("http://hl7.org/fhir/sid/icd-9-cm"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://hl7.org/fhir/sid/icd-10-cm"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://hl7.org/fhir/sid/cvx"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://hl7.org/fhir/sid/ndc"));
    }

    @Test
    void matchesThoCodeSystems() {
        assertTrue(CodeSystems.isKnownCodeSystem("http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://terminology.hl7.org/CodeSystem/icd10CM"));
        assertTrue(CodeSystems.isKnownCodeSystem("http://terminology.hl7.org/CodeSystem/CVX"));
    }

    @Test
    void matchesUrnCodeSystems() {
        assertTrue(CodeSystems.isKnownCodeSystem("urn:ietf:bcp:47"));
        assertTrue(CodeSystems.isKnownCodeSystem("urn:iso:std:iso:3166"));
        assertTrue(CodeSystems.isKnownCodeSystem("urn:oid:2.16.840.1.113883.6.238"));
        assertTrue(CodeSystems.isKnownCodeSystem("urn:iso:std:iso:4217"));
    }
}
