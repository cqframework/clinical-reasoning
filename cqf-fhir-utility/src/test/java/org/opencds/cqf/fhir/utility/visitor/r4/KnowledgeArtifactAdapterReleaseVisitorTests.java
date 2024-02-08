package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
import org.mockito.Spy;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ProxyRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.RestRepository;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class KnowledgeArtifactAdapterReleaseVisitorTests {
  private final FhirContext fhirContext = FhirContext.forR4Cached();
  private Repository spyRepository;

  @BeforeEach
  public void setup() {
    IParser jsonParser = fhirContext.newJsonParser();
    EndpointStatusEnumFactory factory = new EndpointStatusEnumFactory();
    Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
    spyRepository = spy(Repositories.proxy(new InMemoryFhirRepository(fhirContext, bundle), new Endpoint(factory.fromType(new StringType(EndpointStatus.ACTIVE.toCode())), new Coding("http://terminology.hl7.org/CodeSystem/endpoint-connection-type","hl7-fhir-rest",null), new UrlType("http://localhost:8080/fhir")), null, null));
}
  
  @Test
  void visitLibraryTest() {
    // Library library = new Library();
    // library.setId("release-test-library");
    // library.setUrl("http://example.org/fhir/Library/release-test-library");
    // library.setName("ReleaseTestLibrary");
    // library.setVersion("1.22.3.4");
    // library.setStatus(PublicationStatus.ACTIVE);
    // List<RelatedArtifact> relatedArtifacts = new ArrayList<>();
    // relatedArtifacts.add(
    //     new RelatedArtifact()
    //         .setResource("http://example.org/fhir/Library/release-test-library-a")
    //         .setType(RelatedArtifactType.COMPOSEDOF)
    // );
    // relatedArtifacts.add(
    //     new RelatedArtifact()
    //         .setResource("http://example.org/fhir/Library/release-test-library-b")
    //         .setType(RelatedArtifactType.DEPENDSON)
    // );
    // relatedArtifacts.add(
    //     new RelatedArtifact()
    //         .setResource("http://example.org/fhir/Library/release-test-library-c")
    //         .setType(RelatedArtifactType.DEPENDSON)
    // );

    // library.setRelatedArtifact(relatedArtifacts);

    // dataRequirement.profile[]
    // DataRequirement dataRequirementWithProfile = new DataRequirement();
    // dataRequirementWithProfile.addProfile("http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-plandefinition");
    // dataRequirementWithProfile.addProfile("http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-valueset-library");

    // List<DataRequirement> dataRequirements = new ArrayList<>();
    // dataRequirements.add(dataRequirementWithProfile);
    // library.setDataRequirement(dataRequirements);

    // dataRequirement.codeFilter[].valueset
    // DataRequirement dataRequirementWithCodeFilter = new DataRequirement();
    // DataRequirementCodeFilterComponent codeFilter = new DataRequirementCodeFilterComponent();
    // codeFilter.setPath("code").setValueSet("http://ersd.aimsplatform.org/fhir/ValueSet/sdtc");

    // dataRequirementWithCodeFilter.addCodeFilter(codeFilter);
    // library.addDataRequirement(dataRequirementWithCodeFilter);
    KnowledgeArtifactDraftVisitor draftVisitor = new KnowledgeArtifactDraftVisitor();
    Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
    LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    String version = "1.0.1.23";
    String draftedVersion = version + "-draft";
    Parameters params = new Parameters();
    params.addParameter("version", version);
    // Root Artifact must have approval date, releaseLabel and releaseDescription for this test
    assertTrue(library.hasApprovalDate());
    assertTrue(library.hasExtension(KnowledgeArtifactAdapter.releaseDescriptionUrl));
    assertTrue(library.hasExtension(KnowledgeArtifactAdapter.releaseLabelUrl));
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
    assertFalse(lib.hasExtension(KnowledgeArtifactAdapter.releaseDescriptionUrl));
    assertFalse(lib.hasExtension(KnowledgeArtifactAdapter.releaseLabelUrl));
    List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
    assertTrue(!relatedArtifacts.isEmpty());
    MetadataResourceHelper.forEachMetadataResource(returnedBundle.getEntry(), resource -> {
        List<RelatedArtifact> relatedArtifacts2 = new org.opencds.cqf.fhir.utility.adapter.r4.KnowledgeArtifactAdapter(resource).getRelatedArtifact();
        if (relatedArtifacts2 != null && relatedArtifacts2.size() > 0) {
            for (RelatedArtifact relatedArtifact : relatedArtifacts2) {
                if (org.opencds.cqf.fhir.utility.adapter.r4.KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(relatedArtifact)) {
                    assertTrue(Canonicals.getVersion(relatedArtifact.getResource()).equals(draftedVersion));
                }
            }
        }
    }, spyRepository);
  }
}