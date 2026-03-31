package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.search.Searches;

public class InMemoryRepositoryTest {

    IRepository repository;

    InMemoryRepositoryTest() {
        repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repository.update(new Library().setId("Library/example1"));
        repository.update(new Library().setUrl("http://example.com/123").setId("Library/example2"));
        repository.update(new Encounter().setId("Encounter/example3"));
    }

    @Test
    void read() {
        IBaseResource res = repository.read(Library.class, new IdType("Library/example1"), null);
        assertEquals("example1", res.getIdElement().getIdPart());
    }

    @Test
    void readPlainIdWithRelativeReference() {
        // Issue #945: resource stored with plain ID should be found via relative reference
        repository.update(new Library().setId("test-lib"));
        IBaseResource res = repository.read(Library.class, new IdType("Library/test-lib"), null);
        assertEquals("test-lib", res.getIdElement().getIdPart());
    }

    @Test
    void readRelativeReferenceWithPlainId() {
        // Issue #945: resource stored with relative reference should be found via plain ID
        repository.update(new Library().setId("Library/test-lib2"));
        IBaseResource res = repository.read(Library.class, new IdType("test-lib2"), null);
        assertEquals("test-lib2", res.getIdElement().getIdPart());
    }

    @Test
    void searchWithId() {

        var search = Searches.byId("example1");
        var resources = repository.search(Bundle.class, Library.class, search);
        assertEquals(1, resources.getEntry().size());

        search = Searches.byId("example3");
        resources = repository.search(Bundle.class, Encounter.class, search);
        assertEquals(1, resources.getEntry().size());

        search = Searches.byId("2345");
        resources = repository.search(Bundle.class, Encounter.class, search);
        assertEquals(0, resources.getEntry().size());

        search = Searches.byId("example1", "example2");
        resources = repository.search(Bundle.class, Library.class, search);
        assertEquals(2, resources.getEntry().size());
    }

    @Test
    void searchWithUrl() {
        var resources = repository.search(Bundle.class, Library.class, Searches.byUrl("http://example.com/123"));
        assertEquals(1, resources.getEntry().size());
    }

    @Test
    void deleteWithId() {
        try {
            var outcome = repository.delete(Library.class, new IdType("Library/example1"));

            assertEquals("Library/example1", outcome.getId().getValue());

            repository.read(Library.class, new IdType("Library/example1"));
        } catch (ResourceNotFoundException e) {
            assertEquals("HAPI-0971: Resource Library/example1 is not known", e.getMessage());
        }
    }

    @Test
    void linkNotImplemented() {
        try {
            repository.link(Bundle.class, "Library/example1");
        } catch (NotImplementedOperationException e) {
            assertEquals("Paging is not currently supported", e.getMessage());
        }
    }

    @Test
    void capabilitiesNotImplemented() {
        try {
            repository.capabilities(CapabilityStatement.class);
        } catch (NotImplementedOperationException e) {
            assertEquals("The capabilities interaction is not currently supported", e.getMessage());
        }
    }

    @Test
    void invokeNotImplemented() {
        try {
            repository.invoke("someName", new Parameters());
        } catch (NotImplementedOperationException e) {
            assertEquals("Invoke is not currently supported", e.getMessage());
        }
    }
}
