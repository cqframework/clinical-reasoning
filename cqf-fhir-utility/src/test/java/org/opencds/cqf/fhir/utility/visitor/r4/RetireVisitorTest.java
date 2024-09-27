package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.HashMap;
import java.util.stream.Collectors;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.visitor.IKnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.visitor.RetireVisitor;

public class RetireVisitorTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private Repository spyRepository;
    private final IParser jsonParser = fhirContext.newJsonParser();

    @BeforeEach
    void setup() {
        SearchParameter sp = (SearchParameter) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("SearchParameter-artifactAssessment.json"));
        spyRepository = spy(new InMemoryFhirRepository(fhirContext));
        spyRepository.update(sp);
        doAnswer(new Answer<Bundle>() {
                    @Override
                    public Bundle answer(InvocationOnMock a) throws Throwable {
                        Bundle b = a.getArgument(0);
                        return InMemoryFhirRepository.transactionStub(b, spyRepository);
                    }
                })
                .when(spyRepository)
                .transaction(any());
    }

    @Test
    void library_retire_test() {
        Bundle bundle =
                (Bundle) jsonParser.parseResource(WithdrawVisitorTests.class.getResourceAsStream("Bundle-retire.json"));
        Bundle tsBundle = spyRepository.transaction(bundle);
        // Resource is uploaded using POST - need to get id like this
        String id = tsBundle.getEntry().get(0).getResponse().getLocation();
        String version = "1.1.0-draft";
        Library library = spyRepository.read(Library.class, new IdType(id)).copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor retireVisitor = new RetireVisitor();
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(retireVisitor, spyRepository, params);

        var res = returnedBundle.getEntry();

        assert (res.size() == 9);
        var libraries = spyRepository.search(Bundle.class, Library.class, new HashMap()).getEntry().stream()
                .filter(x -> ((Library) x.getResource()).getStatus().equals(Enumerations.PublicationStatus.RETIRED))
                .collect(Collectors.toList());

        var valueSets = spyRepository.search(Bundle.class, ValueSet.class, new HashMap()).getEntry().stream()
                .filter(x -> ((ValueSet) x.getResource()).getStatus().equals(Enumerations.PublicationStatus.RETIRED))
                .collect(Collectors.toList());

        var planDefinitions =
                spyRepository.search(Bundle.class, PlanDefinition.class, new HashMap()).getEntry().stream()
                        .filter(x -> ((PlanDefinition) x.getResource())
                                .getStatus()
                                .equals(Enumerations.PublicationStatus.RETIRED))
                        .collect(Collectors.toList());

        assert (libraries.size() == 2);
        assert (valueSets.size() == 6);
        assert (planDefinitions.size() == 1);
    }

    @Test
    void library_retire_no_draft_test() {
        try {
            Bundle bundle = (Bundle) jsonParser.parseResource(
                    WithdrawVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
            spyRepository.transaction(bundle);
            String version = "1.01.21";
            Library library = spyRepository
                    .read(Library.class, new IdType("Library/SpecificationLibrary"))
                    .copy();
            LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
            IKnowledgeArtifactVisitor retireVisitor = new RetireVisitor();
            Parameters params = parameters(part("version", version));
            libraryAdapter.accept(retireVisitor, spyRepository, params);

            fail("Trying to withdraw an active Library should throw an Exception");
        } catch (PreconditionFailedException e) {
            assert (e.getMessage().contains("Cannot withdraw an artifact that is not in draft status"));
        }
    }
}
