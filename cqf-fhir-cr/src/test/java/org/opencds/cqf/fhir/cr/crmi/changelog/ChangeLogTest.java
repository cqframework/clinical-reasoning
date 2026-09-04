package org.opencds.cqf.fhir.cr.crmi.changelog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.crmi.TransformProperties.CRMI_INTENDED_USAGE_CONTEXT_EXT_URL;
import static org.opencds.cqf.fhir.cr.crmi.TransformProperties.usPHUsageContext;
import static org.opencds.cqf.fhir.cr.crmi.TransformProperties.usPHUsageContextType;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor.DiffCache;

class ChangeLogTest {

    private static final String GROUPER_URL = "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc";
    private static final String MANIFEST_URL = "http://example.org/Library/manifest";
    private static final String LEAF_URL = "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.560";
    private static final String LEAF_CANONICAL = LEAF_URL + "|20240619";
    private static final String LOINC = "http://loinc.org";
    private static final String SNOMED = "http://snomed.info/sct";
    private static final String CODE = "103721-7";

    // code system version and status

    /**
     * compose.include never carries a version in eRSD content, so the code map has to fall back to the
     * version the expansion recorded. Without the fallback the compose entry claimed the code with a
     * null version and the expansion entry was skipped, leaving the changelog's Code System Version
     * column empty for every code.
     */
    @Test
    void codeTakesCodeSystemVersionFromExpansionWhenComposeHasNone() {
        assertEquals("2.81", onlyCodeOf(leaf(null, "2.81")).getVersion());
    }

    /** An explicit version on compose.include is authoritative and must not be overwritten. */
    @Test
    void codeKeepsComposeVersionWhenOneIsPresent() {
        assertEquals("2.76", onlyCodeOf(leaf("2.76", "2.81")).getVersion());
    }

    /** Nothing to fall back to - the version stays null rather than throwing. */
    @Test
    void codeVersionIsNullWhenNeitherSideHasOne() {
        assertNull(onlyCodeOf(leaf(null, null)).getVersion());
    }

    /**
     * Only expansion.contains carries inactive, and compose.include claims the code first, so the status
     * has to reach the Code by the same fallback the code system version uses.
     */
    @Test
    void codeTakesInactiveFromExpansionWhenComposeClaimedItFirst() {
        assertEquals(Boolean.TRUE, onlyCodeOf(leafWithInactive(true)).getInactive());
        assertEquals(Boolean.FALSE, onlyCodeOf(leafWithInactive(false)).getInactive());
    }

