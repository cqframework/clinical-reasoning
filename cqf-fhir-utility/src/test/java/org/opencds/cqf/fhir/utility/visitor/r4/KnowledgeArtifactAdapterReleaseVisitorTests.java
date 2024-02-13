package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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

public class KnowledgeArtifactAdapterReleaseVisitorTests {
  private final FhirContext fhirContext = FhirContext.forR4Cached();
  private Repository spyRepository;

  @BeforeEach
  public void setup() {
    IParser jsonParser = fhirContext.newJsonParser();
    Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-release-bundle.json"));
    SearchParameter sp = (SearchParameter) jsonParser.parseResource(KnowledgeArtifactAdapterReleaseVisitorTests.class.getResourceAsStream("SearchParameter-artifactAssessment.json"));
    spyRepository = spy(new InMemoryFhirRepository(fhirContext, bundle));
    spyRepository.update(sp);
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
    KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
    Library library = spyRepository.read(Library.class, new IdType("Library/ReleaseSpecificationLibrary")).copy();
    r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    String version = "1.0.1.23";
    String existingVersion = "1.2.3";
    Parameters params = new Parameters();
    params.addParameter("version", version);
    params.addParameter("versionBehavior", new CodeType("default"));

    Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, spyRepository, params);
    assertNotNull(returnResource);
		Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
		assertTrue(maybeLib.isPresent());
		Library releasedLibrary = spyRepository.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
		// versionBehaviour == 'default' so version should be
		// existingVersion and not the new version provided in
		// the parameters
		assertTrue(releasedLibrary.getVersion().equals(existingVersion));
		List<String> ersdTestArtifactDependencies = Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-dxtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-ostc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-lotc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-lrtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-mrtc|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/ValueSet/release-sdtc|" + existingVersion,
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1063|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.360|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.120|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.362|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.528|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.408|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.409|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1469|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1866|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1906|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.480|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.481|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.761|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1223|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1182|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1181|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1184|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1601|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1600|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1603|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1602|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1082|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1439|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1436|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1435|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1446|2022-10-19",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1438|2022-10-19",
			"http://notOwnedTest.com/Library/notOwnedRoot|0.1.1",
			"http://notOwnedTest.com/Library/notOwnedLeaf|0.1.1",
			"http://notOwnedTest.com/Library/notOwnedLeaf1|0.1.1"
		);
		List<String> ersdTestArtifactComponents = Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
			"http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
			"http://notOwnedTest.com/Library/notOwnedRoot|0.1.1"
		);
		List<String> dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact()
			.stream()
			.filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
			.map(ra -> ra.getResource())
			.collect(Collectors.toList());
		List<String> componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact()
			.stream()
			.filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
			.map(ra -> ra.getResource())
			.collect(Collectors.toList());
		// check that the released artifact has all the required dependencies
		for(String dependency: ersdTestArtifactDependencies){
			assertTrue(dependenciesOnReleasedArtifact.contains(dependency));
		}
		// and components
		for(String component: ersdTestArtifactComponents){
			assertTrue(componentsOnReleasedArtifact.contains(component));
		}
		assertTrue(ersdTestArtifactDependencies.size() == dependenciesOnReleasedArtifact.size());
		assertTrue(ersdTestArtifactComponents.size() == componentsOnReleasedArtifact.size());
}
}