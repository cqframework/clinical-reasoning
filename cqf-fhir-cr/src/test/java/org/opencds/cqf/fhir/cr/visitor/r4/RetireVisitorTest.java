package org.opencds.cqf.fhir.cr.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.visitor.RetireVisitor;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class RetireVisitorTest {

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
    void library_retire_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                WithdrawVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        Bundle tsBundle = repo.transaction(bundle);
        // Resource is uploaded using POST - need to get id like this
        String id = tsBundle.getEntry().get(0).getResponse().getLocation();
        String version = "1.2.3";
        Library library = repo.read(Library.class, new IdType(id)).copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor retireVisitor = new RetireVisitor(repo);
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(retireVisitor, params);

        var res = returnedBundle.getEntry();

        assertEquals(4, res.size());
        var libraries = repo.search(Bundle.class, Library.class, Map.of()).getEntry().stream()
                .filter(x -> ((Library) x.getResource()).getStatus().equals(Enumerations.PublicationStatus.RETIRED))
                .toList();

        var valueSets = repo.search(Bundle.class, ValueSet.class, Map.of()).getEntry().stream()
                .filter(x -> ((ValueSet) x.getResource()).getStatus().equals(Enumerations.PublicationStatus.RETIRED))
                .toList();

        var planDefinitions = repo.search(Bundle.class, PlanDefinition.class, Map.of()).getEntry().stream()
                .filter(x ->
                        ((PlanDefinition) x.getResource()).getStatus().equals(Enumerations.PublicationStatus.RETIRED))
                .toList();

        assertEquals(2, libraries.size());
        assertEquals(1, valueSets.size());
        assertEquals(1, planDefinitions.size());
    }

    @Test
    void library_retire_no_draft_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                WithdrawVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        String version = "1.01.21";
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor retireVisitor = new RetireVisitor(repo);
        Parameters params = parameters(part("version", version));

        var exception =
                assertThrows(PreconditionFailedException.class, () -> libraryAdapter.accept(retireVisitor, params));
        assertTrue(exception.getMessage().contains("Cannot retire an artifact that is not in active status"));
    }
}
