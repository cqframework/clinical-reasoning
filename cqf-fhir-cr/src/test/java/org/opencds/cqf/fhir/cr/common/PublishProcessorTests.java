package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class PublishProcessorTests {

    private PublishProcessor processor;
    private InMemoryFhirRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryFhirRepository(FhirContext.forR4());
        processor = new PublishProcessor(repository);
    }

    @Test
    void testPublishBundle_success() {
        // Create a valid CRMIPublishableBundle
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Add ImplementationGuide as first entry
        ImplementationGuide ig = new ImplementationGuide();
        ig.setId("test-ig");
        ig.setUrl("http://example.org/ImplementationGuide/test");
        ig.setVersion("1.0.0");
        ig.setName("TestIG");
        ig.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
        ig.setFhirVersion(java.util.Collections.singletonList(new org.hl7.fhir.r4.model.Enumeration<>(
                new org.hl7.fhir.r4.model.Enumerations.FHIRVersionEnumFactory(),
                org.hl7.fhir.r4.model.Enumerations.FHIRVersion._4_0_1)));

        bundle.addEntry()
                .setResource(ig)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("ImplementationGuide/test-ig");

        // Add a Library as second entry
        Library library = new Library();
        library.setId("test-library");
        library.setUrl("http://example.org/Library/test");
        library.setVersion("1.0.0");
        library.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        bundle.addEntry()
                .setResource(library)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("Library/test-library");

        // Execute publish
        Bundle result = (Bundle) processor.publishBundle(bundle);

        // Verify result
        assertNotNull(result);

        // Verify resources were persisted
        ImplementationGuide persistedIg = repository.read(ImplementationGuide.class, ig.getIdElement());
        assertNotNull(persistedIg);
        assertEquals("test-ig", persistedIg.getIdElement().getIdPart());

        Library persistedLibrary = repository.read(Library.class, library.getIdElement());
        assertNotNull(persistedLibrary);
        assertEquals("test-library", persistedLibrary.getIdElement().getIdPart());
    }

    @Test
    void testPublishBundle_nullBundle() {
        UnprocessableEntityException exception =
                assertThrows(UnprocessableEntityException.class, () -> processor.publishBundle(null));

        assertEquals("Bundle is required", exception.getMessage());
    }

    @Test
    void testPublishBundle_invalidBundleType() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION); // Invalid type

        ImplementationGuide ig = new ImplementationGuide();
        ig.setId("test-ig");
        bundle.addEntry().setResource(ig);

        UnprocessableEntityException exception =
                assertThrows(UnprocessableEntityException.class, () -> processor.publishBundle(bundle));

        assertEquals(
                "Bundle type must be 'transaction' per CRMIPublishableBundle profile, found: collection",
                exception.getMessage());
    }

    @Test
    void testPublishBundle_missingBundleType() {
        Bundle bundle = new Bundle();
        // Bundle type not set

        ImplementationGuide ig = new ImplementationGuide();
        ig.setId("test-ig");
        bundle.addEntry().setResource(ig);

        UnprocessableEntityException exception =
                assertThrows(UnprocessableEntityException.class, () -> processor.publishBundle(bundle));

        assertEquals(
                "Bundle type must be 'transaction' per CRMIPublishableBundle profile, found: null",
                exception.getMessage());
    }

    @Test
    void testPublishBundle_emptyBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        // No entries

        UnprocessableEntityException exception =
                assertThrows(UnprocessableEntityException.class, () -> processor.publishBundle(bundle));

        assertEquals(
                "Bundle must contain at least one entry per CRMIPublishableBundle profile", exception.getMessage());
    }

    @Test
    void testPublishBundle_firstEntryNotImplementationGuide() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Add Library as first entry (should be ImplementationGuide)
        Library library = new Library();
        library.setId("test-library");
        bundle.addEntry().setResource(library);

        UnprocessableEntityException exception =
                assertThrows(UnprocessableEntityException.class, () -> processor.publishBundle(bundle));

        assertEquals(
                "First entry in Bundle must be an ImplementationGuide per CRMIPublishableBundle profile, found: Library",
                exception.getMessage());
    }
}
