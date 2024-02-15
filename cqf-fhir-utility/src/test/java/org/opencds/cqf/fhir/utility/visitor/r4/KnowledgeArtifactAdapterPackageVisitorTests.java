package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;

import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatusEnumFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r4.r4KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4LibraryAdapter;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ProxyRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.RestRepository;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class KnowledgeArtifactAdapterPackageVisitorTests {
  private final FhirContext fhirContext = FhirContext.forR4Cached();
  private final IParser jsonParser = fhirContext.newJsonParser();
  private Repository spyRepository;

  @BeforeEach
  public void setup() {
    Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
    spyRepository = spy(new InMemoryFhirRepository(fhirContext, bundle));
    doAnswer(new Answer<Bundle>() {
        @Override
        public Bundle answer(InvocationOnMock a) throws Throwable {
            Bundle b = a.getArgument(0);
            return InMemoryFhirRepository.transactionStub(b, spyRepository);
        }
    }).when(spyRepository).transaction(any());
}
  
  @Test
  void visitLibraryTest() {
    KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
    Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
    r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    Parameters params = new Parameters();

    Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, params);
    assertNotNull(packagedBundle);
    Bundle loadedBundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
    assertTrue(packagedBundle.getEntry().size() == loadedBundle.getEntry().size());
}
}