    /** An absent inactive means the status was never stated - not that the code is active. */
    @Test
    void codeInactiveIsNullWhenTheExpansionDoesNotStateIt() {
        assertNull(onlyCodeOf(leafWithInactive(null)).getInactive());
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

    // one code map per side

    /**
     * Each side gets its own code map, so a code present in both expansions becomes two Code objects -
     * each carrying the version, display and leaf attribution of its own side.
     */
    @Test
    void eachSideCarriesItsOwnCodeValues() {
        var page = pageFor(leafWithCode("2.76", "Old display"), leafWithCode("2.81", "New display"));

        var oldCode = page.getOldData().getCodes().get(0);
        var newCode = page.getNewData().getCodes().get(0);

        assertNotSame(oldCode, newCode);
        assertEquals("2.76", oldCode.getVersion());
        assertEquals("2.81", newCode.getVersion());
        assertEquals("Old display", oldCode.getDisplay());
        assertEquals("New display", newCode.getDisplay());
    }

    /** A code active on one side and inactive on the other must report each side's own status. */
    @Test
    void eachSideCarriesItsOwnInactiveFlag() {
        var page = pageFor(leafWithInactive(false), leafWithInactive(true));

        assertEquals(Boolean.FALSE, page.getOldData().getCodes().get(0).getInactive());
        assertEquals(Boolean.TRUE, page.getNewData().getCodes().get(0).getInactive());
    }

    /** An operation on one side must not appear on the other now that the instances are separate. */
    @Test
    void anOperationOnOneSideDoesNotLeakToTheOther() {
        var source = leafWithCode("2.76", "Old display");
        var page = pageFor(source, leafWithCode("2.81", "New display"));

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

    // operations that must not abort changelog

    /**
     * The one route by which a single Code can still receive two operations at the same type and path:
     * the whole-expansion branch fans over every contains entry using path "ValueSet.expansion", and two
     * entries sharing a code value resolve to the same Code. Both carry the same value, so this is one
     * change reported twice, not a contradiction - it must not abort the changelog.
     */
    @Test
    void twoExpansionEntriesSharingACodeDoNotAbortTheChangelog() {
        var valueSet = emptyLeaf();
        valueSet.getCompose().addInclude().setSystem(LOINC).addConcept().setCode(CODE);
        // The same code under two systems, which the code map collapses to one Code.
        var equalButDistinctCode = new StringBuilder(CODE).toString();
        valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE).setVersion("2.81");
        valueSet.getExpansion()
                .addContains()
                .setSystem("http://hl7.org/fhir/sid/icd-10-cm")
                .setCode(equalButDistinctCode)
                .setVersion("2026");

        var page = pageFor(valueSet, valueSet.copy());

        assertDoesNotThrow(() -> page.addOperation(
                ChangeLog.REPLACE, "ValueSet.expansion", valueSet.getExpansion(), valueSet.getExpansion()));
    }

    /** A genuinely conflicting value at one path is still a contradiction and must raise. */
    @Test
    void twoDifferentValuesAtTheSameCodePathStillRaise() {
        var code = pageFor(leafWithCode("2.76", "d"), leafWithCode("2.76", "d"))
                .getNewData()
                .getCodes()
                .get(0);

        code.setOperation(new Operation(ChangeLog.REPLACE, "ValueSet.expansion.contains[0].display", "first", null));

        var second = new Operation(ChangeLog.REPLACE, "ValueSet.expansion.contains[0].display", "second", null);
        assertThrows(UnprocessableEntityException.class, () -> code.setOperation(second));
    }

    /**
     * A code going inactive is a boolean change at an expansion.contains path. Every other value the diff
     * reports there is a string, so this is the first non-string primitive to reach that branch.
     */
    @Test
    void aCodeGoingInactiveDoesNotAbortTheChangelog() {
        var page = pageFor(leafWithInactive(false), leafWithInactive(true));

        assertDoesNotThrow(() -> page.addOperation(
                ChangeLog.REPLACE,
                "ValueSet.expansion.contains[0].inactive",
                new BooleanType(true),
                new BooleanType(false)));
    }

    /**
     * Every relatedArtifact insert carries the collection path. Multiple inserts can resolve to one
     * entry - it looks identical to a contradiction.
     */
    @Test
    void repeatedRelatedArtifactInsertAtTheCollectionPathDoesNotAbortTheChangelog() {
        var dependsOn = new RelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://ersd.aimsplatform.org/fhir/ValueSet/eltc|3.2.0");
        var target = library("3.2.0");
        target.addRelatedArtifact(dependsOn);

        var page = new ChangeLog(MANIFEST_URL).addPage(library("3.1.2"), target);

        assertDoesNotThrow(() -> {
            page.addOperation(ChangeLog.INSERT, "relatedArtifact", dependsOn.copy(), null);
            page.addOperation(ChangeLog.INSERT, "relatedArtifact", dependsOn.copy(), null);
        });
        assertEquals(
                ChangeLog.INSERT,
                page.getNewData().getRelatedArtifacts().get(0).getOperation().getType());
    }

    //  reading conditions and priority off a manifest

    /** Conditions come off the focus usage contexts - previously always empty. */
    @Test
    void conditionsAreReadFromTheIntendedUsageContextExtension() {
        var conditions = manifestChildFor(relatedArtifact(null, "840539006", "27836007"))
                .getRelatedArtifacts()
                .get(0)
                .getConditions();

        assertEquals(2, conditions.size());
        assertEquals(
                "840539006", conditions.get(0).getValue().getCodingFirstRep().getCode());
        assertEquals(
                "27836007", conditions.get(1).getValue().getCodingFirstRep().getCode());
    }

    /** A stated priority must win over the "routine" default that used to fill every row. */
    @Test
    void priorityIsReadFromTheIntendedUsageContextExtension() {
        assertEquals("emergent", priorityOf(relatedArtifact("emergent")));
    }

    /** With no priority stated, the documented "routine" default still applies. */
    @Test
    void priorityFallsBackToRoutineWhenNoneIsStated() {
        assertEquals("routine", priorityOf(relatedArtifact(null)));
    }

    /** The priority usage context must not be collected as a condition, nor the other way round. */
    @Test
    void theTwoUsageContextCodesDoNotCrossOver() {
        var entry = manifestChildFor(relatedArtifact("emergent", "840539006"))
                .getRelatedArtifacts()
                .get(0);

        assertEquals(1, entry.getConditions().size());
        assertEquals(
                "840539006",
                entry.getConditions().get(0).getValue().getCodingFirstRep().getCode());
        assertEquals(
                "emergent", entry.getPriority().getValue().getCodingFirstRep().getCode());
    }

    /**
     * UsageContext.value is a choice type - CodeableConcept, Quantity, Range or Reference. A usage
     * context carrying a non-concept value has to be skipped rather than read.
     */
    @Test
    void aUsageContextWithANonConceptValueIsSkippedNotRead() {
        var usageContext = new UsageContext();
        usageContext.setCode(new Coding(usPHUsageContextType, "focus", null));
        usageContext.setValue(new Quantity().setValue(5));
        var entry = new RelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource(LEAF_CANONICAL);
        entry.addExtension(new Extension(CRMI_INTENDED_USAGE_CONTEXT_EXT_URL, usageContext));

        assertEquals(
                List.of(),
                assertDoesNotThrow(() -> manifestChildFor(entry))
                        .getRelatedArtifacts()
                        .get(0)
                        .getConditions());
    }

    // which conditions count as a change

    /** The key fix: a condition both sides state is not a change. */
    @Test
    void aConditionStatedByBothSidesIsNotReportedAsAChange() {
        var sides = conditionsPerSide(List.of("840539006"), List.of("840539006"));

        assertEquals(List.of("840539006=unchanged"), sides.oldSide());
        assertEquals(List.of("840539006=unchanged"), sides.newSide());
    }

    /** A condition only the new side states is an insert, and only on the new side. */
    @Test
    void aConditionOnlyTheNewSideStatesIsAnInsert() {
        var sides = conditionsPerSide(List.of("840539006"), List.of("840539006", "27836007"));

        assertEquals(List.of("840539006=unchanged"), sides.oldSide());
        assertEquals(List.of("840539006=unchanged", "27836007=insert"), sides.newSide());
    }

    /** A condition only the old side states is a delete, and only on the old side. */
    @Test
    void aConditionOnlyTheOldSideStatesIsADelete() {
        var sides = conditionsPerSide(List.of("840539006", "27836007"), List.of("840539006"));

        assertEquals(List.of("840539006=unchanged", "27836007=delete"), sides.oldSide());
        assertEquals(List.of("840539006=unchanged"), sides.newSide());
    }

    @Test
    void aPriorityStatedTheSameByBothSidesIsNotReportedAsAChange() {
        var sides = prioritiesPerSide("emergent", "emergent");

        assertEquals(List.of("emergent=unchanged"), sides.oldSide());
        assertEquals(List.of("emergent=unchanged"), sides.newSide());
    }

    /**
     * A priority is never absent - it defaults to routine - so a difference is always one value giving
     * way to another, and both sides report it as a replace.
     */
    @Test
    void aPriorityThatDiffersBetweenSidesIsAReplace() {
        var sides = prioritiesPerSide("routine", "emergent");

        assertEquals(List.of("routine=replace"), sides.oldSide());
        assertEquals(List.of("emergent=replace"), sides.newSide());
    }

    /** The default counts as a value, so gaining a stated priority is still a change. */
    @Test
    void gainingAStatedPriorityOverTheDefaultIsAReplace() {
        var sides = prioritiesPerSide(null, "emergent");

        assertEquals(List.of("routine=replace"), sides.oldSide());
        assertEquals(List.of("emergent=replace"), sides.newSide());
    }

    /**
     * A value set present in only one release already carries that news in its own insert or delete.
     * Marking each of its conditions/priorities too would restate the same fact once per item.
     */
    @Test
    void conditionsAndPrioritiesOnAOneSidedValueSetAreNotMarked() {
        var cache = new DiffCache();
        cache.addTarget(LEAF_CANONICAL, leaf(null, "2.81"));

        var changelog = new ChangeLog(MANIFEST_URL);
        changelog.addPage(manifest("3.1.2", null, Collections.emptyList()), manifest("3.2.0", "emergent", List.of("840539006")));
        var page = changelog.addPage(null, grouper("3.2.0"), cache);
        changelog.handleRelatedArtifacts();

        assertNull(page.getOldData());
        assertEquals(List.of("840539006=unchanged"), describeConditions(page.getNewData()));
        assertEquals(List.of("emergent=unchanged"), describePriorities(page.getNewData()));
    }

    /**
     * A diff operation on a condition extension is not routed onto the condition. Whether one
     * actually changed is decided by comparing the two sides when the conditions are added.
     */
    @Test
    void aDiffOperationOnAConditionExtensionIsNotRoutedOntoTheCondition() {
        var entry = relatedArtifact("emergent", "840539006");
        var target = library("3.2.0");
        target.addRelatedArtifact(entry);
        var page = new ChangeLog(MANIFEST_URL).addPage(null, target);

        assertDoesNotThrow(() -> page.addOperation(
                ChangeLog.INSERT,
                "relatedArtifact[0].extension[0]",
                entry.getExtension().get(0),
                null));

        assertNull(page.getNewData()
                .getRelatedArtifacts()
                .get(0)
                .getConditions()
                .get(0)
                .getOperation());
    }

    // ValueSet fixtures

    /** A leaf with the identity every fixture here shares, and nothing else. */
    private static ValueSet emptyLeaf() {
        var valueSet = new ValueSet();
        valueSet.setUrl(LEAF_URL);
        valueSet.setVersion("20240619");
        valueSet.setName("WestNileVirusRNA");
        valueSet.setTitle("West Nile Virus RNA");
        return valueSet;
    }

    /**
     * Builds a leaf ValueSet the way eRSD content actually arrives: compose.include lists the concepts
     * but carries no version, while the expansion records the code system version per code.
     *
     * @param composeVersion version to put on compose.include, or null to leave it off
     * @param expansionVersion version to put on expansion.contains, or null to leave it off
     */
    private static ValueSet leaf(String composeVersion, String expansionVersion) {
        var valueSet = emptyLeaf();
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
        var valueSet = emptyLeaf();
        // display is read from compose.include.concept, because that path claims the code before the
        // expansion is walked - the expansion only supplies the code system version, via the fallback
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

    /** A leaf whose single expansion entry states an active status, or leaves it absent when null. */
    private static ValueSet leafWithInactive(Boolean inactive) {
        var valueSet = leafWithCode("2.81", "West Nile virus RNA");
        if (inactive != null) {
            valueSet.getExpansion().getContainsFirstRep().setInactive(inactive);
        }
        return valueSet;
    }

    /** One code, twice in the expansion - only one of the two entries states a version and a status. */
    private static ValueSet twoContainsEntriesOneSilent(boolean statedOnFirstEntry) {
        var valueSet = emptyLeaf();
        valueSet.getCompose().addInclude().setSystem(LOINC).addConcept().setCode(CODE);
        var first = valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE);
        var second = valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE);
        (statedOnFirstEntry ? first : second).setVersion("2.81").setInactive(true);
        return valueSet;
    }

