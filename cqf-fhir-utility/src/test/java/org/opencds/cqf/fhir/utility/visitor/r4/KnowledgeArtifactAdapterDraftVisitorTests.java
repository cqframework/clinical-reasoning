package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;

import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r4.r4KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4LibraryAdapter;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class KnowledgeArtifactAdapterDraftVisitorTests {
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
    "",
    null
);
  @BeforeEach
  public void setup() {
    spyRepository = spy(new InMemoryFhirRepository(fhirContext));
    doAnswer(new Answer<Bundle>() {
        @Override
        public Bundle answer(InvocationOnMock a) throws Throwable {
            Bundle b = a.getArgument(0);
            return InMemoryFhirRepository.transactionStub(b, spyRepository);
        }
    }).when(spyRepository).transaction(any());
}
  
  @Test
  void library_draft_test() {
    Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterDraftVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
    spyRepository.transaction(bundle);
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
    // 1 time for setup
    verify(spyRepository, times(2)).transaction(notNull());
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
  @Test
	void draftOperation_no_effectivePeriod_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterDraftVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
        spyRepository.transaction(bundle);
        Library baseLib = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
		assertTrue(baseLib.hasEffectivePeriod());
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        r4KnowledgeArtifactVisitor draftVisitor = new KnowledgeArtifactDraftVisitor();
		PlanDefinition planDef = spyRepository.read(PlanDefinition.class, new IdType("PlanDefinition/plandefinition-ersd-instance-example")).copy();
		assertTrue(planDef.hasEffectivePeriod());
		String version = "1.01.21.273";
		Parameters params = parameters(part("version", version) );
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(draftVisitor, spyRepository, params);


		MetadataResourceHelper.forEachMetadataResource(returnedBundle.getEntry(), resource -> {
            r4KnowledgeArtifactAdapter adapter = new AdapterFactory().createLibrary(baseLib);
			assertFalse(adapter.getEffectivePeriod().hasStart() || adapter.getEffectivePeriod().hasEnd());
		}, spyRepository);
 	}
	@Test
	void draftOperation_version_conflict_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterDraftVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
        Library versionConflictLibrary = (Library) jsonParser.parseResource(KnowledgeArtifactAdapterDraftVisitorTests.class.getResourceAsStream("Library-version-conflict.json"));
        spyRepository.transaction(bundle);
        spyRepository.update(versionConflictLibrary);
		Parameters params = parameters(part("version", "1.0.0.23") );
		String maybeException = null;
        Library baseLib = spyRepository.read(Library.class, new IdType(specificationLibReference)).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        r4KnowledgeArtifactVisitor draftVisitor = new KnowledgeArtifactDraftVisitor();

		try {
          libraryAdapter.accept(draftVisitor, spyRepository, params);

		} catch (Exception e) {
			maybeException = e.getMessage();
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.contains("already exists"));
	}
	
	@Test
	void draftOperation_cannot_create_draft_of_draft_test() {
        Library versionConflictLibrary = (Library) jsonParser.parseResource(KnowledgeArtifactAdapterDraftVisitorTests.class.getResourceAsStream("Library-version-conflict.json"));
        spyRepository.update(versionConflictLibrary);
		Parameters params = parameters(part("version", "1.2.1.23") );
		String maybeException = "";
        Library baseLib = spyRepository.read(Library.class, new IdType("Library/SpecificationLibraryDraftVersion-1-0-0-23")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        r4KnowledgeArtifactVisitor draftVisitor = new KnowledgeArtifactDraftVisitor();
		try {
            libraryAdapter.accept(draftVisitor, spyRepository, params);
		} catch (Exception e) {
			maybeException = e.getMessage();
		}
		assertNotNull(maybeException);
		assertTrue(maybeException.contains("status of 'active'"));
	}

	@Test
	void draftOperation_version_format_test() {
        Library versionConflictLibrary = (Library) jsonParser.parseResource(KnowledgeArtifactAdapterDraftVisitorTests.class.getResourceAsStream("Library-version-conflict.json"));
        spyRepository.update(versionConflictLibrary);
        Library baseLib = spyRepository.read(Library.class, new IdType("Library/SpecificationLibraryDraftVersion-1-0-0-23")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        r4KnowledgeArtifactVisitor draftVisitor = new KnowledgeArtifactDraftVisitor();

        for(String version:badVersionList){
			UnprocessableEntityException maybeException = null;
			Parameters params = parameters(part("version", new StringType(version)) );
			try {
                libraryAdapter.accept(draftVisitor, spyRepository, params);
			} catch (UnprocessableEntityException e) {
				maybeException = e;
			}
			assertNotNull(maybeException);
		}
	}
}