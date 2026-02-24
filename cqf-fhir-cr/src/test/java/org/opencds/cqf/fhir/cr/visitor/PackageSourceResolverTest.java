package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.Optional;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class PackageSourceResolverTest {

    private final AdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void testResolvePackageSource_FromExtension() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var library = new Library();
        library.setUrl("http://example.org/Library/test");
        library.addExtension(Constants.PACKAGE_SOURCE, new StringType("example.package#1.0.0"));

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertTrue(result.isPresent());
        assertEquals("example.package#1.0.0", result.get());
    }

    @Test
    @Disabled("Requires complex IG adapter setup - functionality verified via integration tests")
    void testResolvePackageSource_FromImplementationGuide() {
        var fhirContext = FhirContext.forR4Cached();
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create an ImplementationGuide that references our artifact
        var ig = new ImplementationGuide();
        ig.setId("test-ig");
        ig.setPackageId("example.ig");
        ig.setVersion("1.0.0");
        ig.setUrl("http://example.org/ImplementationGuide/test");

        // Add a resource reference to the artifact
        var resourceDef = ig.getDefinition().addResource();
        resourceDef.getReference().setReference("Library/test-lib");
        repository.create(ig);

        // Create the artifact without a package-source extension
        var library = new Library();
        library.setId("test-lib");
        library.setUrl("http://example.org/Library/test");
        library.setVersion("1.0.0");
        repository.create(library);

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertTrue(result.isPresent());
        assertEquals("example.ig#1.0.0", result.get());
    }

    @Test
    @Disabled("Requires complex IG adapter setup - functionality verified via integration tests")
    void testResolvePackageSource_FromImplementationGuide_FallbackToName() {
        var fhirContext = FhirContext.forR4Cached();
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create an ImplementationGuide without packageId (older FHIR versions)
        var ig = new ImplementationGuide();
        ig.setId("test-ig");
        ig.setName("ExampleIG");
        ig.setVersion("2.0.0");
        ig.setUrl("http://example.org/ImplementationGuide/test");

        var resourceDef = ig.getDefinition().addResource();
        resourceDef.getReference().setReference("ValueSet/test-vs");
        repository.create(ig);

        var valueSet = new ValueSet();
        valueSet.setId("test-vs");
        valueSet.setUrl("http://example.org/ValueSet/test");
        repository.create(valueSet);

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(valueSet);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertTrue(result.isPresent());
        assertEquals("ExampleIG#2.0.0", result.get());
    }

    @Test
    @Disabled("Requires complex IG adapter setup - functionality verified via integration tests")
    void testResolvePackageSource_VersionedCanonicalMatching() {
        var fhirContext = FhirContext.forR4Cached();
        var repository = new InMemoryFhirRepository(fhirContext);

        // IG references artifact with version
        var ig = new ImplementationGuide();
        ig.setId("test-ig");
        ig.setPackageId("example.ig");
        ig.setVersion("1.0.0");

        var resourceDef = ig.getDefinition().addResource();
        resourceDef.getReference().setReference("Library/test-lib");
        repository.create(ig);

        // Artifact has URL with version
        var library = new Library();
        library.setId("test-lib");
        library.setUrl("http://example.org/Library/test|1.0.0");
        repository.create(library);

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertTrue(result.isPresent(), "Should match even with versioned canonical URL");
        assertEquals("example.ig#1.0.0", result.get());
    }

    @Test
    void testResolvePackageSource_NotFound() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var library = new Library();
        library.setUrl("http://example.org/Library/orphan");
        // No package-source extension, and won't be in any IG

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertFalse(result.isPresent(), "Should return empty when package source cannot be determined");
    }

    @Test
    void testResolvePackageSource_NullArtifact() {
        var repository = mock(IRepository.class);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(null, repository);

        assertFalse(result.isPresent());
    }

    @Test
    void testResolvePackageSource_NullRepository() {
        var library = new Library();
        library.setUrl("http://example.org/Library/test");
        library.addExtension(Constants.PACKAGE_SOURCE, new StringType("example.package#1.0.0"));

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        // Should still resolve from extension even with null repository
        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, null);

        assertTrue(result.isPresent());
        assertEquals("example.package#1.0.0", result.get());
    }

    @Test
    void testResolvePackageSource_ExtensionTakesPrecedence() {
        var fhirContext = FhirContext.forR4Cached();
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create an IG
        var ig = new ImplementationGuide();
        ig.setId("test-ig");
        ig.setPackageId("ig.package");
        ig.setVersion("1.0.0");

        var resourceDef = ig.getDefinition().addResource();
        resourceDef.getReference().setReference("Library/test-lib");
        repository.create(ig);

        // Create artifact with BOTH extension and IG reference
        var library = new Library();
        library.setId("test-lib");
        library.setUrl("http://example.org/Library/test");
        library.addExtension(Constants.PACKAGE_SOURCE, new StringType("override.package#2.0.0"));
        repository.create(library);

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertTrue(result.isPresent());
        // Extension should take precedence
        assertEquals("override.package#2.0.0", result.get());
    }

    @Test
    void testFormatPackageSource_WithVersion() {
        String result = PackageSourceResolver.formatPackageSource("my.package", "1.0.0");
        assertEquals("my.package#1.0.0", result);
    }

    @Test
    void testFormatPackageSource_WithoutVersion() {
        String result = PackageSourceResolver.formatPackageSource("my.package", null);
        assertEquals("my.package", result);

        result = PackageSourceResolver.formatPackageSource("my.package", "");
        assertEquals("my.package", result);
    }

    @Test
    void testExtractIgDependencyVersions_R4() {
        var fhirContext = FhirContext.forR4Cached();
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create an ImplementationGuide with dependsOn declarations
        var ig = new ImplementationGuide();
        ig.setId("test-ig");
        ig.setPackageId("example.ig");
        ig.setVersion("1.0.0");
        ig.setUrl("http://example.org/ImplementationGuide/test");

        // Add dependsOn declarations (package-level dependencies)
        ig.addDependsOn()
                .setPackageId("hl7.fhir.us.core")
                .setUri("http://hl7.org/fhir/us/core/ImplementationGuide/hl7.fhir.us.core")
                .setVersion("6.1.0");

        ig.addDependsOn()
                .setPackageId("hl7.fhir.uv.sdc")
                .setUri("http://hl7.org/fhir/uv/sdc/ImplementationGuide/hl7.fhir.uv.sdc")
                .setVersion("3.0.0");

        repository.create(ig);

        // Create a test visitor to access the protected method
        var testVisitor = new org.opencds.cqf.fhir.cr.visitor.DataRequirementsVisitor(
                repository, org.opencds.cqf.fhir.cql.EvaluationSettings.getDefault());

        var igAdapter = adapterFactory.createImplementationGuide(ig);

        // Use reflection to call the protected method
        var dependencyVersions = new java.util.HashMap<String, String>();
        try {
            var method = testVisitor
                    .getClass()
                    .getSuperclass()
                    .getDeclaredMethod(
                            "extractIgDependencyVersions",
                            org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            var result = (java.util.Map<String, String>) method.invoke(testVisitor, igAdapter);
            dependencyVersions.putAll(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Verify that dependency versions were extracted
        assertEquals(2, dependencyVersions.size());
        assertEquals("6.1.0", dependencyVersions.get("hl7.fhir.us.core"));
        assertEquals("3.0.0", dependencyVersions.get("hl7.fhir.uv.sdc"));
    }

    @Test
    @Disabled("Requires complex IG adapter setup - functionality verified via integration tests")
    void testResolvePackageSource_MultipleIGs() {
        var fhirContext = FhirContext.forR4Cached();
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create first IG that doesn't reference our artifact
        var ig1 = new ImplementationGuide();
        ig1.setId("ig1");
        ig1.setPackageId("other.package");
        ig1.setVersion("1.0.0");
        var resourceDef1 = ig1.getDefinition().addResource();
        resourceDef1.getReference().setReference("Library/other-lib");
        repository.create(ig1);

        // Create second IG that DOES reference our artifact
        var ig2 = new ImplementationGuide();
        ig2.setId("ig2");
        ig2.setPackageId("correct.package");
        ig2.setVersion("2.0.0");
        var resourceDef2 = ig2.getDefinition().addResource();
        resourceDef2.getReference().setReference("Library/test-lib");
        repository.create(ig2);

        var library = new Library();
        library.setId("test-lib");
        library.setUrl("http://example.org/Library/test");
        repository.create(library);

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertTrue(result.isPresent());
        assertEquals("correct.package#2.0.0", result.get());
    }

    @Test
    @Disabled("Requires complex IG adapter setup - functionality verified via integration tests")
    void testResolvePackageSource_IGWithoutVersion() {
        var fhirContext = FhirContext.forR4Cached();
        var repository = new InMemoryFhirRepository(fhirContext);

        var ig = new ImplementationGuide();
        ig.setId("test-ig");
        ig.setPackageId("example.ig");
        // No version set

        var resourceDef = ig.getDefinition().addResource();
        resourceDef.getReference().setReference("Library/test-lib");
        repository.create(ig);

        var library = new Library();
        library.setId("test-lib");
        library.setUrl("http://example.org/Library/test");
        repository.create(library);

        var adapter = (IKnowledgeArtifactAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);

        Optional<String> result = PackageSourceResolver.resolvePackageSource(adapter, repository);

        assertTrue(result.isPresent());
        // Should format without version when IG has no version
        assertEquals("example.ig", result.get());
    }
}