    /** A grouper whose compose points at the one leaf, as eRSD groupers do. */
    private static ValueSet grouper(String version) {
        var grouper = new ValueSet();
        grouper.setUrl(GROUPER_URL).setVersion(version).setName("dxtc").setTitle("Diagnosis");
        grouper.getCompose().addInclude().addValueSet(LEAF_CANONICAL);
        return grouper;
    }

    // Library fixtures

    private static Library library(String version) {
        var library = new Library();
        library.setUrl(MANIFEST_URL).setVersion(version).setName("m");
        return library;
    }

    /** A manifest stating the given conditions against the one leaf. */
    private static Library manifest(String version, String priorityCode, List<String> conditions) {
        var library = library(version);
        library.addRelatedArtifact(relatedArtifact(priorityCode, conditions.toArray(new String[0])));
        return library;
    }

    /**
     * A manifest relatedArtifact per CRMI spec: conditions and priority are crmi-intendedUsageContext
     * usage contexts told apart by a `focus` or `priority` code.
     */
    private static RelatedArtifact relatedArtifact(String priorityCode, String... conditions) {
        var entry = new RelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource(LEAF_CANONICAL);
        for (var condition : conditions) {
            entry.addExtension(intendedUsageContext("focus", new Coding(SNOMED, condition, "Condition " + condition)));
        }
        if (priorityCode != null) {
            entry.addExtension(intendedUsageContext("priority", new Coding(usPHUsageContext, priorityCode, null)));
        }
        return entry;
    }

