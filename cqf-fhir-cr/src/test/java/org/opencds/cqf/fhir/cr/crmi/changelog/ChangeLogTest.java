package org.opencds.cqf.fhir.cr.crmi.changelog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
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

    /** A leaf carrying one code, with the version and display that side should report. */
    private static ValueSet leafWithCode(String expansionVersion, String display) {
        var valueSet = new ValueSet();
        valueSet.setUrl(LEAF_URL);
        valueSet.setVersion("20240619");
        valueSet.setName("WestNileVirusRNA");
        valueSet.setTitle("West Nile Virus RNA");
        // display is read from compose.include.concept
        valueSet.getCompose()
                .addInclude()
                .setSystem(LOINC)
                .addConcept()
                .setCode(CODE)
                .setDisplay(display);
        valueSet.getExpansion()
                .addContains()
                .setSystem(LOINC)
                .setCode(CODE)
                .setVersion(expansionVersion)
                .setDisplay(display);
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

    /**
     * Each side gets its own code map, so a code present in both expansions becomes two Code objects -
     * each carrying the version, display and leaf attribution of its own side.
     */
    @Test
    void eachSideCarriesItsOwnCodeValues() {
        var source = leafWithCode("2.76", "Old display");
        var target = leafWithCode("2.81", "New display");
        var page = new ChangeLog(LEAF_URL).addPage(source, target, (ArtifactDiffProcessor.DiffCache) null);

        var oldCode = page.getOldData().getCodes().get(0);
        var newCode = page.getNewData().getCodes().get(0);

        assertNotSame(oldCode, newCode);
        assertEquals("2.76", oldCode.getVersion());
        assertEquals("2.81", newCode.getVersion());
        assertEquals("Old display", oldCode.getDisplay());
        assertEquals("New display", newCode.getDisplay());
    }

    /** An operation on one side must not appear on the other now that the instances are separate. */
    @Test
    void anOperationOnOneSideDoesNotLeakToTheOther() {
        var source = leafWithCode("2.76", "Old display");
        var target = leafWithCode("2.81", "New display");
        var page = new ChangeLog(LEAF_URL).addPage(source, target, (ArtifactDiffProcessor.DiffCache) null);

        page.addOperation(
                ChangeLog.DELETE,
                "ValueSet.expansion.contains[0]",
                null,
                source.getExpansion().getContains().get(0));

        assertEquals(
                ChangeLog.DELETE,
                page.getOldData().getCodes().get(0).getOperation().getType());
        assertNull(page.getNewData().getCodes().get(0).getOperation());
    }

    /**
     * The one route by which a single Code can still receive two operations at the same type and path:
     * the whole-expansion branch fans over every contains entry using path "ValueSet.expansion", and two
     * entries sharing a code value resolve to the same Code. Both carry the same value, so this is one
     * change reported twice, not a contradiction - it must not abort the changelog.
     */
    @Test
    void twoExpansionEntriesSharingACodeDoNotAbortTheChangelog() {
        var valueSet = new ValueSet();
        valueSet.setUrl(LEAF_URL);
        valueSet.setVersion("20240619");
        valueSet.setName("WestNileVirusRNA");
        valueSet.setTitle("West Nile Virus RNA");
        valueSet.getCompose().addInclude().setSystem(LOINC).addConcept().setCode(CODE);
        // The same code under two systems, which the code map collapses to one Code.
        var equalButDistinctCode = new StringBuilder(CODE).toString();
        valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE).setVersion("2.81");
        valueSet.getExpansion()
                .addContains()
                .setSystem("http://hl7.org/fhir/sid/icd-10-cm")
                .setCode(equalButDistinctCode)
                .setVersion("2026");

        var page = new ChangeLog(LEAF_URL).addPage(valueSet, valueSet.copy(), (ArtifactDiffProcessor.DiffCache) null);

        assertDoesNotThrow(() -> page.addOperation(
                ChangeLog.REPLACE, "ValueSet.expansion", valueSet.getExpansion(), valueSet.getExpansion()));
    }

    /** A genuinely conflicting value at one path is still a contradiction and must raise. */
    @Test
    void twoDifferentValuesAtTheSameCodePathStillRaise() {
        var page = new ChangeLog(LEAF_URL)
                .addPage(leafWithCode("2.76", "d"), leafWithCode("2.76", "d"), (ArtifactDiffProcessor.DiffCache) null);
        var code = page.getNewData().getCodes().get(0);

        code.setOperation(new Operation(ChangeLog.REPLACE, "ValueSet.expansion.contains[0].display", "first", null));

        var second = new Operation(ChangeLog.REPLACE, "ValueSet.expansion.contains[0].display", "second", null);
        assertThrows(UnprocessableEntityException.class, () -> code.setOperation(second));
    }

    /** A leaf whose single expansion entry states an active status, or leaves it absent when null. */
    private static ValueSet leafWithInactive(Boolean inactive) {
        var valueSet = leafWithCode("2.81", "West Nile virus RNA");
        if (inactive != null) {
            valueSet.getExpansion().getContainsFirstRep().setInactive(inactive);
        }
        return valueSet;
    }

    /**
     * Only expansion.contains carries inactive, and compose.include claims the code first, so the status
     * has to reach the Code by the same fallback the code system version uses.
     */
    @Test
    void codeTakesInactiveFromExpansionWhenComposeClaimedItFirst() {
        assertEquals(Boolean.TRUE, getChangelogCode(leafWithInactive(true)).getInactive());
        assertEquals(Boolean.FALSE, getChangelogCode(leafWithInactive(false)).getInactive());
    }

    /** An absent inactive means the status was never stated - not that the code is active. */
    @Test
    void codeInactiveIsNullWhenTheExpansionDoesNotStateIt() {
        assertNull(getChangelogCode(leafWithInactive(null)).getInactive());
    }

    /** A code active on one side and inactive on the other must report each side's own status. */
    @Test
    void eachSideCarriesItsOwnInactiveFlag() {
        var page = new ChangeLog(LEAF_URL)
                .addPage(leafWithInactive(false), leafWithInactive(true), (ArtifactDiffProcessor.DiffCache) null);

        assertEquals(Boolean.FALSE, page.getOldData().getCodes().get(0).getInactive());
        assertEquals(Boolean.TRUE, page.getNewData().getCodes().get(0).getInactive());
    }

    /**
     * A code can appear more than once in an expansion, under different systems. The fallback fills each
     * field from the first entry that states it, so an entry with no version must not block a later one
     * that has it - nor must collecting the status reintroduce that.
     */
    @Test
    void expansionFallbackTakesEachFieldFromTheFirstEntryThatStatesIt() {
        // Both orderings matter and they fail differently: a silent first entry must not block a later
        // one that speaks, and a silent later entry must not overwrite what the first already stated.
        for (var statedOnFirstEntry : new boolean[] {true, false}) {
            var code = firstCodeOf(twoContainsEntriesOneSilent(statedOnFirstEntry));

            assertEquals("2.81", code.getVersion(), "version, stated first: " + statedOnFirstEntry);
            assertEquals(Boolean.TRUE, code.getInactive(), "inactive, stated first: " + statedOnFirstEntry);
        }
    }

    /** One code, twice in the expansion - only one of the two entries states a version and a status. */
    private static ValueSet twoContainsEntriesOneSilent(boolean statedOnFirstEntry) {
        var valueSet = new ValueSet();
        valueSet.setUrl(LEAF_URL);
        valueSet.setVersion("20240619");
        valueSet.setName("WestNileVirusRNA");
        valueSet.setTitle("West Nile Virus RNA");
        valueSet.getCompose().addInclude().setSystem(LOINC).addConcept().setCode(CODE);
        var first = valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE);
        var second = valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE);
        (statedOnFirstEntry ? first : second).setVersion("2.81").setInactive(true);
        return valueSet;
    }

    /** Both contains entries resolve to the one Code the map holds for that code value. */
    private static ValueSetChild.Code firstCodeOf(ValueSet valueSet) {
        return new ChangeLog(LEAF_URL)
                .addPage(valueSet, valueSet.copy(), (ArtifactDiffProcessor.DiffCache) null)
                .getNewData()
                .getCodes()
                .get(0);
    }

    /**
     * A code going inactive is a boolean change at an expansion.contains path. Every other value the diff
     * reports there is a string, so this is the first non-string primitive to reach that branch.
     */
    @Test
    void aCodeGoingInactiveDoesNotAbortTheChangelog() {
        var source = leafWithInactive(false);
        var target = leafWithInactive(true);
        var page = new ChangeLog(LEAF_URL).addPage(source, target, (ArtifactDiffProcessor.DiffCache) null);

        assertDoesNotThrow(() -> page.addOperation(
                ChangeLog.REPLACE,
                "ValueSet.expansion.contains[0].inactive",
                new org.hl7.fhir.r4.model.BooleanType(true),
                new org.hl7.fhir.r4.model.BooleanType(false)));
    }

    /**
     * Every relatedArtifact insert carries the collection path. Multiple inserts can resolve to one
     * entry - it looks identical to a contradiction. It must not abort the changelog.
     */
    @Test
    void repeatedRelatedArtifactInsertAtTheCollectionPathDoesNotAbortTheChangelog() {
        var canonical = "http://ersd.aimsplatform.org/fhir/ValueSet/eltc|3.2.0";
        var dependsOn = new org.hl7.fhir.r4.model.RelatedArtifact()
                .setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource(canonical);
        var source = new org.hl7.fhir.r4.model.Library();
        source.setUrl("http://example.org/Library/manifest").setVersion("3.1.2").setName("m");
        var target = new org.hl7.fhir.r4.model.Library();
        target.setUrl("http://example.org/Library/manifest").setVersion("3.2.0").setName("m");
        target.addRelatedArtifact(dependsOn);

        var page = new ChangeLog("http://example.org/Library/manifest").addPage(source, target);

        assertDoesNotThrow(() -> {
            page.addOperation(ChangeLog.INSERT, "relatedArtifact", dependsOn.copy(), null);
            page.addOperation(ChangeLog.INSERT, "relatedArtifact", dependsOn.copy(), null);
        });
        assertEquals(
                ChangeLog.INSERT,
                page.getNewData().getRelatedArtifacts().get(0).getOperation().getType());
    }
}
