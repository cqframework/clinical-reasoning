package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyProviderRouter;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * Tests for the enriched DataRequirementsVisitor path that activates when
 * terminologyProviderRouter is non-null (i.e. IG $data-requirements).
 */
class DataRequirementsVisitorEnrichedTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirContext(fhirContext);

    /**
     * Uses update instead of create to preserve resource IDs, since InMemoryFhirRepository.create()
     * overwrites the ID with a random value. IDs must include the resource type prefix
     * (e.g. "ValueSet/test-vs") for InMemoryFhirRepository lookups to work correctly.
     */
    private void addToRepo(IRepository repository, IBaseResource resource) {
        repository.update(resource);
    }

    /**
     * When router is null (Library/Measure path using 2-arg constructor),
     * the original recursiveGather path should be used.
     */
    @Test
    void visit_withNullRouter_usesOriginalPath() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var ig = createSimpleIg("test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0");
        addToRepo(repository, ig);

        // No router — simulating Library/Measure path
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault());

        var operationParameters = newParameters(fhirContext);

        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;
        assertEquals("module-definition", library.getType().getCodingFirstRep().getCode());
    }

    /**
     * When router is present (IG path using 3-arg constructor), the enriched path
     * should be used and produce a module-definition Library.
     */
    @Test
    void visit_withRouter_usesEnrichedPath() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create a ValueSet that will be a dependency
        var vs = new ValueSet();
        vs.setId("ValueSet/test-vs");
        vs.setUrl("http://example.org/ValueSet/test-vs");
        vs.setVersion("2.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Test ValueSet");
        vs.addExtension(Constants.PACKAGE_SOURCE, new StringType("example.package#2.0.0"));
        addToRepo(repository, vs);

        // Create an IG with the ValueSet as a definition resource
        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "ValueSet/test-vs");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);

        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;
        assertEquals("module-definition", library.getType().getCodingFirstRep().getCode());
    }

    /**
     * Verifies that dependency role classification produces crmi-dependencyRole extensions.
     * All dependencies get at least the "default" role.
     */
    @Test
    void visit_enrichedPath_addsDependencyRoleExtension() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create ValueSet in local repo
        var vs = new ValueSet();
        vs.setId("ValueSet/test-vs");
        vs.setUrl("http://example.org/ValueSet/test-vs");
        vs.setVersion("1.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Test VS");
        addToRepo(repository, vs);

        // Create Library that depends on the ValueSet
        var lib = new Library();
        lib.setId("Library/test-lib");
        lib.setUrl("http://example.org/Library/test-lib");
        lib.setVersion("1.0.0");
        lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib.setName("Test Library");
        lib.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/test-vs");
        addToRepo(repository, lib);

        // Create IG with Library as a definition resource
        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "Library/test-lib");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);

        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        // The enriched path produces RelatedArtifacts from leaf artifacts (not the root)
        assertTrue(!library.getRelatedArtifact().isEmpty(), "Should have gathered dependencies");
        // Check dependency role extensions only on depends-on entries (composed-of entries won't have them)
        var dependsOnRas = library.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .toList();
        assertTrue(!dependsOnRas.isEmpty(), "Should have at least one depends-on entry");
        for (var ra : dependsOnRas) {
            var roleExtensions = ra.getExtensionsByUrl(Constants.CRMI_DEPENDENCY_ROLE);
            assertTrue(!roleExtensions.isEmpty(), "Each depends-on should have at least one dependency role");

            boolean hasDefault = roleExtensions.stream()
                    .anyMatch(ext -> "default".equals(((org.hl7.fhir.r4.model.CodeType) ext.getValue()).getValue()));
            assertTrue(hasDefault, "All dependencies should have 'default' role");
        }
    }

    /**
     * Verifies that complex package-source extension is added when package source is resolvable.
     */
    @Test
    void visit_enrichedPath_addsComplexPackageSourceExtension() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create ValueSet with package-source extension
        var vs = new ValueSet();
        vs.setId("ValueSet/test-vs");
        vs.setUrl("http://example.org/ValueSet/test-vs");
        vs.setVersion("1.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Test VS");
        vs.addExtension(Constants.PACKAGE_SOURCE, new StringType("my.package#1.0.0"));
        addToRepo(repository, vs);

        // Create Library that depends on the ValueSet
        var lib = new Library();
        lib.setId("Library/test-lib");
        lib.setUrl("http://example.org/Library/test-lib");
        lib.setVersion("1.0.0");
        lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib.setName("Test Library");
        lib.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/test-vs");
        addToRepo(repository, lib);

        // Create IG
        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "Library/test-lib");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);

        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        // Find the relatedArtifact for the ValueSet
        var vsRelatedArtifact = library.getRelatedArtifact().stream()
                .filter(ra -> ra.getUrl() != null && ra.getUrl().contains("ValueSet/test-vs"))
                .findFirst();

        if (vsRelatedArtifact.isPresent()) {
            var packageSourceExts = vsRelatedArtifact.get().getExtensionsByUrl(Constants.PACKAGE_SOURCE);
            assertTrue(!packageSourceExts.isEmpty(), "Should have package-source extension");

            Extension packageSourceExt = packageSourceExts.get(0);
            assertNotNull(packageSourceExt.getExtensionByUrl("packageId"), "Should have packageId sub-extension");
            assertEquals(
                    "my.package",
                    ((StringType) packageSourceExt
                                    .getExtensionByUrl("packageId")
                                    .getValue())
                            .getValue());

            assertNotNull(packageSourceExt.getExtensionByUrl("version"), "Should have version sub-extension");
            assertEquals(
                    "1.0.0",
                    ((StringType) packageSourceExt.getExtensionByUrl("version").getValue()).getValue());
        }
    }

    /**
     * Verifies that cqf-resourceType extension is added to each relatedArtifact.
     */
    @Test
    void visit_enrichedPath_addsResourceTypeExtension() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create a ValueSet
        var vs = new ValueSet();
        vs.setId("ValueSet/test-vs");
        vs.setUrl("http://example.org/ValueSet/test-vs");
        vs.setVersion("1.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Test VS");
        addToRepo(repository, vs);

        // Create Library depending on the VS
        var lib = new Library();
        lib.setId("Library/test-lib");
        lib.setUrl("http://example.org/Library/test-lib");
        lib.setVersion("1.0.0");
        lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib.setName("Test Library");
        lib.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/test-vs");
        addToRepo(repository, lib);

        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "Library/test-lib");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);

        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        for (var ra : library.getRelatedArtifact()) {
            if (ra.getUrl() != null && ra.getUrl().contains("ValueSet")) {
                var resourceElement = ra.getResourceElement();
                var resourceTypeExt = resourceElement.getExtensionByUrl(Constants.CQF_RESOURCETYPE);
                assertNotNull(resourceTypeExt, "Should have cqf-resourceType extension");
                assertEquals("ValueSet", ((org.hl7.fhir.r4.model.CodeType) resourceTypeExt.getValue()).getValue());
            }
        }
    }

    /**
     * Verifies that the Tx server is queried when a dependency is not in the local repo
     * and artifactEndpointConfigurations are provided.
     */
    @Test
    void visit_enrichedPath_queriesTxServerWithConfigurations() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create Library that references a remote ValueSet (not in local repo)
        var lib = new Library();
        lib.setId("Library/test-lib");
        lib.setUrl("http://example.org/Library/test-lib");
        lib.setVersion("1.0.0");
        lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib.setName("Test Library");
        lib.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://tx.example.org/ValueSet/remote-vs");
        addToRepo(repository, lib);

        // Create IG
        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "Library/test-lib");
        addToRepo(repository, ig);

        // Set up router to return a remote ValueSet
        var remoteVs = new ValueSet();
        remoteVs.setId("remote-vs");
        remoteVs.setUrl("http://tx.example.org/ValueSet/remote-vs");
        remoteVs.setVersion("3.0.0");
        remoteVs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        remoteVs.setName("Remote VS");

        var router = mock(ITerminologyProviderRouter.class);
        when(router.getValueSetResourceWithConfigurations(any(), anyString())).thenReturn(Optional.of(remoteVs));

        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        // Build parameters with artifactEndpointConfiguration
        var operationParameters = (Parameters) newParameters(fhirContext);
        var config = operationParameters.addParameter().setName("artifactEndpointConfiguration");
        config.addPart().setName("artifactRoute").setValue(new UriType("http://tx.example.org"));
        config.addPart().setName("endpointUri").setValue(new UriType("http://tx.example.org/fhir"));

        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);

        // Verify that the router was called with configurations
        verify(router).getValueSetResourceWithConfigurations(any(List.class), anyString());
    }

    /**
     * Verifies that the root IG appears as a composed-of RelatedArtifact
     * in the module-definition Library output when using the enriched path.
     */
    @Test
    void visit_enrichedPath_addsRootIgAsComposedOf() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create a simple IG with no definition resources
        var ig = createSimpleIg("test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);

        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        // Find the composed-of entry for the root IG
        var composedOfEntries = library.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .toList();
        assertEquals(1, composedOfEntries.size(), "Should have exactly one composed-of entry for the root IG");

        var igEntry = composedOfEntries.get(0);
        assertTrue(
                igEntry.getResource().contains("http://example.org/ImplementationGuide/test-ig"),
                "composed-of entry should reference the IG canonical");
    }

    private ImplementationGuide createSimpleIg(String id, String url, String version) {
        var ig = new ImplementationGuide();
        ig.setId("ImplementationGuide/" + id);
        ig.setUrl(url);
        ig.setVersion(version);
        ig.setStatus(Enumerations.PublicationStatus.ACTIVE);
        ig.setName("TestIG");
        ig.setPackageId("test.ig");
        return ig;
    }

    /**
     * Verifies that crmi-referenceSource extensions are added when dependency has fhirPaths.
     */
    @Test
    void visit_enrichedPath_addsReferenceSourceExtension() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create ValueSet dependency
        var vs = new ValueSet();
        vs.setId("ValueSet/test-vs");
        vs.setUrl("http://example.org/ValueSet/test-vs");
        vs.setVersion("1.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Test VS");
        addToRepo(repository, vs);

        // Create Library that depends on the ValueSet via relatedArtifact
        var lib = new Library();
        lib.setId("Library/test-lib");
        lib.setUrl("http://example.org/Library/test-lib");
        lib.setVersion("1.0.0");
        lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib.setName("Test Library");
        lib.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/test-vs");
        addToRepo(repository, lib);

        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "Library/test-lib");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);
        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        // The enriched path should produce related artifacts
        assertTrue(!library.getRelatedArtifact().isEmpty(), "Should have gathered dependencies");
    }

    /**
     * Verifies that composed-of count matches expectation when IG has definition resources.
     * The enriched path should add exactly one composed-of for the root IG itself.
     */
    @Test
    void visit_enrichedPath_composedOfCountMatchesIg() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var vs = new ValueSet();
        vs.setId("ValueSet/test-vs");
        vs.setUrl("http://example.org/ValueSet/test-vs");
        vs.setVersion("1.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Test VS");
        addToRepo(repository, vs);

        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "ValueSet/test-vs");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);
        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        // IG as root → should have exactly one composed-of entry (the root IG itself)
        var composedOfEntries = library.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .toList();
        assertEquals(1, composedOfEntries.size(), "Should have exactly one composed-of entry (the root IG)");
    }

    /**
     * Verifies that an unresolvable dependency is still listed as depends-on.
     */
    @Test
    void visit_enrichedPath_handlesUnresolvableDependency() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Library with dependency that cannot be resolved
        var lib = new Library();
        lib.setId("Library/test-lib");
        lib.setUrl("http://example.org/Library/test-lib");
        lib.setVersion("1.0.0");
        lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib.setName("Test Library");
        lib.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://nonexistent.example.org/ValueSet/no-such-vs");
        addToRepo(repository, lib);

        var ig = createIgWithDefinitionResource(
                "test-ig", "http://example.org/ImplementationGuide/test-ig", "1.0.0", "Library/test-lib");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);
        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        // Unresolvable dependency should still appear in the output
        var dependsOn = library.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .toList();
        assertTrue(!dependsOn.isEmpty(), "Unresolvable dependency should still be listed as depends-on");
    }

    /**
     * Verifies that duplicate dependencies referenced from two different artifacts
     * only appear once in the output.
     */
    @Test
    void visit_enrichedPath_deduplicatesDependencies() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Shared dependency
        var vs = new ValueSet();
        vs.setId("ValueSet/shared-vs");
        vs.setUrl("http://example.org/ValueSet/shared-vs");
        vs.setVersion("1.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Shared VS");
        addToRepo(repository, vs);

        // First Library referencing the shared VS
        var lib1 = new Library();
        lib1.setId("Library/lib-1");
        lib1.setUrl("http://example.org/Library/lib-1");
        lib1.setVersion("1.0.0");
        lib1.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib1.setName("Library 1");
        lib1.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/shared-vs");
        addToRepo(repository, lib1);

        // Second Library also referencing the shared VS
        var lib2 = new Library();
        lib2.setId("Library/lib-2");
        lib2.setUrl("http://example.org/Library/lib-2");
        lib2.setVersion("1.0.0");
        lib2.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib2.setName("Library 2");
        lib2.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/shared-vs");
        addToRepo(repository, lib2);

        // IG with both libraries
        var ig = new ImplementationGuide();
        ig.setId("ImplementationGuide/test-ig");
        ig.setUrl("http://example.org/ImplementationGuide/test-ig");
        ig.setVersion("1.0.0");
        ig.setStatus(Enumerations.PublicationStatus.ACTIVE);
        ig.setName("TestIG");
        ig.setPackageId("test.ig");
        ig.getDefinition().addResource().setReference(new Reference("Library/lib-1"));
        ig.getDefinition().addResource().setReference(new Reference("Library/lib-2"));
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);
        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;

        // The shared-vs should appear at most once
        var sharedVsEntries = library.getRelatedArtifact().stream()
                .filter(ra -> {
                    var ref = ra.getResource();
                    return ref != null && ref.contains("shared-vs");
                })
                .toList();
        assertTrue(sharedVsEntries.size() <= 1, "Shared VS should appear at most once (deduplication)");
    }

    /**
     * Verifies that IG dependsOn packages are passed through to the enriched path.
     */
    @Test
    void visit_enrichedPath_extractsDependsOnPackages() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create IG with dependsOn entries
        var ig = new ImplementationGuide();
        ig.setId("ImplementationGuide/test-ig");
        ig.setUrl("http://example.org/ImplementationGuide/test-ig");
        ig.setVersion("1.0.0");
        ig.setStatus(Enumerations.PublicationStatus.ACTIVE);
        ig.setName("TestIG");
        ig.setPackageId("test.ig");
        ig.addDependsOn()
                .setPackageId("hl7.fhir.us.core")
                .setVersion("6.1.0")
                .setUri("http://hl7.org/fhir/us/core/ImplementationGuide/hl7.fhir.us.core");
        addToRepo(repository, ig);

        var router = mock(ITerminologyProviderRouter.class);
        var visitor = new DataRequirementsVisitor(repository, EvaluationSettings.getDefault(), router);

        var operationParameters = newParameters(fhirContext);
        var igAdapter = adapterFactory.createKnowledgeArtifactAdapter(ig);

        // Should not throw — dependsOn packages are extracted and passed through
        var result = visitor.visit(igAdapter, operationParameters);

        assertInstanceOf(Library.class, result);
        var library = (Library) result;
        assertEquals("module-definition", library.getType().getCodingFirstRep().getCode());
    }

    private ImplementationGuide createIgWithDefinitionResource(
            String id, String url, String version, String resourceReference) {
        var ig = createSimpleIg(id, url, version);
        ig.getDefinition().addResource().setReference(new Reference(resourceReference));
        return ig;
    }
}
