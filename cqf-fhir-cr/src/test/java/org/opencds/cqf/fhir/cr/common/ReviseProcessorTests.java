package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.ClasspathUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ReviseProcessorTests {

    public ReviseProcessor processor;

    @Test
    void testReviseProcessor() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        processor = new ReviseProcessor(repository);

        Bundle draftBundle =
            ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "org/opencds/cqf/fhir/cr/visitor/r4/Bundle-small-draft.json");
        repository.transaction(draftBundle);
        Library existingLibrary = draftBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Library)
            .map(e -> (Library) e.getResource())
            .findFirst()
            .get();

        Library proposedLibrary = existingLibrary.copy();
        proposedLibrary.setVersion("10.0.0-draft");

        processor.reviseResource(proposedLibrary);

        Library revisedLibrary = repository.read(Library.class, existingLibrary.getIdElement());

        Assertions.assertEquals(proposedLibrary.getVersion(), revisedLibrary.getVersion());
    }

    @Test
    void testReviseProcessor_existingResourceNotDraft() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        processor = new ReviseProcessor(repository);

        Bundle activeBundle =
            ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "org/opencds/cqf/fhir/cr/visitor/r4/Bundle-ersd-small-active.json");
        repository.transaction(activeBundle);
        Library existingLibrary = activeBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Library)
            .map(e -> (Library) e.getResource())
            .findFirst()
            .get();

        Library proposedLibrary = existingLibrary.copy();
        proposedLibrary.setVersion("10.0.0");

        Assertions.assertThrows(IllegalStateException.class, () -> processor.reviseResource(proposedLibrary));
    }

    @Test
    void testReviseProcessor_proposedResourceNotDraft() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        processor = new ReviseProcessor(repository);

        Bundle draftBundle =
            ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "org/opencds/cqf/fhir/cr/visitor/r4/Bundle-small-draft.json");
        repository.transaction(draftBundle);
        Library existingLibrary = draftBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Library)
            .map(e -> (Library) e.getResource())
            .findFirst()
            .get();

        Library proposedLibrary = existingLibrary.copy();
        proposedLibrary.setStatus(PublicationStatus.ACTIVE);

        Assertions.assertThrows(IllegalStateException.class, () -> processor.reviseResource(proposedLibrary));
    }

    @Test
    void testReviseProcessor_resourceNotFound() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        processor = new ReviseProcessor(repository);

        Bundle draftBundle =
            ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "org/opencds/cqf/fhir/cr/visitor/r4/Bundle-small-draft.json");
        Library existingLibrary = draftBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Library)
            .map(e -> (Library) e.getResource())
            .findFirst()
            .get();

        Library proposedLibrary = existingLibrary.copy();
        proposedLibrary.setVersion("10.0.0-draft");

        Assertions.assertThrows(ResourceNotFoundException.class, () -> processor.reviseResource(proposedLibrary));
    }

}
