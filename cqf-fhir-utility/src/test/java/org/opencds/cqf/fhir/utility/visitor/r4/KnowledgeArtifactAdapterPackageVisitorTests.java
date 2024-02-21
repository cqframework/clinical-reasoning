package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;

import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatusEnumFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
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
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class KnowledgeArtifactAdapterPackageVisitorTests {
  private final FhirContext fhirContext = FhirContext.forR4Cached();
  private final IParser jsonParser = fhirContext.newJsonParser();
  private Repository spyRepository;

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
  void visitLibraryTest() {
    Bundle loadedBundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-example.json"));
    spyRepository.transaction(loadedBundle);
    KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
    Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
    r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    Parameters params = new Parameters();

    Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, params);
    assertNotNull(packagedBundle);
    assertTrue(packagedBundle.getEntry().size() == loadedBundle.getEntry().size());
}
    @Test
    void packageOperation_should_fail_non_matching_capability() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-package-capabilities.json"));
        spyRepository.transaction(bundle);
        List<String> capabilities = Arrays.asList(
            "computable",
            "publishable",
            "executable"
        );
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        // the library contains all three capabilities
        // so we should get an error when trying with
        // any one capability
        for (String capability : capabilities) {
                Parameters params = parameters(
                    part("capability", capability)
                );
                PreconditionFailedException maybeException = null;
                try {
                    libraryAdapter.accept(packageVisitor, spyRepository, params);
                } catch (PreconditionFailedException e) {
                    maybeException = e;
                }
                assertNotNull(maybeException);
        }
        Parameters allParams = parameters(
            part("capability", "computable"),
            part("capability", "publishable"),
            part("capability", "executable")
        );
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, allParams);

        // no error when running the operation with all
        // three capabilities
        assertNotNull(packaged);
    }
	@Test
	void packageOperation_should_apply_check_force_canonicalVersions() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-active-no-versions.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        String versionToUpdateTo = "1.3.1.23";
		Parameters params = parameters(
			part("artifactVersion", new CanonicalType("http://to-add-missing-version/PlanDefinition/us-ecr-specification|" + versionToUpdateTo)),
			part("artifactVersion", new CanonicalType("http://to-add-missing-version/ValueSet/dxtc|" + versionToUpdateTo))
		);
        Bundle updatedCanonicalVersionPackage = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, params);

		List<MetadataResource> updatedResources = updatedCanonicalVersionPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-add-missing-version"))
			.collect(Collectors.toList());
		assertTrue(updatedResources.size() == 2);
		for (MetadataResource updatedResource: updatedResources) {
			assertTrue(updatedResource.getVersion().equals(versionToUpdateTo));
		}
		params = parameters(
			part("checkArtifactVersion", new CanonicalType("http://to-check-version/Library/SpecificationLibrary|1.3.1"))
		);
		String correctCheckVersion = "2022-10-19";
		PreconditionFailedException checkCanonicalThrewError = null;
		try {
			libraryAdapter.accept(packageVisitor, spyRepository, params);
		} catch (PreconditionFailedException e) {
			checkCanonicalThrewError = e;
		}
		assertNotNull(checkCanonicalThrewError);
		params = parameters(
			part("checkArtifactVersion", new CanonicalType("http://to-check-version/Library/SpecificationLibrary|" + correctCheckVersion))
		);
		Bundle noErrorCheckCanonicalPackage = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, params);
		Optional<MetadataResource> checkedVersionResource = noErrorCheckCanonicalPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-check-version"))
			.findFirst();
		assertTrue(checkedVersionResource.isPresent());
		assertTrue(checkedVersionResource.get().getVersion().equals(correctCheckVersion));
		String versionToForceTo = "1.1.9.23";
		params = parameters(
			part("forceArtifactVersion", new CanonicalType("http://to-force-version/Library/rctc|" + versionToForceTo))
		);
		Bundle forcedVersionPackage = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, params);
		Optional<MetadataResource> forcedVersionResource = forcedVersionPackage.getEntry().stream()
			.map(entry -> (MetadataResource) entry.getResource())
			.filter(resource -> resource.getUrl().contains("to-force-version"))
			.findFirst();
		assertTrue(forcedVersionResource.isPresent());
		assertTrue(forcedVersionResource.get().getVersion().equals(versionToForceTo));

	}
	@Test
	void packageOperation_should_respect_count_offset() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
		Parameters countZeroParams = parameters(
			part("count", new IntegerType(0))
		);
		Bundle countZeroBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, countZeroParams);
		// when count = 0 only show the total
		assertTrue(countZeroBundle.getEntry().size() == 0);
		assertTrue(countZeroBundle.getTotal() == 6);
		Parameters count2Params = parameters(
			part("count", new IntegerType(2))
		);
		Bundle count2Bundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, count2Params);
		assertTrue(count2Bundle.getEntry().size() == 2);
		Parameters count2Offset2Params = parameters(
			part("count", new IntegerType(2)),
			part("offset", new IntegerType(2))
		);
		Bundle count2Offset2Bundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, count2Offset2Params);
		assertTrue(count2Offset2Bundle.getEntry().size() == 2);
		Parameters offset4Params = parameters(
			part("offset", new IntegerType(4))
		);
		Bundle offset4Bundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, offset4Params);
		assertTrue(offset4Bundle.getEntry().size() == (countZeroBundle.getTotal() - 4));
		assertTrue(offset4Bundle.getType() == BundleType.COLLECTION);
		assertTrue(offset4Bundle.hasTotal() == false);
		Parameters offsetMaxParams = parameters(
			part("offset", new IntegerType(countZeroBundle.getTotal()))
		);
		Bundle offsetMaxBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, offsetMaxParams);
		assertTrue(offsetMaxBundle.getEntry().size() == 0);
		Parameters offsetMaxRandomCountParams = parameters(
			part("offset", new IntegerType(countZeroBundle.getTotal())),
			part("count", new IntegerType(ThreadLocalRandom.current().nextInt(3, 20)))
		);
		Bundle offsetMaxRandomCountBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, offsetMaxRandomCountParams);
		assertTrue(offsetMaxRandomCountBundle.getEntry().size() == 0);
	}
	@Test
	void packageOperation_different_bundle_types() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
		Parameters countZeroParams = parameters(
			part("count", new IntegerType(0))
		);
		Bundle countZeroBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, countZeroParams);
		assertTrue(countZeroBundle.getType() == BundleType.SEARCHSET);
		Parameters countSevenParams = parameters(
			part("count", new IntegerType(7))
		);
		Bundle countSevenBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, countSevenParams);
		assertTrue(countSevenBundle.getType() == BundleType.TRANSACTION);
		Parameters countFourParams = parameters(
			part("count", new IntegerType(4))
		);
		Bundle countFourBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, countFourParams);
		assertTrue(countFourBundle.getType() == BundleType.COLLECTION);
		// these assertions test for Bundle base profile conformance when type = collection
		assertFalse(countFourBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
		assertFalse(countFourBundle.hasTotal());
		Parameters offsetOneParams = parameters(
			part("offset", new IntegerType(1))
		);
		Bundle offsetOneBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, offsetOneParams);
		assertTrue(offsetOneBundle.getType() == BundleType.COLLECTION);
		// these assertions test for Bundle base profile conformance when type = collection
		assertFalse(offsetOneBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
		assertFalse(offsetOneBundle.hasTotal());

		Parameters countOneOffsetOneParams = parameters(
			part("count", new IntegerType(1)),
			part("offset", new IntegerType(1))
		);
		Bundle countOneOffsetOneBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, countOneOffsetOneParams);
		assertTrue(countOneOffsetOneBundle.getType() == BundleType.COLLECTION);
		// these assertions test for Bundle base profile conformance when type = collection
		assertFalse(countOneOffsetOneBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
		assertFalse(countOneOffsetOneBundle.hasTotal());

	}
	@Test
	void packageOperation_should_conditionally_create() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
		Parameters emptyParams = parameters();
		Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, emptyParams);
		for (BundleEntryComponent component : packagedBundle.getEntry()) {
			String ifNoneExist = component.getRequest().getIfNoneExist();
			String url = ((MetadataResource) component.getResource()).getUrl();
			String version = ((MetadataResource) component.getResource()).getVersion();
			assertTrue(ifNoneExist.equals("url="+url+"&version="+version));
		}
	}
	@Test
	void packageOperation_should_be_aware_of_valueset_priority_extension() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
		Parameters emptyParams = parameters();
		Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, emptyParams);
		Optional<ValueSet> shouldBeUpdatedToEmergent = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldBeUpdatedToEmergent.isPresent());
		Optional<UsageContext> priority = shouldBeUpdatedToEmergent.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(IBaseKnowledgeArtifactAdapter.usPhContextTypeUrl) && useContext.getCode().getCode().equals(IBaseKnowledgeArtifactAdapter.valueSetPriorityCode))
			.findFirst();
		assertTrue(priority.isPresent());
		assertTrue(((CodeableConcept) priority.get().getValue()).getCoding().get(0).getCode().equals("emergent"));
		assertTrue(((CodeableConcept) priority.get().getValue()).getCoding().get(0).getSystem().equals(IBaseKnowledgeArtifactAdapter.contextUrl));

		Optional<ValueSet> shouldBeUpdatedToRoutine = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldBeUpdatedToRoutine.isPresent());
		Optional<UsageContext> priority2 = shouldBeUpdatedToRoutine.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(IBaseKnowledgeArtifactAdapter.usPhContextTypeUrl) && useContext.getCode().getCode().equals(IBaseKnowledgeArtifactAdapter.valueSetPriorityCode))
			.findFirst();
		assertTrue(priority2.isPresent());
		assertTrue(((CodeableConcept) priority2.get().getValue()).getCoding().get(0).getCode().equals("routine"));
		assertTrue(((CodeableConcept) priority2.get().getValue()).getCoding().get(0).getSystem().equals(IBaseKnowledgeArtifactAdapter.contextUrl));
	}
	
	@Test
	void packageOperation_should_be_aware_of_useContext_extension() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
		Parameters emptyParams = parameters();
		Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, emptyParams);
		Optional<ValueSet> shouldHaveFocusSetToNewValue = packagedBundle.getEntry().stream()
			.filter(entry -> entry.getResource().getResourceType().equals(ResourceType.ValueSet))
			.map(entry -> (ValueSet) entry.getResource())
			.filter(vs -> vs.getUrl().equals("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6") && vs.getVersion().equals("20210526"))
			.findFirst();
		assertTrue(shouldHaveFocusSetToNewValue.isPresent());
		Optional<UsageContext> focus = shouldHaveFocusSetToNewValue.get().getUseContext().stream()
			.filter(useContext -> useContext.getCode().getSystem().equals(IBaseKnowledgeArtifactAdapter.contextTypeUrl) && useContext.getCode().getCode().equals(IBaseKnowledgeArtifactAdapter.valueSetConditionCode))
			.findFirst();
		assertTrue(focus.isPresent());
		assertTrue(((CodeableConcept) focus.get().getValue()).getCoding().get(0).getCode().equals("49649001"));
		assertTrue(((CodeableConcept) focus.get().getValue()).getCoding().get(0).getSystem().equals("http://snomed.info/sct"));
	}

	@Test
	void packageOperation_should_respect_include() {
        Bundle bundle = (Bundle) jsonParser.parseResource(KnowledgeArtifactAdapterPackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactPackageVisitor packageVisitor = new KnowledgeArtifactPackageVisitor();
        Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
        r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
		Map<String, List<String>> includeOptions = new HashMap<String,List<String>>();
		includeOptions.put("artifact",Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
		includeOptions.put("canonical",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc",
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
			"http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"
		));
		includeOptions.put("knowledge",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
			"http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
			"http://ersd.aimsplatform.org/fhir/Library/rctc"
		));
		includeOptions.put("terminology",Arrays.asList(
			"http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
			"http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
			"http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"
		));
		includeOptions.put("conformance",Arrays.asList());
		includeOptions.put("extensions",Arrays.asList());
		includeOptions.put("profiles",Arrays.asList());
		includeOptions.put("tests",Arrays.asList());
		includeOptions.put("examples",Arrays.asList());
		for (Entry<String, List<String>> includedTypeURLs : includeOptions.entrySet()) {
			Parameters params = parameters(
				part("include", includedTypeURLs.getKey())
			);
			Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, spyRepository, params);
			List<MetadataResource> resources = packaged.getEntry().stream()
					.map(entry -> (MetadataResource) entry.getResource())
					.collect(Collectors.toList());
			for (MetadataResource resource: resources) {
				Boolean noExtraResourcesReturned = includedTypeURLs.getValue().stream()
					.anyMatch(url -> url.equals(resource.getUrl()));
				assertTrue(noExtraResourcesReturned);
			}
			for (String url: includedTypeURLs.getValue()) {
				Boolean expectedResourceReturned = resources.stream()
					.anyMatch(resource -> resource.getUrl().equals(url));
				assertTrue(expectedResourceReturned);
			}
		}
	}
}