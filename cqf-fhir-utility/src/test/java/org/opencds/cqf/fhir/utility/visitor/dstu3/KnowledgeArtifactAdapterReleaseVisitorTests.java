package org.opencds.cqf.fhir.utility.visitor.dstu3;

import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor;
import org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeArtifactAdapterReleaseVisitorTests {
  @Test
  void visitLibraryTest() {
    Library library = new Library();
    library.setId("release-test-library");
    library.setUrl("http://example.org/fhir/Library/release-test-library");
    library.setName("ReleaseTestLibrary");
    List<RelatedArtifact> relatedArtifacts = new ArrayList<>();
    relatedArtifacts.add(
        new RelatedArtifact()
            .setResource(new Reference("http://example.org/fhir/Library/release-test-library-a"))
            .setType(RelatedArtifactType.COMPOSEDOF)
    );
    relatedArtifacts.add(
        new RelatedArtifact()
            .setResource(new Reference("http://example.org/fhir/Library/release-test-library-b"))
            .setType(RelatedArtifactType.DEPENDSON)
    );
    relatedArtifacts.add(
        new RelatedArtifact()
            .setResource(new Reference("http://example.org/fhir/Library/release-test-library-c"))
            .setType(RelatedArtifactType.DEPENDSON)
    );

    library.setRelatedArtifact(relatedArtifacts);

    // dataRequirement.profile[]
    DataRequirement dataRequirementWithProfile = new DataRequirement();
    dataRequirementWithProfile.addProfile("http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-plandefinition");
    dataRequirementWithProfile.addProfile("http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-valueset-library");

    List<DataRequirement> dataRequirements = new ArrayList<>();
    dataRequirements.add(dataRequirementWithProfile);
    library.setDataRequirement(dataRequirements);

    // dataRequirement.codeFilter[].valueset
    DataRequirement dataRequirementWithCodeFilter = new DataRequirement();
    DataRequirementCodeFilterComponent codeFilter = new DataRequirementCodeFilterComponent();
    codeFilter.setPath("code").setValueSet(new Reference("http://ersd.aimsplatform.org/fhir/ValueSet/sdtc"));

    dataRequirementWithCodeFilter.addCodeFilter(codeFilter);
    library.addDataRequirement(dataRequirementWithCodeFilter);

    org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
    LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    libraryAdapter.accept(releaseVisitor);
  }
}