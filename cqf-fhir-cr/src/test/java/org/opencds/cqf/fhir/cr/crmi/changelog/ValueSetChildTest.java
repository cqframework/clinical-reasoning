package org.opencds.cqf.fhir.cr.crmi.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor;

class ValueSetChildTest {

    /**
     * Every code system eRSD content actually uses. RxNorm and CVX were missing, which left the
     * changelog's Code System and Code System OID columns empty for some groupers.
     */
    @ParameterizedTest
    @CsvSource({
        "http://snomed.info/sct, SNOMEDCT, 2.16.840.1.113883.6.96",
        "http://loinc.org, LOINC, 2.16.840.1.113883.6.1",
        "http://hl7.org/fhir/sid/icd-10-cm, ICD10CM, 2.16.840.1.113883.6.90",
        "http://hl7.org/fhir/sid/icd-9-cm, ICD9CM, '2.16.840.1.113883.6.103, 2.16.840.1.113883.6.104'",
        "http://www.nlm.nih.gov/research/umls/rxnorm, RXNORM, 2.16.840.1.113883.6.88",
        "http://hl7.org/fhir/sid/cvx, CVX, 2.16.840.1.113883.12.292"
    })
    void resolvesNameAndOidForEachCodeSystem(String systemUrl, String name, String oid) {
        assertEquals(name, ValueSetChild.Code.getCodeSystemName(systemUrl));
        assertEquals(oid, ValueSetChild.Code.getCodeSystemOid(systemUrl));
    }

    /** An unrecognised system yields nothing rather than a misleading label. */
    @Test
    void unknownCodeSystemResolvesToNull() {
        assertNull(ValueSetChild.Code.getCodeSystemName("http://example.org/CodeSystem/local"));
        assertNull(ValueSetChild.Code.getCodeSystemOid("http://example.org/CodeSystem/local"));
    }

    /** The system is optional on an expansion entry, so the lookup has to tolerate null. */
    @Test
    void nullCodeSystemResolvesToNull() {
        assertNull(ValueSetChild.Code.getCodeSystemName(null));
        assertNull(ValueSetChild.Code.getCodeSystemOid(null));
    }

    /**
     * Proves the wiring, not just the lookup: a code assembled through the changelog carries the name
     * and OID onto the Code the spreadsheet reads.
     */
    @Test
    void changelogCodeCarriesRxNormNameAndOid() {
        var url = "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1913";
        var valueSet = new ValueSet();
        valueSet.setUrl(url).setVersion("20250218").setName("Medications").setTitle("Medications");
        valueSet.getCompose()
                .addInclude()
                .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                .addConcept()
                .setCode("2710424");
        valueSet.getExpansion()
                .addContains()
                .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
                .setCode("2710424")
                .setVersion("2026-01");

        var page = new ChangeLog(url).addPage(valueSet, valueSet.copy(), (ArtifactDiffProcessor.DiffCache) null);
        var newData = page.getNewData();
        assertNotNull(newData);
        assertEquals(1, newData.getCodes().size());

        var code = newData.getCodes().get(0);
        assertEquals("RXNORM", code.getCodeSystemName());
        assertEquals("2.16.840.1.113883.6.88", code.getCodeSystemOid());
        assertEquals("2026-01", code.getVersion());
    }
}