    private static Extension intendedUsageContext(String code, Coding value) {
        var usageContext = new UsageContext();
        usageContext.setCode(new Coding(usPHUsageContextType, code, null));
        usageContext.setValue(new CodeableConcept(value));
        return new Extension(CRMI_INTENDED_USAGE_CONTEXT_EXT_URL, usageContext);
    }

    // running the changelog

    private static Page<ValueSetChild> pageFor(ValueSet source, ValueSet target) {
        return new ChangeLog(LEAF_URL).addPage(source, target, (DiffCache) null);
    }

    /** The single code a one-code fixture must produce. */
    private static ValueSetChild.Code onlyCodeOf(ValueSet valueSet) {
        var newData = pageFor(valueSet, valueSet.copy()).getNewData();
        assertNotNull(newData);
        assertEquals(1, newData.getCodes().size());
        return newData.getCodes().get(0);
    }

    /** Where a fixture deliberately produces more than one entry for the same code value. */
    private static ValueSetChild.Code firstCodeOf(ValueSet valueSet) {
        return pageFor(valueSet, valueSet.copy()).getNewData().getCodes().get(0);
    }

    private static LibraryChild manifestChildFor(RelatedArtifact entry) {
        var target = library("3.2.0");
        target.addRelatedArtifact(entry);
        return new ChangeLog(MANIFEST_URL).addPage(null, target).getNewData();
    }

