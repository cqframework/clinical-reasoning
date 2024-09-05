package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.visitor.DraftVisitor;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.visitor.WithdrawVisitor;

class WithdrawVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private Repository spyRepository;
    private final IParser jsonParser = fhirContext.newJsonParser();
    private final String specificationLibReference = "Library/SpecificationLibrary";
    private final List<String> badVersionList = Arrays.asList(
            "11asd1",
            "1.1.3.1.1",
            "1.|1.1.1",
            "1/.1.1.1",
            "-1.-1.2.1",
            "1.-1.2.1",
            "1.1.-2.1",
            "7.1..21",
            "1.2.1.3-draft",
            "1.2.3-draft",
            "3.2",
            "1.",
            "3.ad.2.",
            "1.0.0.1",
            "",
            null);

    @BeforeEach
    void setup() {
        spyRepository = spy(new InMemoryFhirRepository(fhirContext));
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
    void library_withdraw_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(WithdrawVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        spyRepository.transaction(bundle);
        String version = "1.01.21";
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        KnowledgeArtifactVisitor withdrawVisitor = new WithdrawVisitor();
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(withdrawVisitor, spyRepository, params);

        var res = returnedBundle.getEntry();

        assert(!res.isEmpty());
    }
}
