package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.SearchParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.visitor.DeleteVisitor;
import org.opencds.cqf.fhir.utility.visitor.IKnowledgeArtifactVisitor;

public class DeleteVisitorTests {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private Repository repo;
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
        Bundle bundle =
                (Bundle) jsonParser.parseResource(DeleteVisitorTests.class.getResourceAsStream("Bundle-delete.json"));
        Bundle tsBundle = repo.transaction(bundle);
        // Resource is uploaded using POST - need to get id like this
        String id = tsBundle.getEntry().get(0).getResponse().getLocation();
        String version = "1.1.0";
        Library library = repo.read(Library.class, new IdType(id)).copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor deleteVisitor = new DeleteVisitor();
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(deleteVisitor, repo, params);

        var res = returnedBundle.getEntry();

        assert (res.size() == 9);
    }

    @Test
    void library_delete_active_test() {
        try {
            Bundle bundle = (Bundle)
                    jsonParser.parseResource(DeleteVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
            repo.transaction(bundle);
            String version = "1.0.0";
            Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                    .copy();
            LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
            IKnowledgeArtifactVisitor deleteVisitor = new DeleteVisitor();
            Parameters params = parameters(part("version", version));
            libraryAdapter.accept(deleteVisitor, repo, params);

            fail("Trying to withdraw an active Library should throw an Exception");
        } catch (PreconditionFailedException e) {
            assert (e.getMessage().contains("Cannot delete an artifact that is not in retired status"));
        }
    }

    @Test
    void library_delete_draft_test() {
        try {
            Bundle bundle = (Bundle)
                    jsonParser.parseResource(DeleteVisitorTests.class.getResourceAsStream("Bundle-withdraw.json"));
            Bundle tsBundle = repo.transaction(bundle);
            String id = tsBundle.getEntry().get(0).getResponse().getLocation();
            String version = "1.1.0-draft";
            Library library = repo.read(Library.class, new IdType(id)).copy();
            LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
            IKnowledgeArtifactVisitor deleteVisitor = new DeleteVisitor();
            Parameters params = parameters(part("version", version));
            libraryAdapter.accept(deleteVisitor, repo, params);

            fail("Trying to withdraw a draft Library should throw an Exception");
        } catch (PreconditionFailedException e) {
            assert (e.getMessage().contains("Cannot delete an artifact that is not in retired status"));
        }
    }
}