    private static String priorityOf(RelatedArtifact entry) {
        return manifestChildFor(entry)
                .getRelatedArtifacts()
                .get(0)
                .getPriority()
                .getValue()
                .getCodingFirstRep()
                .getCode();
    }

    private record Sides(List<String> oldSide, List<String> newSide) {}

    /** A whole changelog over one grouper and one leaf, with each side's manifest supplied. */
    private static Page<ValueSetChild> runChangelog(Library oldManifest, Library newManifest) {
        var cache = new DiffCache();
        cache.addSource(LEAF_CANONICAL, leaf(null, "2.81"));
        cache.addTarget(LEAF_CANONICAL, leaf(null, "2.81"));

        var changelog = new ChangeLog(MANIFEST_URL);
        changelog.addPage(oldManifest, newManifest);
        var page = changelog.addPage(grouper("3.1.2"), grouper("3.2.0"), cache);
        changelog.handleRelatedArtifacts();
        return page;
    }

    private static Sides conditionsPerSide(List<String> oldConditions, List<String> newConditions) {
        var page = runChangelog(manifest("3.1.2", null, oldConditions), manifest("3.2.0", null, newConditions));
        return new Sides(describeConditions(page.getOldData()), describeConditions(page.getNewData()));
    }

    private static Sides prioritiesPerSide(String oldPriority, String newPriority) {
        var page = runChangelog(
                manifest("3.1.2", oldPriority, List.of("840539006")),
                manifest("3.2.0", newPriority, List.of("840539006")));
        return new Sides(describePriorities(page.getOldData()), describePriorities(page.getNewData()));
    }

