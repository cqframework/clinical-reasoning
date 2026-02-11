package org.opencds.cqf.fhir.cr.ecr.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.cr.ecr.r4.R4ImportBundleProducer.isRootSpecificationLibrary;
import static org.opencds.cqf.fhir.cr.ecr.r4.R4ImportBundleProducer.transformImportBundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.opencds.cqf.fhir.cr.crmi.TransformProperties;
import org.opencds.cqf.fhir.cr.ecr.FhirResourceExistsException;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class R4ImportBundleProducerTest {
    private IRepository repository;
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final IParser jsonParser = fhirContext.newJsonParser();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Initializes mocks
        repository = new InMemoryFhirRepository(fhirContext);
    }

    @Test
    void testEnsureHttpsConvertsHttpToHttps() throws Exception {
        String input = "http://example.com/path";
        String output = R4ImportBundleProducer.ensureHttps(input);
        assertTrue(output.startsWith("https://"));
    }

    @Test
    void testEnsureHttpsPreservesHttps() throws Exception {
        String input = "https://secure.com";
        String output = R4ImportBundleProducer.ensureHttps(input);
        assertEquals(input, output);
    }

    @Test
    void testEnsureHttpsMalformedUrl() {
        assertThrows(MalformedURLException.class, () -> R4ImportBundleProducer.ensureHttps("://bad-url"));
    }

    @Test
    void testFixIdentifiersAddsUrnOidPrefix() {
        var id1 = new org.hl7.fhir.r4.model.Identifier();
        id1.setSystem("urn:ietf:rfc:3986");
        id1.setValue("12345");
        var id2 = new org.hl7.fhir.r4.model.Identifier();
        id2.setSystem("urn:ietf:rfc:3986");
        id2.setValue("http://already.ok");

        var fixed = R4ImportBundleProducer.fixIdentifiers(List.of(id1, id2));

        assertTrue(fixed.get(0).getValue().startsWith("urn:oid:"));
        assertEquals("http://already.ok", fixed.get(1).getValue());
    }

    @Test
    void testRemoveProfileFromListRemovesMatchingValue() {
        var profiles = List.of(new CanonicalType("keep"), new CanonicalType("remove"));
        var result = R4ImportBundleProducer.removeProfileFromList(profiles, "remove");
        assertEquals(1, result.size());
        assertEquals("keep", result.get(0).getValue());
    }

    @Test
    void testRemoveProfileFromListHandlesNull() {
        var result = R4ImportBundleProducer.removeProfileFromList(null, "remove");
        assertTrue(result.isEmpty());
    }

    /**
     * @throws FhirResourceExistsException
     */
    @Test
    void testRootLibraryImport() throws FhirResourceExistsException {
        Bundle v2Bundle = (Bundle) jsonParser.parseResource(
                R4ImportBundleProducerTest.class.getResourceAsStream("ersd-bundle-example.json"));
        String targetedValueSetUrl = "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1506";
        String targetedPinnedValueSetVersion = "1.0.0";

        // Extract Root Library
        Library rootLibrary = extractRootLibrary(v2Bundle.getEntry());

        // Assert state before pre-import conformance
        assertFalse(rootLibrary.getRelatedArtifact().stream()
                .anyMatch(i -> i.getResource().equals(targetedValueSetUrl + "|" + targetedPinnedValueSetVersion)));

        // Extract targeted ValueSet to check for import conformance
        Optional<BundleEntryComponent> preImportBundleEntry = v2Bundle.getEntry().stream()
                .filter(i -> i.getFullUrl().equals(targetedValueSetUrl))
                .findFirst();

        ValueSet preImportValueSet = (ValueSet) preImportBundleEntry.get().getResource();

        assertFalse(preImportValueSet
                .getMeta()
                .getProfile()
                .containsAll(Arrays.asList(
                        new CanonicalType(TransformProperties.leafValueSetVsmHostedProfile),
                        new CanonicalType(TransformProperties.leafValueSetConditionProfile))));
        assertTrue(preImportValueSet.getUseContext().stream()
                .anyMatch(i -> i.getCode().getCode().equals("focus")
                        || i.getCode().getCode().equals("priority")));
        assertNotNull(preImportValueSet);

        List<BundleEntryComponent> transactionBundleEntry =
                transformImportBundle(v2Bundle.copy(), repository, "http://localhost:8080/fhir");

        Library updatedRootLibrary = extractRootLibrary(transactionBundleEntry);

        List<RelatedArtifact> ra = updatedRootLibrary.getRelatedArtifact().stream()
                .filter(i -> i.getResource().equals(targetedValueSetUrl + "|" + targetedPinnedValueSetVersion))
                .collect(Collectors.toList());
        assertTrue(!ra.isEmpty());

        List<RelatedArtifact> pds = updatedRootLibrary.getRelatedArtifact().stream()
                .filter(i -> i.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
                .filter(i -> i.getResourceElement()
                        .asStringValue()
                        .equals("http://hl7.org/fhir/us/ecr/PlanDefinition/plandefinition-ersd-instance-example|0.1"))
                .collect(Collectors.toList());
        assertEquals(1, pds.size());
        assertTrue(pds.get(0).hasExtension("http://hl7.org/fhir/StructureDefinition/artifact-isOwned"));

        // Updated assertions for UsageContext
        UsageContext conditionUsageContext =
                (UsageContext) ra.get(0).getExtension().get(0).getValue();
        assertEquals("focus", conditionUsageContext.getCode().getCode());
        assertEquals(
                "Infection caused by Acanthamoeba (disorder)",
                conditionUsageContext.getValueCodeableConcept().getText());

        UsageContext priorityUsageContext =
                (UsageContext) ra.get(0).getExtension().get(1).getValue();
        assertEquals("priority", priorityUsageContext.getCode().getCode());
        assertEquals(
                "routine",
                priorityUsageContext
                        .getValueCodeableConcept()
                        .getCodingFirstRep()
                        .getCode());

        // Extract targeted ValueSet to check for post-import conformance
        Optional<Bundle.BundleEntryComponent> postImportBundleEntry = transactionBundleEntry.stream()
                .filter(i -> i.getFullUrl().equals(targetedValueSetUrl))
                .findFirst();

        ValueSet postImportVs = (ValueSet) postImportBundleEntry.get().getResource();

        List<String> profileStrings = postImportVs.getMeta().getProfile().stream()
                .map(PrimitiveType::getValueAsString)
                .collect(Collectors.toList());
        assertTrue(profileStrings.containsAll(Arrays.asList(
                TransformProperties.leafValueSetVsmHostedProfile, TransformProperties.leafValueSetConditionProfile)));
        assertFalse(postImportVs.getUseContext().stream()
                .anyMatch(i -> i.getCode().getCode().equals("focus")
                        || i.getCode().getCode().equals("priority")));
        assertNotNull(postImportVs);
    }

    @Test
    void testImportOperation() {
        Bundle v2Bundle = (Bundle) jsonParser.parseResource(
                R4ImportBundleProducerTest.class.getResourceAsStream("ersd-bundle-example.json"));
        var updatedBundleEntries = transformImportBundle(v2Bundle.copy(), repository, "www.test.com");

        List<ValueSet> exportedGroupers = v2Bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof MetadataResource
                        && R4ImportBundleProducer.isGrouper((MetadataResource) entry.getResource()))
                .map(entry -> (ValueSet) entry.getResource())
                .collect(Collectors.toList());

        var importedGroupers = updatedBundleEntries.stream()
                .filter(entry -> entry.getResource() instanceof MetadataResource
                        && R4ImportBundleProducer.isGrouper((MetadataResource) entry.getResource()))
                .map(entry -> (ValueSet) entry.getResource())
                .collect(Collectors.toList());

        var groupersWithGroupTypeFromExportedBundle = exportedGroupers.stream()
                .filter(vs -> !R4ImportBundleProducer.isModelGrouperUseContextMissing(vs))
                .collect(Collectors.toList());

        var transformedGroupersWithGroupType = importedGroupers.stream()
                .filter(vs -> !R4ImportBundleProducer.isModelGrouperUseContextMissing(vs))
                .collect(Collectors.toList());

        importedGroupers.forEach(grouper -> {
            assertFalse(grouper.hasExpansion());
        });

        // Check there are 6 groupers to be imported and none of them have group type  as use context
        assertEquals(6, exportedGroupers.size());
        assertEquals(0, groupersWithGroupTypeFromExportedBundle.size());

        // After the import, check all of them have the group type as use context
        assertEquals(6, transformedGroupersWithGroupType.size());

        // Check that none of the valuesets have a v1 profile
        var valueSetHasV1 = updatedBundleEntries.stream()
                .filter(e -> e.getResource().getResourceType() == ResourceType.ValueSet)
                .map(e -> (ValueSet) e.getResource())
                .anyMatch(vs -> vs.getMeta().getProfile().stream()
                        .anyMatch(p -> p.getValue().equals(TransformProperties.ersdVSProfile)));
        assertFalse(valueSetHasV1);
        var valueSetLibraryId = "library-rctc-example";
        var valueSetLibrary = getResourceFromEntriesById(updatedBundleEntries, valueSetLibraryId)
                .map(r -> (Library) r);
        assertTrue(valueSetLibrary.isPresent());
        var valueSetLibraryHasV1 = valueSetLibrary.get().getMeta().getProfile().stream()
                .anyMatch(p -> p.getValue().equals(TransformProperties.ersdVSLibProfile));
        assertFalse(valueSetLibraryHasV1);
        valueSetLibrary.get().getIdentifier().forEach(i -> {
            if (i.getSystem().equals("urn:ietf:rfc:3986")
                    && i.hasValue()
                    && !i.getValue().startsWith("http")
                    && !i.getValue().startsWith("urn:oid")
                    && !i.getValue().startsWith("urn:uuid")
                    && Character.isDigit(i.getValue().charAt(0))) {
                fail("Invalid identifier present, should have been fixed by import");
            }
        });
        var planDefinition = getResourceFromEntriesById(updatedBundleEntries, "plandefinition-ersd-instance-example")
                .map(r -> (PlanDefinition) r);
        assertTrue(planDefinition.isPresent());
        var valueSetLibraryReferenceInPlanDef = planDefinition.get().getRelatedArtifact().stream()
                .filter(ra -> ra.getResource().contains(valueSetLibraryId))
                .findFirst()
                .map(ra -> ra.getResource());
        assertTrue(valueSetLibraryReferenceInPlanDef.isPresent());
        assertEquals(
                valueSetLibrary.get().getVersion(), Canonicals.getVersion(valueSetLibraryReferenceInPlanDef.get()));
    }

    @Test
    void testExtractPrioritiesAndConditionsPopulatesLists() {
        // focus usage context goes to conditions
        UsageContext context1 = new UsageContext();
        context1.setCode(new Coding().setCode("focus"));
        context1.setValue(new CodeableConcept().setText("Condition1").addCoding(new Coding().setCode("condition1")));

        // priority usage context goes to priorities
        UsageContext context2 = new UsageContext();
        context2.setCode(new Coding().setCode("priority"));
        context2.setValue(new CodeableConcept().addCoding(new Coding().setCode("routine")));

        List<UsageContext> priorities = new ArrayList<>();
        List<UsageContext> conditions = new ArrayList<>();

        // Execute
        R4ImportBundleProducer.extractPrioritiesAndConditions(
                Arrays.asList(context1, context2), priorities, conditions, "fakeUrl");

        // Verify
        assertEquals(1, priorities.size());
        assertEquals(
                "routine",
                priorities.get(0).getValueCodeableConcept().getCodingFirstRep().getCode());
        assertEquals(1, conditions.size());
        assertEquals("Condition1", conditions.get(0).getValueCodeableConcept().getText());
    }

    @Test
    void testExtractPrioritiesAndConditionsConflictingPrioritiesThrows() {
        UsageContext context1 = new UsageContext();
        context1.setCode(new Coding().setCode("priority"));
        context1.setValue(new CodeableConcept().addCoding(new Coding().setCode("urgent")));

        UsageContext context2 = new UsageContext();
        context2.setCode(new Coding().setCode("priority"));
        context2.setValue(new CodeableConcept().addCoding(new Coding().setCode("routine")));

        List<UsageContext> priorities = new ArrayList<>();
        List<UsageContext> conditions = new ArrayList<>();

        assertThrows(
                UnprocessableEntityException.class,
                () -> R4ImportBundleProducer.extractPrioritiesAndConditions(
                        Arrays.asList(context1, context2), priorities, conditions, "http://example.com/fhir"));
    }

    @Test
    void testImportOperation_conflicting_priorities() {
        Bundle v2Bundle = (Bundle) jsonParser.parseResource(
                R4ImportBundleProducerTest.class.getResourceAsStream("ersd-bundle-example-conflicting-priority.json"));
        UnprocessableEntityException expectingPriorityConflict = null;

        try {
            transformImportBundle(v2Bundle.copy(), repository, "www.test.com");
        } catch (UnprocessableEntityException e) {
            expectingPriorityConflict = e;
        }
        assertNotNull(expectingPriorityConflict);
        assertTrue(expectingPriorityConflict.getMessage().contains("conflicting priorit"));
    }

    @Test
    void testImportOperation_handle_duplicate_priorities() {
        Bundle v2Bundle = (Bundle) jsonParser.parseResource(
                R4ImportBundleProducerTest.class.getResourceAsStream("ersd-bundle-example-2-priority.json"));
        var updatedBundleEntries = transformImportBundle(v2Bundle.copy(), repository, "www.test.com");
        var library = getResourceFromEntriesById(updatedBundleEntries, "SpecificationLibrary")
                .map(r -> (Library) r);
        assertTrue(library.isPresent());

        boolean foundPriorityExtension = false;

        for (final var ra : library.get().getRelatedArtifact()) {
            if (Canonicals.getResourceType(ra.getResource()).equals("ValueSet")) {
                // Get all crmi-intendedUsageContext extensions
                var intendedUsageContextExtensions =
                        ra.getExtensionsByUrl(TransformProperties.CRMI_INTENDED_USAGE_CONTEXT_EXT_URL);

                // Filter those where the valueUsageContext has code = "priority"
                var priorityExtensions = intendedUsageContextExtensions.stream()
                        .filter(ext -> {
                            if (ext.getValue() instanceof UsageContext uc
                                    && uc.hasCode()
                                    && "priority".equalsIgnoreCase(uc.getCode().getCode())) {
                                return true;
                            }
                            return false;
                        })
                        .toList();

                if (!priorityExtensions.isEmpty()) {
                    foundPriorityExtension = true;
                    // Assert there is only one with code = priority
                    assertEquals(
                            1,
                            priorityExtensions.size(),
                            "Expected exactly one crmi-intendedUsageContext extension with code 'priority'");
                }
            }
        }

        assertTrue(foundPriorityExtension, "No crmi-intendedUsageContext extension with code 'priority' was found.");
    }

    @Test
    void testAddAuthoritativeSourceAddsOnlyOnce() {
        var vs = new ValueSet();
        R4ImportBundleProducer.addAuthoritativeSource(vs, "http://auth");
        R4ImportBundleProducer.addAuthoritativeSource(vs, "http://auth");
        long count = vs.getExtension().stream()
                .filter(e -> e.getUrl().equals(TransformProperties.authoritativeSourceExtUrl))
                .count();
        assertEquals(1, count);
    }

    @Test
    void testAddMetaProfileUrlRemovesDuplicates() {
        var meta = new org.hl7.fhir.r4.model.Meta();
        meta.addProfile("keep");
        meta.addProfile("dup");
        var result = R4ImportBundleProducer.addMetaProfileUrl(meta, List.of("dup", "new"));
        assertTrue(result.stream()
                .map(CanonicalType::getValue)
                .collect(Collectors.toSet())
                .contains("new"));
        assertEquals(3, result.size()); // keep, dup, new
    }

    @Test
    void testSingleUsageContext() {
        UsageContext uc = new UsageContext()
                .setCode(new Coding().setSystem("system").setCode("code"))
                .setValue(new CodeableConcept().setText("test"));

        List<Extension> extensions =
                R4ImportBundleProducer.processUsageContextMapForLibrary(List.of(uc), "http://example.org/ext");

        assertEquals(1, extensions.size());
        assertEquals("http://example.org/ext", extensions.get(0).getUrl());
        assertSame(uc, extensions.get(0).getValue());
    }

    @Test
    void testMultipleUsageContexts() {
        UsageContext uc1 = new UsageContext()
                .setCode(new Coding().setSystem("s1").setCode("c1"))
                .setValue(new CodeableConcept().setText("first"));
        UsageContext uc2 = new UsageContext()
                .setCode(new Coding().setSystem("s2").setCode("c2"))
                .setValue(new CodeableConcept().setText("second"));

        List<Extension> extensions =
                R4ImportBundleProducer.processUsageContextMapForLibrary(List.of(uc1, uc2), "http://example.org/ext");

        assertEquals(2, extensions.size());
        assertSame(uc1, extensions.get(0).getValue());
        assertSame(uc2, extensions.get(1).getValue());
    }

    @Test
    void testEmptyUsageContextList() {
        List<Extension> extensions =
                R4ImportBundleProducer.processUsageContextMapForLibrary(List.of(), "http://example.org/ext");

        assertTrue(extensions.isEmpty());
    }

    @Test
    void testDifferentExtensionUrl() {
        UsageContext uc = new UsageContext()
                .setCode(new Coding().setSystem("s").setCode("c"))
                .setValue(new CodeableConcept().setText("text"));

        List<Extension> extensions = R4ImportBundleProducer.processUsageContextMapForLibrary(
                List.of(uc), "http://example.com/different-url");

        assertEquals(1, extensions.size());
        assertEquals("http://example.com/different-url", extensions.get(0).getUrl());
        assertSame(uc, extensions.get(0).getValue());
    }

    @Test
    void testUsageContextWithNullValue() {
        UsageContext uc = new UsageContext().setCode(new Coding().setSystem("s").setCode("c"));
        List<Extension> extensions =
                R4ImportBundleProducer.processUsageContextMapForLibrary(List.of(uc), "http://example.org/ext");

        assertEquals(1, extensions.size());
        assertSame(uc, extensions.get(0).getValue());
        assertNull(((UsageContext) extensions.get(0).getValue()).getValue());
    }

    @Test
    void testImportOperation_appliesGrouperUseContext() {
        Bundle v2Bundle = (Bundle) jsonParser.parseResource(R4ImportBundleProducerTest.class.getResourceAsStream(
                "ersd-bundle-example-missing-grouper-use-context.json"));
        var updatedBundleEntries = transformImportBundle(v2Bundle.copy(), repository, "www.test.com");

        List<ValueSet> importedGroupers = updatedBundleEntries.stream()
                .filter(entry -> entry.getResource() instanceof MetadataResource
                        && R4ImportBundleProducer.isGrouper((MetadataResource) entry.getResource()))
                .map(entry -> (ValueSet) entry.getResource())
                .collect(Collectors.toList());

        // After the import, check all of them have the group type as use context
        assertEquals(6, importedGroupers.size());
    }

    @Test
    void testImportOperationRemoveErsdValueset() {
        Bundle v2Bundle = (Bundle) jsonParser.parseResource(
                R4ImportBundleProducerTest.class.getResourceAsStream("ersd-bundle-example-v1-vs.json"));
        var updatedBundleEntries = transformImportBundle(v2Bundle.copy(), repository, "www.test.com");

        List<ValueSet> exportedGroupers = v2Bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof MetadataResource
                        && R4ImportBundleProducer.isGrouper((MetadataResource) entry.getResource()))
                .map(entry -> (ValueSet) entry.getResource())
                .collect(Collectors.toList());

        var exportedDxtc = exportedGroupers.stream()
                .filter(vs -> vs.getUrl().contains("dxtc"))
                .collect(Collectors.toList())
                .get(0);
        assertEquals(1, (int) exportedDxtc.getMeta().getProfile().stream()
                .filter(p -> p.getValue().equals(TransformProperties.ersdVSProfile))
                .count());

        List<ValueSet> importedGroupers = updatedBundleEntries.stream()
                .filter(entry -> entry.getResource() instanceof MetadataResource
                        && R4ImportBundleProducer.isGrouper((MetadataResource) entry.getResource()))
                .map(entry -> (ValueSet) entry.getResource())
                .collect(Collectors.toList());

        var importedDxtc = importedGroupers.stream()
                .filter(vs -> vs.getUrl().contains("dxtc"))
                .collect(Collectors.toList())
                .get(0);
        assertEquals(0, (int) importedDxtc.getMeta().getProfile().stream()
                .filter(p -> p.getValue().equals(TransformProperties.ersdVSProfile))
                .count());
    }

    private Library extractRootLibrary(List<Bundle.BundleEntryComponent> bundleEntry) {
        Optional<IBaseResource> rootLibraryEntry = bundleEntry.stream()
                .filter(entry -> entry.hasResource() && isRootSpecificationLibrary(entry.getResource()))
                .findFirst()
                .map(Bundle.BundleEntryComponent::getResource);
        assertTrue(rootLibraryEntry.isPresent());
        return (Library) rootLibraryEntry.get();
    }

    private Optional<Resource> getResourceFromEntriesById(List<BundleEntryComponent> bundle, String id) {
        return bundle.stream()
                .map(e -> e.getResource())
                .filter(r -> r.getIdElement().getIdPart().equals(id))
                .findFirst();
    }
}
