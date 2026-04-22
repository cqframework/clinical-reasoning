package org.opencds.cqf.fhir.cr.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.visitor.ReleaseManifestVisitor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ReleaseManifestVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final AdapterFactory adapterFactory = new AdapterFactory();
    private IRepository repo;

    @BeforeEach
    void setup() {
        repo = new InMemoryFhirRepository(fhirContext);
    }

    private Library createManifestLibrary() {
        var manifest = new Library();
        manifest.setId("test-manifest");
        manifest.setUrl("http://example.org/Library/test-manifest");
        manifest.setVersion("1.0.0-draft");
        manifest.setName("TestManifest");
        manifest.setStatus(Enumerations.PublicationStatus.DRAFT);
        manifest.setDate(new Date());
        manifest.setApprovalDate(manifest.getDate());

        var typeCC = new CodeableConcept();
        typeCC.addCoding(new Coding("http://terminology.hl7.org/CodeSystem/library-type", "asset-collection", null));
        manifest.setType(typeCC);

        return manifest;
    }

    private Parameters releaseParams(String version) {
        return parameters(part("version", new StringType(version)), part("versionBehavior", new CodeType("force")));
    }

    @Test
    void releaseManifest_updatesMetadata() {
        var manifest = createManifestLibrary();
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        var result = (Bundle) adapter.accept(visitor, releaseParams("1.0.0"));

        assertNotNull(result);
        // The visitor modifies the resource in place before the transaction
        assertEquals("1.0.0", manifest.getVersion());
        assertEquals(Enumerations.PublicationStatus.ACTIVE, manifest.getStatus());
    }

    @Test
    void releaseManifest_preservesDependsOnEntries() {
        var manifest = createManifestLibrary();
        manifest.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/us/core/ValueSet/birthsex|6.1.0");
        manifest.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0");
        manifest.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource("http://example.org/ImplementationGuide/my-ig|1.0.0");
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        adapter.accept(visitor, releaseParams("1.0.0"));

        var dependsOn = manifest.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .toList();
        var composedOf = manifest.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .toList();

        assertEquals(2, dependsOn.size());
        assertEquals(1, composedOf.size());
    }

    @Test
    void releaseManifest_rejectsNonLibrary() {
        var measure = new Measure();
        measure.setId("test-measure");
        measure.setUrl("http://example.org/Measure/test");
        measure.setVersion("1.0.0-draft");
        measure.setStatus(Enumerations.PublicationStatus.DRAFT);
        repo.update(measure);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        var visitor = new ReleaseManifestVisitor(repo);

        assertThrows(UnprocessableEntityException.class, () -> adapter.accept(visitor, releaseParams("1.0.0")));
    }

    @Test
    void releaseManifest_requiresVersion() {
        var manifest = createManifestLibrary();
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        var params = parameters(part("versionBehavior", new CodeType("force")));

        assertThrows(UnprocessableEntityException.class, () -> adapter.accept(visitor, params));
    }

    @Test
    void releaseManifest_requiresVersionBehavior() {
        var manifest = createManifestLibrary();
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        var params = parameters(part("version", new StringType("1.0.0")));

        assertThrows(UnprocessableEntityException.class, () -> adapter.accept(visitor, params));
    }

    @Test
    void releaseManifest_requiresApprovalDate() {
        var manifest = createManifestLibrary();
        manifest.setApprovalDate(null);
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);

        assertThrows(Exception.class, () -> adapter.accept(visitor, releaseParams("1.0.0")));
    }

    @Test
    void releaseManifest_removesSupersededExpansionParams() {
        var manifest = createManifestLibrary();

        // Add contained expansion parameters with both unversioned and versioned entries
        var expansionParams = new Parameters();
        expansionParams.setId("expansion-parameters");
        expansionParams.addParameter().setName("system-version").setValue(new StringType("http://loinc.org"));
        expansionParams.addParameter().setName("system-version").setValue(new StringType("http://snomed.info/sct"));
        expansionParams.addParameter().setName("system-version").setValue(new StringType("http://loinc.org|2.76"));
        expansionParams
                .addParameter()
                .setName("system-version")
                .setValue(new StringType("http://hl7.org/fhir/sid/icd-10-cm|2026"));
        manifest.addContained(expansionParams);
        manifest.addExtension(Constants.CQF_EXPANSION_PARAMETERS, new Reference("#expansion-parameters"));

        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        adapter.accept(visitor, releaseParams("1.0.0"));

        // The visitor modifies the manifest in place
        var released = manifest;
        var releasedParams = (Parameters) released.getContained().stream()
                .filter(r -> r instanceof Parameters)
                .findFirst()
                .orElse(null);
        assertNotNull(releasedParams);

        var systemVersionParams = releasedParams.getParameter().stream()
                .filter(p -> "system-version".equals(p.getName()))
                .toList();

        // "http://loinc.org" should be removed (superseded by "http://loinc.org|2.76")
        // "http://snomed.info/sct" should remain (no versioned counterpart)
        // "http://loinc.org|2.76" should remain
        // "http://hl7.org/fhir/sid/icd-10-cm|2026" should remain
        var values = systemVersionParams.stream()
                .map(p -> p.getValue().primitiveValue())
                .toList();

        assertEquals(3, systemVersionParams.size());
        assertTrue(values.contains("http://snomed.info/sct"));
        assertTrue(values.contains("http://loinc.org|2.76"));
        assertTrue(values.contains("http://hl7.org/fhir/sid/icd-10-cm|2026"));
        assertTrue(!values.contains("http://loinc.org"));
    }

    @Test
    void releaseManifest_latestFromTxServerWithoutEndpoint_throws() {
        var manifest = createManifestLibrary();
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        var params = parameters(
                part("version", new StringType("1.0.0")),
                part("versionBehavior", new CodeType("force")),
                part("latestFromTxServer", new org.hl7.fhir.r4.model.BooleanType(true)));

        assertThrows(UnprocessableEntityException.class, () -> adapter.accept(visitor, params));
    }

    @Test
    void releaseManifest_emptyDependencies_succeeds() {
        var manifest = createManifestLibrary();
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        var result = (Bundle) adapter.accept(visitor, releaseParams("1.0.0"));

        assertNotNull(result);
        // The visitor modifies the manifest in place
        var released = manifest;
        assertEquals("1.0.0", released.getVersion());
        assertTrue(released.getRelatedArtifact().isEmpty());
    }

    @Test
    void releaseManifest_versionBehaviorForce_usesProvidedVersion() {
        var manifest = createManifestLibrary();
        manifest.setVersion("0.1.0-draft");
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        adapter.accept(visitor, releaseParams("2.0.0"));

        // The visitor modifies the manifest in place
        var released = manifest;
        assertEquals("2.0.0", released.getVersion());
    }

    @Test
    void releaseManifest_nonSemverVersion_succeeds() {
        var manifest = createManifestLibrary();
        manifest.setVersion("2025-09-draft");
        repo.update(manifest);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(manifest);
        var visitor = new ReleaseManifestVisitor(repo);
        adapter.accept(visitor, releaseParams("2025-09"));

        // The visitor modifies the manifest in place
        var released = manifest;
        assertEquals("2025-09", released.getVersion());
    }
}
