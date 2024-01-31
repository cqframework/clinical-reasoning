package org.opencds.cqf.cql.evaluator.fhir.visitor.r4;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.serializer.FhirResourceDeserializer;

public class KnowledgeArtifactAdapterReleaseVisitorTests {
  private final FhirContext fhirContext = FhirContext.forR4Cached();
  private Repository repository;

  @BeforeClass
  public void setup() {
    var repository = TestRepositoryFactory.createRepository(
        fhirContext, this.getClass(), "org/opencds/cqf/fhir/cr/plandefinition/r4/opioid-Rec10-patient-view");
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
    Library library = repository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
    LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    libraryAdapter.accept(draftVisitor, repository);
  }
}