    private static List<String> describePriorities(ValueSetChild data) {
        return data.getLeafValueSets().stream()
                .map(leaf -> leaf.getPriority().getValue() + "="
                        + (leaf.getPriority().getOperation() == null
                                ? "unchanged"
                                : leaf.getPriority().getOperation().getType()))
                .toList();
    }

    private static List<String> describeConditions(ValueSetChild data) {
        return data.getLeafValueSets().stream()
                .flatMap(leaf -> leaf.getConditions().stream())
                .map(condition -> condition.getCodeValue() + "="
                        + (condition.getOperation() == null
                                ? "unchanged"
                                : condition.getOperation().getType()))
                .toList();
    }

    /**
     * expansion.contains.system is optional, so an entry can carry a version with no system.
     * Matching on system prevents the expansion version from being incorrectly assigned.
     */
    @Test
    void codeDoesNotTakeVersionFromAnExpansionEntryWithNoSystem() {
        var valueSet = new ValueSet();
        valueSet.setUrl(LEAF_URL);
        valueSet.setVersion("20240619");
        valueSet.setName("WestNileVirusRNA");
        valueSet.setTitle("West Nile Virus RNA");
        valueSet.getCompose().addInclude().setSystem(LOINC).addConcept().setCode(CODE);
        valueSet.getExpansion().addContains().setCode(CODE).setVersion("2.81");

        assertNull(onlyCodeOf(valueSet).getVersion());
    }

    /**
     * A code string is only unique within its code system. Here the expansion lists the same code under
     * ICD-10-CM before LOINC, while compose.include only draws it from LOINC - so a lookup keyed on the
     * code alone would hand the LOINC concept ICD-10-CM's version.
     */
    @Test
    void codeTakesTheVersionOfItsOwnCodeSystem() {
        var valueSet = new ValueSet();
        valueSet.setUrl(LEAF_URL);
        valueSet.setVersion("20240619");
        valueSet.setName("WestNileVirusRNA");
        valueSet.setTitle("West Nile Virus RNA");
        valueSet.getCompose().addInclude().setSystem(LOINC).addConcept().setCode(CODE);
        valueSet.getExpansion()
                .addContains()
                .setSystem("http://hl7.org/fhir/sid/icd-10-cm")
                .setCode(CODE)
                .setVersion("2026");
        valueSet.getExpansion().addContains().setSystem(LOINC).setCode(CODE).setVersion("2.81");
        
        var page = new ChangeLog(LEAF_URL).addPage(valueSet, valueSet.copy(), (ArtifactDiffProcessor.DiffCache) null);
        var distinctVersions = page.getNewData().getCodes().stream()
                .map(ValueSetChild.Code::getVersion)
                .distinct()
                .toList();

        assertEquals(List.of("2.81"), distinctVersions);
    }
}
