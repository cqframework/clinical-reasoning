package org.opencds.cqf.fhir.cr.visitor.r5;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r5.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.SearchParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.visitor.DeleteVisitor;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class DeleteVisitorTests {

    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private IRepository repo;
    private final IParser jsonParser = fhirContext.newJsonParser();

    @BeforeEach
    void setup() {
        SearchParameter sp = (SearchParameter) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("SearchParameter-artifactAssessment.json"));
        repo = new InMemoryFhirRepository(fhirContext);
        repo.update(sp);
    }

    @Test
    void library_delete_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(DeleteVisitorTests.class.getResourceAsStream("Bundle-small-retired.json"));
        Bundle tsBundle = repo.transaction(bundle);
        // Resource is uploaded using POST - need to get id like this
        String id = tsBundle.getEntry().get(0).getResponse().getLocation();
        String version = "1.2.3";
        Library library = repo.read(Library.class, new IdType(id)).copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        var deleteVisitor = new DeleteVisitor(repo);
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(deleteVisitor, params);

        var libraryEntries = BundleHelper.getEntryResources(returnedBundle).stream()
            .filter(r -> r.fhirType().equals("Library"))
            .toList();
        var planDefEntries = BundleHelper.getEntryResources(returnedBundle).stream()
            .filter(r -> r.fhirType().equals("PlanDefinition"))
            .toList();
        var valueSetEntries = BundleHelper.getEntryResources(returnedBundle).stream()
            .filter(r -> r.fhirType().equals("ValueSet"))
            .toList();
        var basicEntries = BundleHelper.getEntryResources(returnedBundle).stream()
            .filter(r -> r.fhirType().equals("Basic"))
            .toList();


        assertEquals(2, libraryEntries.size());
        assertEquals(1, planDefEntries.size());
        assertEquals(1, valueSetEntries.size());
        assertEquals(1, basicEntries.size());
    }

    @Test
    void library_delete_active_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(DeleteVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        String version = "1.2.3";
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        var deleteVisitor = new DeleteVisitor(repo);
        Parameters params = parameters(part("version", version));

        var exception =
                assertThrows(PreconditionFailedException.class, () -> libraryAdapter.accept(deleteVisitor, params));
        assertTrue(exception.getMessage().contains("Cannot delete an artifact that is not in retired status"));
    }

    @Test
    void library_delete_draft_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                DeleteVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        Bundle tsBundle = repo.transaction(bundle);
        String id = tsBundle.getEntry().get(0).getResponse().getLocation();
        String version = "1.2.3-draft";
        Library library = repo.read(Library.class, new IdType(id)).copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        var deleteVisitor = new DeleteVisitor(repo);
        Parameters params = parameters(part("version", version));

        var exception =
                assertThrows(PreconditionFailedException.class, () -> libraryAdapter.accept(deleteVisitor, params));
        assertTrue(exception.getMessage().contains("Cannot delete an artifact that is not in retired status"));
    }
}
