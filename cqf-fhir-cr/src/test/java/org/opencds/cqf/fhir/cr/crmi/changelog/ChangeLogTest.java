package org.opencds.cqf.fhir.cr.crmi.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor;

class ChangeLogTest {

    private static final String LEAF_URL = "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.560";
    private static final String LOINC = "http://loinc.org";
    private static final String CODE = "103721-7";

    /**
     * Builds a leaf ValueSet the way eRSD content actually arrives: compose.include lists the concepts
     * but carries no version, while the expansion records the code system version per code.
     *
     * @param composeVersion version to put on compose.include, or null to leave it off
     * @param expansionVersion version to put on expansion.contains, or null to leave it off
     */
    private static ValueSet leaf(String composeVersion, String expansionVersion) {
        var valueSet = new ValueSet();
        valueSet.setUrl(LEAF_URL);
        valueSet.setVersion("20240619");
        valueSet.setName("WestNileVirusRNA");
        valueSet.setTitle("West Nile Virus RNA");

        var include = valueSet.getCompose().addInclude().setSystem(LOINC);
        include.addConcept().setCode(CODE);
        if (composeVersion != null) {
            include.setVersion(composeVersion);
        }

        var contains = valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE);
        if (expansionVersion != null) {
            contains.setVersion(expansionVersion);
        }
        return valueSet;
    }

    private static ValueSetChild.Code getChangelogCode(ValueSet valueSet) {
        var page = new ChangeLog(LEAF_URL).addPage(valueSet, valueSet.copy(), (ArtifactDiffProcessor.DiffCache) null);
        var newData = page.getNewData();
        assertNotNull(newData);
        assertEquals(1, newData.getCodes().size());
        return newData.getCodes().get(0);
    }

    /**
     * compose.include never carries a version in eRSD content, so the code map has to fall back to the
     * version the expansion recorded. Without the fallback the compose entry claimed the code with a
     * null version and the expansion entry was skipped, leaving the changelog's Code System Version
     * column empty for every code.
     */
    @Test
    void codeTakesCodeSystemVersionFromExpansionWhenComposeHasNone() {
        assertEquals("2.81", getChangelogCode(leaf(null, "2.81")).getVersion());
    }

    /** An explicit version on compose.include is authoritative and must not be overwritten. */
    @Test
    void codeKeepsComposeVersionWhenOneIsPresent() {
        assertEquals("2.76", getChangelogCode(leaf("2.76", "2.81")).getVersion());
    }

    /** Nothing to fall back to - the version stays null rather than throwing. */
    @Test
    void codeVersionIsNullWhenNeitherSideHasOne() {
        assertNull(getChangelogCode(leaf(null, null)).getVersion());
    }
}
