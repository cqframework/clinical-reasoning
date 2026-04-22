package org.opencds.cqf.fhir.cr.crmi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.Date;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class R4ReleaseManifestServiceTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private InMemoryFhirRepository repo;

    @BeforeEach
    void setup() {
        repo = new InMemoryFhirRepository(fhirContext);
    }

    private Library createManifestLibrary() {
        var manifest = new Library();
        manifest.setId(new IdType("Library", "test-manifest"));
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

    @Test
    void releaseManifest_succeeds() throws FHIRException {
        var manifest = createManifestLibrary();
        repo.update(manifest);

        // Verify the resource is readable
        assertNotNull(repo.read(Library.class, manifest.getIdElement()));

        var service = new R4ReleaseManifestService(repo);
        var result = service.releaseManifest(
                new IdType("Library", "test-manifest"), "1.0.0", new CodeType("force"), null, null, null);

        assertNotNull(result);
    }

    @Test
    void releaseManifest_resourceNotFound_throws() {
        var service = new R4ReleaseManifestService(repo);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.releaseManifest(
                        new IdType("Library/nonexistent"), "1.0.0", new CodeType("force"), null, null, null));
    }

    @Test
    void releaseManifest_nonLibrary_throws() {
        var measure = new Measure();
        measure.setId("test-measure");
        measure.setUrl("http://example.org/Measure/test");
        measure.setVersion("1.0.0-draft");
        measure.setStatus(Enumerations.PublicationStatus.DRAFT);
        repo.update(measure);

        var service = new R4ReleaseManifestService(repo);

        // Reading a Measure via Library ID throws ResourceNotFoundException
        assertThrows(
                Exception.class,
                () -> service.releaseManifest(
                        new IdType("Measure/test-measure"), "1.0.0", new CodeType("force"), null, null, null));
    }

    @Test
    void releaseManifest_withAllParameters() throws FHIRException {
        var manifest = createManifestLibrary();
        repo.update(manifest);

        var service = new R4ReleaseManifestService(repo);
        var result = service.releaseManifest(
                new IdType("Library", "test-manifest"),
                "2.0.0",
                new CodeType("force"),
                null,
                null,
                "release-label-test");

        assertNotNull(result);
    }
}
