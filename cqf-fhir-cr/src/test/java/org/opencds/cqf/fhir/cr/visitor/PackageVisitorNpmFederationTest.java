package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * Tests the $package NPM-federation setup: locating the governing ImplementationGuide (the artifact itself,
 * or the one referenced by a {@code composed-of} related artifact) and extracting its dependsOn packages.
 */
class PackageVisitorNpmFederationTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final IAdapterFactory factory = IAdapterFactory.forFhirContext(FhirContext.forR4Cached());

    private IKnowledgeArtifactAdapter adapt(MetadataResource r) {
        return (IKnowledgeArtifactAdapter) factory.createKnowledgeArtifactAdapter(r);
    }

    private ImplementationGuide usCoreIg() {
        var ig = new ImplementationGuide();
        ig.setUrl("http://hl7.org/fhir/us/core/ImplementationGuide/hl7.fhir.us.core");
        ig.setVersion("6.1.0");
        ig.setPackageId("hl7.fhir.us.core");
        ig.addDependsOn().setPackageId("hl7.fhir.uv.sdc").setVersion("3.0.0");
        return ig;
    }

    @Test
    void resolveImplementationGuide_findsIgViaComposedOf() {
        var repo = new InMemoryFhirRepository(fhirContext);
        var ig = usCoreIg();
        repo.update(ig);
        var visitor = new PackageVisitor(repo);

        var library = new Library();
        library.setUrl("http://example.org/Library/vs-package");
        library.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource(ig.getUrl());

        var resolved = visitor.resolveImplementationGuide(adapt(library));
        assertTrue(resolved.isPresent(), "IG should be resolved via the composed-of reference");
        assertEquals(ig.getUrl(), resolved.get().getUrl());
    }

    @Test
    void resolveImplementationGuide_returnsSelfWhenArtifactIsIg() {
        var visitor = new PackageVisitor(new InMemoryFhirRepository(fhirContext));
        var ig = usCoreIg();
        var resolved = visitor.resolveImplementationGuide(adapt(ig));
        assertTrue(resolved.isPresent());
        assertEquals(ig.getUrl(), resolved.get().getUrl());
    }

    @Test
    void resolveImplementationGuide_emptyWhenNoComposedOfIg() {
        var visitor = new PackageVisitor(new InMemoryFhirRepository(fhirContext));
        var library = new Library();
        library.setUrl("http://example.org/Library/no-ig");
        assertTrue(
                visitor.resolveImplementationGuide(adapt(library)).isEmpty(),
                "no IG should be determined when there is no composed-of ImplementationGuide reference");
    }

    @Test
    void extractDependsOnPackages_returnsIgOwnPackageAndDependsOn() {
        var visitor = new PackageVisitor(new InMemoryFhirRepository(fhirContext));
        var packages = visitor.extractDependsOnPackages(adapt(usCoreIg()));
        assertEquals(2, packages.size(), "the IG's own package plus its one dependsOn entry");
        assertTrue(packages.stream().anyMatch(p -> "hl7.fhir.us.core".equals(p[0]) && "6.1.0".equals(p[1])));
        assertTrue(packages.stream().anyMatch(p -> "hl7.fhir.uv.sdc".equals(p[0]) && "3.0.0".equals(p[1])));
    }
}
