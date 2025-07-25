package org.opencds.cqf.fhir.cr.visitor.r5;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r5.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Endpoint;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.cr.visitor.IValueSetExpansionCache;
import org.opencds.cqf.fhir.cr.visitor.PackageVisitor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r5.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r5.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class PackageVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private final IParser jsonParser = fhirContext.newJsonParser();
    private IRepository repo;

    @BeforeEach
    void setup() {
        repo = new InMemoryFhirRepository(fhirContext);
    }

    @Test
    void visitLibraryTest() {
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-example-naive.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();

        Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);
        assertNotNull(packagedBundle);
        assertEquals(packagedBundle.getEntry().size(), loadedBundle.getEntry().size());

        List<ValueSet> leafValueSets = packagedBundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.ValueSet)
                .map(entry -> ((ValueSet) entry.getResource()))
                .filter(valueSet -> !valueSet.hasCompose()
                        || (valueSet.hasCompose()
                                && valueSet.getCompose()
                                                .getIncludeFirstRep()
                                                .getValueSet()
                                                .size()
                                        == 0))
                .toList();

        // Ensure expansion is populated for all leaf value sets
        leafValueSets.forEach(valueSet -> assertNotNull(valueSet.getExpansion()));
    }

    @Test
    @Disabled("This test needs a ValueSet that cannot be naively expanded")
    void packageOperation_should_fail_no_credentials() {
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();

        var exception = assertThrows(UnprocessableEntityException.class, () -> {
            libraryAdapter.accept(packageVisitor, params);
        });

        assertTrue(exception.getMessage().contains("Cannot expand ValueSet without credentials: "));
    }

    @Test
    @Disabled("This test needs a ValueSet that cannot be naively expanded")
    void packageOperation_should_fail_credentials_missing_username() {
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();
        Endpoint terminologyEndpoint = new Endpoint();
        terminologyEndpoint.addExtension(Constants.VSAC_USERNAME, new StringType(null));
        terminologyEndpoint.addExtension(Constants.APIKEY, new StringType("some-api-key"));
        params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);

        var exception = assertThrows(UnprocessableEntityException.class, () -> {
            libraryAdapter.accept(packageVisitor, params);
        });

        assertTrue(exception.getMessage().contains("Cannot expand ValueSet without VSAC Username: "));
    }

    @Test
    @Disabled("This test needs a ValueSet that cannot be naively expanded")
    void packageOperation_should_fail_credentials_missing_apikey() {
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();
        Endpoint terminologyEndpoint = new Endpoint();
        terminologyEndpoint.addExtension(Constants.VSAC_USERNAME, new StringType("someUsername"));
        terminologyEndpoint.addExtension(Constants.APIKEY, new StringType(null));
        params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);

        var exception = assertThrows(UnprocessableEntityException.class, () -> {
            libraryAdapter.accept(packageVisitor, params);
        });

        assertTrue(exception.getMessage().contains("Cannot expand ValueSet without VSAC API Key: "));
    }

    @Test
    @Disabled("This test needs a ValueSet that cannot be naively expanded")
    void packageOperation_should_fail_credentials_invalid() {
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();
        Endpoint terminologyEndpoint = new Endpoint();
        terminologyEndpoint.addExtension(Constants.VSAC_USERNAME, new StringType("someUsername"));
        terminologyEndpoint.addExtension(Constants.APIKEY, new StringType("some-api-key"));
        params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);

        var exception = assertThrows(UnprocessableEntityException.class, () -> {
            libraryAdapter.accept(packageVisitor, params);
        });

        assertTrue(exception.getMessage().contains("Terminology Server expansion failed for: "));
        assertTrue(exception.getAdditionalMessages().stream().allMatch(msg -> msg.contains("HTTP 401")));
    }

    @Test
    void packageOperation_should_fail_non_matching_capability() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-package-capabilities.json"));
        repo.transaction(bundle);
        List<String> capabilities = Arrays.asList("computable", "publishable", "executable");
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        // the library contains all three capabilities
        // so we should get an error when trying with
        // any one capability
        for (String capability : capabilities) {
            Parameters params = parameters(part("capability", capability));
            PreconditionFailedException maybeException = null;
            try {
                libraryAdapter.accept(packageVisitor, params);
            } catch (PreconditionFailedException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
        Parameters allParams = parameters(
                part("capability", "computable"), part("capability", "publishable"), part("capability", "executable"));
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, allParams);

        // no error when running the operation with all
        // three capabilities
        assertNotNull(packaged);
    }

    @Test
    void packageOperation_should_apply_check_force_canonicalVersions() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-active-no-versions.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        String versionToUpdateTo = "1.3.1.23";
        Parameters params = parameters(
                part(
                        "artifactVersion",
                        new CanonicalType("http://to-add-missing-version/PlanDefinition/us-ecr-specification|"
                                + versionToUpdateTo)),
                part(
                        "artifactVersion",
                        new CanonicalType("http://to-add-missing-version/ValueSet/dxtc|" + versionToUpdateTo)));
        Bundle updatedCanonicalVersionPackage = (Bundle) libraryAdapter.accept(packageVisitor, params);

        List<MetadataResource> updatedResources = updatedCanonicalVersionPackage.getEntry().stream()
                .map(entry -> (MetadataResource) entry.getResource())
                .filter(resource -> resource.getUrl().contains("to-add-missing-version"))
                .toList();
        assertEquals(2, updatedResources.size());
        for (MetadataResource updatedResource : updatedResources) {
            assertEquals(updatedResource.getVersion(), versionToUpdateTo);
        }
        params = parameters(part(
                "checkArtifactVersion",
                new CanonicalType("http://to-check-version/Library/SpecificationLibrary|1.3.1")));
        String correctCheckVersion = "2022-10-19";
        PreconditionFailedException checkCanonicalThrewError = null;
        try {
            libraryAdapter.accept(packageVisitor, params);
        } catch (PreconditionFailedException e) {
            checkCanonicalThrewError = e;
        }
        assertNotNull(checkCanonicalThrewError);
        params = parameters(part(
                "checkArtifactVersion",
                new CanonicalType("http://to-check-version/Library/SpecificationLibrary|" + correctCheckVersion)));
        Bundle noErrorCheckCanonicalPackage = (Bundle) libraryAdapter.accept(packageVisitor, params);
        Optional<MetadataResource> checkedVersionResource = noErrorCheckCanonicalPackage.getEntry().stream()
                .map(entry -> (MetadataResource) entry.getResource())
                .filter(resource -> resource.getUrl().contains("to-check-version"))
                .findFirst();
        assertTrue(checkedVersionResource.isPresent());
        assertEquals(checkedVersionResource.get().getVersion(), correctCheckVersion);
        String versionToForceTo = "1.1.9.23";
        params = parameters(part(
                "forceArtifactVersion", new CanonicalType("http://to-force-version/Library/rctc|" + versionToForceTo)));
        Bundle forcedVersionPackage = (Bundle) libraryAdapter.accept(packageVisitor, params);
        Optional<MetadataResource> forcedVersionResource = forcedVersionPackage.getEntry().stream()
                .map(entry -> (MetadataResource) entry.getResource())
                .filter(resource -> resource.getUrl().contains("to-force-version"))
                .findFirst();
        assertTrue(forcedVersionResource.isPresent());
        assertEquals(forcedVersionResource.get().getVersion(), versionToForceTo);
    }

    @Test
    void packageOperation_should_respect_count_offset() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters countZeroParams = parameters(part("count", new IntegerType(0)));
        Bundle countZeroBundle = (Bundle) libraryAdapter.accept(packageVisitor, countZeroParams);
        // when count = 0 only show the total
        assertEquals(0, countZeroBundle.getEntry().size());
        assertEquals(6, countZeroBundle.getTotal());
        Parameters count2Params = parameters(part("count", new IntegerType(2)));
        Bundle count2Bundle = (Bundle) libraryAdapter.accept(packageVisitor, count2Params);
        assertEquals(2, count2Bundle.getEntry().size());
        Parameters count2Offset2Params =
                parameters(part("count", new IntegerType(2)), part("offset", new IntegerType(2)));
        Bundle count2Offset2Bundle = (Bundle) libraryAdapter.accept(packageVisitor, count2Offset2Params);
        assertEquals(2, count2Offset2Bundle.getEntry().size());
        Parameters offset4Params = parameters(part("offset", new IntegerType(4)));
        Bundle offset4Bundle = (Bundle) libraryAdapter.accept(packageVisitor, offset4Params);
        assertEquals(offset4Bundle.getEntry().size(), (countZeroBundle.getTotal() - 4));
        assertSame(BundleType.COLLECTION, offset4Bundle.getType());
        assertFalse(offset4Bundle.hasTotal());
        Parameters offsetMaxParams = parameters(part("offset", new IntegerType(countZeroBundle.getTotal())));
        Bundle offsetMaxBundle = (Bundle) libraryAdapter.accept(packageVisitor, offsetMaxParams);
        assertEquals(0, offsetMaxBundle.getEntry().size());
        Parameters offsetMaxRandomCountParams = parameters(
                part("offset", new IntegerType(countZeroBundle.getTotal())),
                part("count", new IntegerType(ThreadLocalRandom.current().nextInt(3, 20))));
        Bundle offsetMaxRandomCountBundle = (Bundle) libraryAdapter.accept(packageVisitor, offsetMaxRandomCountParams);
        assertEquals(0, offsetMaxRandomCountBundle.getEntry().size());
    }

    @Test
    void packageOperation_different_bundle_types() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters countZeroParams = parameters(part("count", new IntegerType(0)));
        Bundle countZeroBundle = (Bundle) libraryAdapter.accept(packageVisitor, countZeroParams);
        assertSame(BundleType.SEARCHSET, countZeroBundle.getType());
        Parameters countSevenParams = parameters(part("count", new IntegerType(7)));
        Bundle countSevenBundle = (Bundle) libraryAdapter.accept(packageVisitor, countSevenParams);
        assertSame(BundleType.TRANSACTION, countSevenBundle.getType());
        Parameters countFourParams = parameters(part("count", new IntegerType(4)));
        Bundle countFourBundle = (Bundle) libraryAdapter.accept(packageVisitor, countFourParams);
        assertSame(BundleType.COLLECTION, countFourBundle.getType());
        // these assertions test for Bundle base profile conformance when type = collection
        assertFalse(countFourBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
        assertFalse(countFourBundle.hasTotal());
        Parameters offsetOneParams = parameters(part("offset", new IntegerType(1)));
        Bundle offsetOneBundle = (Bundle) libraryAdapter.accept(packageVisitor, offsetOneParams);
        assertSame(BundleType.COLLECTION, offsetOneBundle.getType());
        // these assertions test for Bundle base profile conformance when type = collection
        assertFalse(offsetOneBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
        assertFalse(offsetOneBundle.hasTotal());

        Parameters countOneOffsetOneParams =
                parameters(part("count", new IntegerType(1)), part("offset", new IntegerType(1)));
        Bundle countOneOffsetOneBundle = (Bundle) libraryAdapter.accept(packageVisitor, countOneOffsetOneParams);
        assertSame(BundleType.COLLECTION, countOneOffsetOneBundle.getType());
        // these assertions test for Bundle base profile conformance when type = collection
        assertFalse(countOneOffsetOneBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
        assertFalse(countOneOffsetOneBundle.hasTotal());
    }

    @Test
    void packageOperation_should_conditionally_create() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters emptyParams = parameters();
        Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, emptyParams);
        for (BundleEntryComponent component : packagedBundle.getEntry()) {
            String ifNoneExist = component.getRequest().getIfNoneExist();
            String url = ((MetadataResource) component.getResource()).getUrl();
            String version = ((MetadataResource) component.getResource()).getVersion();
            assertEquals(ifNoneExist, "url=" + url + "&version=" + version);
        }
    }

    @Test
    void packageOperation_should_respect_include() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Map<String, List<String>> includeOptions = new HashMap<String, List<String>>();
        includeOptions.put("artifact", Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
        includeOptions.put(
                "canonical",
                Arrays.asList(
                        "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
                        "http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
                        "http://ersd.aimsplatform.org/fhir/Library/rctc",
                        "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"));
        includeOptions.put(
                "knowledge",
                Arrays.asList(
                        "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
                        "http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
                        "http://ersd.aimsplatform.org/fhir/Library/rctc"));
        includeOptions.put(
                "terminology",
                Arrays.asList(
                        "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"));
        includeOptions.put("conformance", Arrays.asList());
        includeOptions.put("extensions", Arrays.asList());
        includeOptions.put("profiles", Arrays.asList());
        includeOptions.put("tests", Arrays.asList());
        includeOptions.put("examples", Arrays.asList());
        for (Entry<String, List<String>> includedTypeURLs : includeOptions.entrySet()) {
            Parameters params = parameters(part("include", includedTypeURLs.getKey()));
            Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);
            List<MetadataResource> resources = packaged.getEntry().stream()
                    .map(entry -> (MetadataResource) entry.getResource())
                    .toList();
            for (MetadataResource resource : resources) {
                Boolean noExtraResourcesReturned =
                        includedTypeURLs.getValue().stream().anyMatch(url -> url.equals(resource.getUrl()));
                assertTrue(noExtraResourcesReturned);
            }
            for (String url : includedTypeURLs.getValue()) {
                Boolean expectedResourceReturned = resources.stream()
                        .anyMatch(resource -> resource.getUrl().equals(url));
                assertTrue(expectedResourceReturned);
            }
        }
    }

    @Test
    void packageVisitorShouldUseExpansionCacheIfProvided() {
        // Arrange
        var bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        var library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var libraryAdapter = new AdapterFactory().createLibrary(library);
        var mockCache = Mockito.mock(IValueSetExpansionCache.class);
        var packageVisitor = new PackageVisitor(repo, null, mockCache);

        var canonical1 = "http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine|20210526";
        var mockValueSetAdapter1 = Mockito.mock(ValueSetAdapter.class);
        when(mockValueSetAdapter1.getExpansion()).thenReturn(new ValueSetExpansionComponent());
        when(mockValueSetAdapter1.getCanonical()).thenReturn(canonical1);

        when(mockCache.getExpansionForCanonical(canonical1, null)).thenReturn(mockValueSetAdapter1);

        // Act
        var params = parameters();
        libraryAdapter.accept(packageVisitor, params);

        // Assert
        // there are three ValueSets being expanded and only the first one is in the cache
        verify(mockCache, times(1)).getExpansionForCanonical(canonical1, null);

        verify(mockCache, times(1))
                .getExpansionForCanonical(
                        "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|20210526", null);
        verify(mockCache, times(1))
                .getExpansionForCanonical("http://ersd.aimsplatform.org/fhir/ValueSet/dxtc|2022-11-19", null);

        // the other two will be added to the cache after expansion if possible
        verify(mockCache, times(2)).addToCache(any(), isNull());
        verify(mockCache, times(1)).getExpansionParametersHash(any(LibraryAdapter.class));
    }

    @Test
    void fallback_to_tx_server_if_valueset_missing_locally() {
        final var leafOid = "2.16.840.1.113762.1.4.1146.6";
        final var authoritativeSource = "http://cts.nlm.nih.gov/fhir/";
        var bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        Predicate<BundleEntryComponent> leafFinder = e -> e.getResource().getResourceType() == ResourceType.ValueSet
                && ((ValueSet) e.getResource()).getUrl().contains(leafOid);
        // remove leaf from bundle
        var leafEntry = bundle.getEntry().stream().filter(leafFinder).findFirst();
        var missingVset = leafEntry.map(e -> (ValueSet) e.getResource()).get();
        bundle.getEntry().remove(leafEntry.get());

        repo.transaction(bundle);
        var library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var endpoint = createEndpoint(authoritativeSource);

        var clientMock = mock(TerminologyServerClient.class, new ReturnsDeepStubs());
        // expect the Tx Server to provide the missing ValueSet
        when(clientMock.getResource(any(IEndpointAdapter.class), any())).thenReturn(Optional.of(missingVset));
        doAnswer(new Answer<ValueSet>() {
                    @Override
                    public ValueSet answer(InvocationOnMock invocation) throws Throwable {
                        return new ValueSet(); // Return a new instance of ValueSet
                    }
                })
                .when(clientMock)
                .expand(any(IValueSetAdapter.class), any(IEndpointAdapter.class), any(IParametersAdapter.class));
        var packageVisitor = new PackageVisitor(repo, clientMock);
        var libraryAdapter = new AdapterFactory().createLibrary(library);
        var params = parameters(part("terminologyEndpoint", (org.hl7.fhir.r5.model.Endpoint) endpoint.get()));
        // create package
        var packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);
        var containsVset = packagedBundle.getEntry().stream().anyMatch(leafFinder);
        // check for ValueSet
        assertTrue(containsVset);
    }

    private IEndpointAdapter createEndpoint(String authoritativeSource) {
        var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R5);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.r5.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.r5.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.r5.model.StringType("username")));
        endpoint.addExtension(new org.hl7.fhir.r5.model.Extension(
                Constants.APIKEY, new org.hl7.fhir.r5.model.StringType("password")));
        return endpoint;
    }
}
