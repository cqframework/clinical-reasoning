package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatusEnumFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r4.r4KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4LibraryAdapter;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class KnowledgeArtifactAdapterDraftVisitorTests {
  private final FhirContext fhirContext = FhirContext.forR4Cached();
  private Repository spyRepository;

  @BeforeEach
  public void setup() {
    IParser jsonParser = fhirContext.newJsonParser();
    Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterDraftVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
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
    r4KnowledgeArtifactVisitor draftVisitor = new KnowledgeArtifactDraftVisitor();
    Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
    r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    String version = "1.0.1.23";
    String draftedVersion = version + "-draft";
    Parameters params = new Parameters();
    params.addParameter("version", version);
    // Root Artifact must have approval date, releaseLabel and releaseDescription for this test
    assertTrue(library.hasApprovalDate());
    assertTrue(library.hasExtension(IBaseKnowledgeArtifactAdapter.releaseDescriptionUrl));
    assertTrue(library.hasExtension(IBaseKnowledgeArtifactAdapter.releaseLabelUrl));
    assertTrue(library.hasApprovalDate());
    Bundle returnedBundle = (Bundle) libraryAdapter.accept(draftVisitor, spyRepository, params);
    verify(spyRepository).transaction(notNull());
    assertNotNull(returnedBundle);
    Optional<BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findAny();
    assertTrue(maybeLib.isPresent());
    Library lib = spyRepository.read(Library.class,new IdType(maybeLib.get().getResponse().getLocation()));
    assertNotNull(lib);
    assertTrue(lib.getStatus() == Enumerations.PublicationStatus.DRAFT);
    assertTrue(lib.getVersion().equals(draftedVersion));
    assertFalse(lib.hasApprovalDate());
    assertFalse(lib.hasExtension(IBaseKnowledgeArtifactAdapter.releaseDescriptionUrl));
    assertFalse(lib.hasExtension(IBaseKnowledgeArtifactAdapter.releaseLabelUrl));
    List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
    assertTrue(!relatedArtifacts.isEmpty());
    MetadataResourceHelper.forEachMetadataResource(returnedBundle.getEntry(), resource -> {
        List<RelatedArtifact> relatedArtifacts2 = new org.opencds.cqf.fhir.utility.adapter.r4.KnowledgeArtifactAdapter(resource).getRelatedArtifact();
        if (relatedArtifacts2 != null && relatedArtifacts2.size() > 0) {
            for (RelatedArtifact relatedArtifact : relatedArtifacts2) {
                if (r4KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(relatedArtifact)) {
                    assertTrue(Canonicals.getVersion(relatedArtifact.getResource()).equals(draftedVersion));
                }
            }
        }
    }, spyRepository);
  }
}