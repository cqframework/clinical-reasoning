package org.opencds.cqf.fhir.cr.visitor.r4;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.SearchParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.visitor.WithdrawVisitor;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class WithdrawVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
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
    void library_withdraw_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(WithdrawVisitorTests.class.getResourceAsStream("Bundle-small-draft.json"));
        Bundle tsBundle = repo.transaction(bundle);
        // InMemoryFhirRepository bug - need to get id like this
        String id = tsBundle.getEntry().get(0).getResponse().getLocation();
        String version = "1.2.3-draft";
        Library library = repo.read(Library.class, new IdType(id)).copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor withdrawVisitor = new WithdrawVisitor(repo);
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(withdrawVisitor, params);

        var res = returnedBundle.getEntry();

        assertEquals(4, res.size());
    }

    @Test
    void library_withdraw_with_approval_test() throws Exception {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                WithdrawVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        SearchParameter sp = (SearchParameter) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("SearchParameter-artifactAssessment.json"));
        Bundle tsBundle = repo.transaction(bundle);
        repo.update(sp);
        // Resource is uploaded using POST - need to get id like this
        String id = tsBundle.getEntry().get(0).getResponse().getLocation();
        String version = "1.2.3-draft";
        Library library = repo.read(Library.class, new IdType(id)).copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor withdrawVisitor = new WithdrawVisitor(repo);
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(withdrawVisitor, params);

        var res = returnedBundle.getEntry();

        assertEquals(5, res.size());
    }

    @Test
    void library_withdraw_No_draft_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                WithdrawVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        String version = "1.2.3";
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor withdrawVisitor = new WithdrawVisitor(repo);
        Parameters params = parameters(part("version", version));

        var exception =
                assertThrows(PreconditionFailedException.class, () -> libraryAdapter.accept(withdrawVisitor, params));
        assertTrue(exception.getMessage().contains("Cannot withdraw an artifact that is not in draft status"));
    }
}
