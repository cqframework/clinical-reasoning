package org.opencds.cqf.fhir.cr.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.r4.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class PackageVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final IParser jsonParser = fhirContext.newJsonParser();
    private IRepository repo;
    protected static final String CRMI_INTENDED_USAGE_CONTEXT_URL =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-intendedUsageContext";

    @BeforeEach
    void setup() {
        repo = new InMemoryFhirRepository(fhirContext);
    }

    @Test
    void visitLibraryTest() {
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-example-naive.json"));
        repo.transaction(loadedBundle);
        var settings = TerminologyServerClientSettings.getDefault()
                .setMaxRetryCount(5)
                .setRetryIntervalMillis(500)
                .setTimeoutSeconds(10)
                .setSocketTimeout(45)
                .setCrmiVersion("2.0.0")
                .setExpansionsPerPage(500)
                .setMaxExpansionPages(500);
        var settingsCopy = new TerminologyServerClientSettings(settings);

        var packageVisitor = new PackageVisitor(repo, settingsCopy, null);
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
        Parameters params = parameters();

        var exception = assertThrows(UnprocessableEntityException.class, () -> {
            libraryAdapter.accept(packageVisitor, params);
        });

        assertTrue(exception.getMessage().contains("Cannot expand ValueSet without a terminology server: "));
    }

    @ParameterizedTest
    @CsvSource({
        ",some-api-key,Found a vsacUsername extension with no value",
        "someUsername,,Found a apiKey extension with no value",
    })
    void packageOperation_should_fail(@Nullable String username, String apiKey, String expectedError) {
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Endpoint terminologyEndpoint = new Endpoint();
        terminologyEndpoint.addExtension(Constants.VSAC_USERNAME, new StringType(username));
        terminologyEndpoint.addExtension(Constants.APIKEY, new StringType(apiKey));
        terminologyEndpoint.setAddress("test.com");
        Parameters params = parameters(part("terminologyEndpoint", terminologyEndpoint));

        var result = (Bundle) libraryAdapter.accept(packageVisitor, params);

        // Get the Library from the result bundle to check for messages
        Library bundledLibrary = result.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.Library)
                .map(entry -> (Library) entry.getResource())
                .filter(lib -> lib.getUrl().equals(library.getUrl()))
                .findFirst()
                .orElse(null);

        assertNotNull(bundledLibrary, "Expected Library to be in the packaged bundle");
        ILibraryAdapter bundledAdapter = new AdapterFactory().createLibrary(bundledLibrary);

        assertTrue(bundledAdapter.hasExtension(ILibraryAdapter.CQF_MESSAGES_EXT_URL));
        assertTrue(bundledAdapter.hasContained());
        assertTrue(bundledAdapter.getContained().stream().allMatch(c -> c instanceof OperationOutcome));
        var oo = (OperationOutcome) bundledAdapter.getContained().get(0);

        // Check that the expected error exists in the issues (may not be the first issue)
        boolean foundExpectedError =
                oo.getIssue().stream().anyMatch(issue -> issue.getDiagnostics().equals(expectedError));
        assertTrue(foundExpectedError, "Expected to find issue with diagnostics: " + expectedError);
    }

    @Test
    void packageOperation_expansion_should_fail() {
        String username = "someUsername";
        String apiKey = "some-api-key";
        String expectedError = "Cannot expand ValueSet without a terminology server: ValueSet/dxtc";
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active-intensional-vs.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Endpoint terminologyEndpoint = new Endpoint();
        terminologyEndpoint.addExtension(Constants.VSAC_USERNAME, new StringType(username));
        terminologyEndpoint.addExtension(Constants.APIKEY, new StringType(apiKey));
        terminologyEndpoint.setAddress("test.com");
        Parameters params = parameters(part("terminologyEndpoint", terminologyEndpoint));

        libraryAdapter.accept(packageVisitor, params);

        assertTrue(libraryAdapter.hasExtension(ILibraryAdapter.CQF_MESSAGES_EXT_URL));
    }

    @Test
    void packageOperation_should_fail_paging_with_transaction_bundle_type_request() {
        String expectedError = "It is invalid to use paging when requesting a bundle of type 'transaction'";
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active-intensional-vs.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(part("bundleType", "transaction"));
        params.addParameter("count", 1);

        InvalidRequestException exception = null;
        try {
            libraryAdapter.accept(packageVisitor, params);
        } catch (InvalidRequestException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(exception.getMessage(), expectedError);
    }

    @Test
    void packageOperation_should_fail_negative_count_parameter() {
        String expectedError = "'count' must be non-negative";
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active-intensional-vs.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(part("bundleType", "transaction"));
        params.addParameter("count", -1);

        InvalidRequestException exception = null;
        try {
            libraryAdapter.accept(packageVisitor, params);
        } catch (InvalidRequestException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(exception.getMessage(), expectedError);
    }

    @Test
    void packageOperation_should_fail_negative_offset_parameter() {
        String expectedError = "'offset' must be non-negative";
        Bundle loadedBundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active-intensional-vs.json"));
        repo.transaction(loadedBundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(part("bundleType", "transaction"));
        params.addParameter("offset", -1);

        InvalidRequestException exception = null;
        try {
            libraryAdapter.accept(packageVisitor, params);
        } catch (InvalidRequestException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(exception.getMessage(), expectedError);
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
            PreconditionFailedException exception = null;
            try {
                libraryAdapter.accept(packageVisitor, params);
            } catch (PreconditionFailedException e) {
                exception = e;
            }
            assertNotNull(exception);
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
        assertEquals((countZeroBundle.getTotal() - 4), offset4Bundle.getEntry().size());
        assertSame(BundleType.SEARCHSET, offset4Bundle.getType());
        assertTrue(offset4Bundle.hasTotal());
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
        var packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        Parameters countZeroParams = parameters(part("count", new IntegerType(0)));
        Bundle countZeroBundle = (Bundle) libraryAdapter.accept(packageVisitor, countZeroParams);
        assertSame(BundleType.SEARCHSET, countZeroBundle.getType());

        Parameters countSevenParams = parameters(part("count", new IntegerType(7)));
        Bundle countSevenBundle = (Bundle) libraryAdapter.accept(packageVisitor, countSevenParams);
        assertSame(BundleType.SEARCHSET, countSevenBundle.getType());

        Parameters collectionBundleTypeParams = parameters(part("bundleType", "collection"));
        Bundle collectionBundleType = (Bundle) libraryAdapter.accept(packageVisitor, collectionBundleTypeParams);
        assertSame(BundleType.COLLECTION, collectionBundleType.getType());
        // these assertions test for Bundle base profile conformance when type = collection
        assertFalse(collectionBundleType.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
        assertFalse(collectionBundleType.hasTotal());

        Parameters transactionBundleTypeParams = parameters(part("bundleType", "transaction"));
        Bundle transactionBundle = (Bundle) libraryAdapter.accept(packageVisitor, transactionBundleTypeParams);
        assertSame(BundleType.TRANSACTION, transactionBundle.getType());
        // these assertions test for Bundle base profile conformance when type = collection
        assertTrue(transactionBundle.getEntry().stream().anyMatch(entry -> entry.hasRequest()));
        assertFalse(transactionBundle.hasTotal());
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
        Map<String, List<String>> includeOptions = new HashMap<>();
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
                        "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
                        "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"));
        includeOptions.put(
                "conformance", Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
        includeOptions.put(
                "extensions", Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
        includeOptions.put("profiles", Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
        includeOptions.put("tests", Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
        includeOptions.put("examples", Arrays.asList("http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary"));
        // FHIR Types
        includeOptions.put(
                "PlanDefinition",
                Arrays.asList(
                        "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
                        "http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification"));
        includeOptions.put(
                "ValueSet",
                Arrays.asList(
                        "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
                        "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
                        "http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine"));
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
    void packageOperation_include_get_resources_by_fhir_type_only() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        List<String> expectedUrls = Arrays.asList(
                "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6",
                "http://cts.nlm.nih.gov/fhir/ValueSet/123-this-will-be-routine",
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
                "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary");

        Parameters params = parameters(part("include", "PlanDefinition"), part("include", "ValueSet"));
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);
        List<String> actualUrls = packaged.getEntry().stream()
                .map(entry -> ((MetadataResource) entry.getResource()).getUrl())
                .sorted()
                .toList();
        Collections.sort(expectedUrls);
        assertEquals(actualUrls, expectedUrls);
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
        var packageVisitor = new PackageVisitor(repo, (TerminologyServerClient) null, mockCache);

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
        when(clientMock.getValueSetResource(any(IEndpointAdapter.class), any())).thenReturn(Optional.of(missingVset));
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
        var params = parameters(part("terminologyEndpoint", (org.hl7.fhir.r4.model.Endpoint) endpoint.get()));
        // create package
        var packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);
        var containsVset = packagedBundle.getEntry().stream().anyMatch(leafFinder);
        // check for ValueSet
        assertTrue(containsVset);
    }

    @Test
    void packageOperation_manifest_is_first_with_include_terminology() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        Parameters params = parameters(part("include", "terminology"));
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        assertNotNull(packaged);
        assertTrue(packaged.hasEntry());
        // First entry must be the outcome manifest Library
        assertEquals("Library", packaged.getEntryFirstRep().getResource().fhirType());

        // Everything after the manifest should be terminology-only
        for (int i = 1; i < packaged.getEntry().size(); i++) {
            String t = packaged.getEntry().get(i).getResource().fhirType();
            boolean isTerminology = "ValueSet".equals(t)
                    || "CodeSystem".equals(t)
                    || "ConceptMap".equals(t)
                    || "NamingSystem".equals(t);
            assertTrue(isTerminology, "Non-terminology entry returned with include=terminology: " + t);
        }
    }

    @Test
    void packageOperation_manifest_is_first_with_include_valueset() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        Parameters params = parameters(part("include", "ValueSet"));
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        assertNotNull(packaged);
        assertTrue(packaged.hasEntry());
        // Manifest always first
        assertEquals("Library", packaged.getEntryFirstRep().getResource().fhirType());

        // Remaining entries should be ValueSets only
        for (int i = 1; i < packaged.getEntry().size(); i++) {
            String t = packaged.getEntry().get(i).getResource().fhirType();
            assertEquals("ValueSet", t, "Non-ValueSet entry returned with include=ValueSet: " + t);
        }
    }

    @Test
    void packageOperation_unknown_include_returns_only_manifest() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        Parameters params = parameters(part("include", "not-a-real-category"));
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        assertNotNull(packaged);
        assertEquals(1, packaged.getEntry().size());
        assertEquals("Library", packaged.getEntryFirstRep().getResource().fhirType());
    }

    @Test
    void packageOperation_include_tests_returns_only_test_marked_entries() {
        // Arrange
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        // Mark one non-manifest Library as a test-case via extension that contains 'isTestCase'
        Library manifest = (Library) bundle.getEntryFirstRep().getResource();
        Library testLib = null;
        for (int i = 1; i < bundle.getEntry().size(); i++) {
            if (bundle.getEntry().get(i).getResource() instanceof Library) {
                testLib = (Library) bundle.getEntry().get(i).getResource();
                break;
            }
        }
        assertNotNull(testLib, "Test library not found in fixture bundle");
        testLib.addExtension("http://example.org/extensions/isTestCase", new org.hl7.fhir.r4.model.BooleanType(true));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(manifest.copy());

        // Act
        Parameters params = parameters(part("include", "tests"));
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        // Assert
        assertNotNull(packaged);
        assertTrue(packaged.hasEntry());
        // Manifest first
        assertEquals("Library", packaged.getEntryFirstRep().getResource().fhirType());
        // All subsequent resources must be the test-marked entry/entries
        for (int i = 1; i < packaged.getEntry().size(); i++) {
            var r = packaged.getEntry().get(i).getResource();
            assertTrue(r instanceof Library, "Only test-marked Libraries should be returned for include=tests");
            var extOk = ((Library) r)
                    .getExtension().stream()
                            .anyMatch(x -> x.getUrl().contains("isTestCase")
                                    && ((org.hl7.fhir.r4.model.BooleanType) x.getValue()).booleanValue());
            assertTrue(extOk, "Returned entry was not marked as test-case via isTestCase extension");
        }
    }

    @Test
    void packageOperation_include_examples_returns_only_example_marked_entries() {
        // Arrange
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        // Mark one non-manifest resource as example via extension that contains 'isExample'
        Library manifest = (Library) bundle.getEntryFirstRep().getResource();
        // Prefer a ValueSet if present; otherwise mark the next resource
        org.hl7.fhir.r4.model.DomainResource exampleRes = null;
        for (int i = 1; i < bundle.getEntry().size(); i++) {
            if (bundle.getEntry().get(i).getResource() instanceof org.hl7.fhir.r4.model.ValueSet) {
                exampleRes = (org.hl7.fhir.r4.model.ValueSet)
                        bundle.getEntry().get(i).getResource();
                break;
            }
        }
        if (exampleRes == null) {
            for (int i = 1; i < bundle.getEntry().size(); i++) {
                if (bundle.getEntry().get(i).getResource() instanceof org.hl7.fhir.r4.model.DomainResource) {
                    exampleRes = (org.hl7.fhir.r4.model.DomainResource)
                            bundle.getEntry().get(i).getResource();
                    break;
                }
            }
        }
        assertNotNull(exampleRes, "Example resource not found in fixture bundle");
        exampleRes.addExtension("http://example.org/extensions/isExample", new org.hl7.fhir.r4.model.BooleanType(true));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(manifest.copy());

        // Act
        Parameters params = parameters(part("include", "examples"));
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        // Assert
        assertNotNull(packaged);
        assertTrue(packaged.hasEntry());
        // Manifest first
        assertEquals("Library", packaged.getEntryFirstRep().getResource().fhirType());
        // All subsequent resources must carry the isExample=true extension
        for (int i = 1; i < packaged.getEntry().size(); i++) {
            var r = packaged.getEntry().get(i).getResource();
            assertTrue(
                    r instanceof org.hl7.fhir.r4.model.DomainResource, "Expected DomainResource entries for examples");
            var extOk = ((org.hl7.fhir.r4.model.DomainResource) r)
                    .getExtension().stream()
                            .anyMatch(x -> x.getUrl().contains("isExample")
                                    && ((org.hl7.fhir.r4.model.BooleanType) x.getValue()).booleanValue());
            assertTrue(extOk, "Returned entry was not marked as example via isExample extension");
        }
    }

    @Test
    void packageOperation_normalizes_ids_from_canonical_tail_and_version() {
        // Arrange
        Bundle bundle = (Bundle) jsonParser.parseResource(
                PackageVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        // Act
        Parameters params = parameters();
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        // Assert: manifest Library id should be normalized to tail-version
        assertNotNull(packaged);
        assertTrue(packaged.hasEntry());
        var manifest = (Library) packaged.getEntryFirstRep().getResource();
        var manifestUrl = manifest.getUrl();
        var manifestVersion = manifest.getVersion();
        var tail = manifestUrl.substring(manifestUrl.lastIndexOf('/') + 1);
        assertEquals(tail + "-" + manifestVersion, manifest.getIdElement().getIdPart());

        // Assert: a known ValueSet also has its id normalized to tail-version
        var dxtcValueSet = packaged.getEntry().stream()
                .map(BundleEntryComponent::getResource)
                .filter(r -> r instanceof ValueSet)
                .map(ValueSet.class::cast)
                .filter(vs -> "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc".equals(vs.getUrl()))
                .findFirst()
                .orElse(null);

        assertNotNull(dxtcValueSet, "Expected ValueSet dxtc to be present in packaged bundle");
        assertEquals("dxtc-2022-11-19", dxtcValueSet.getIdElement().getIdPart());
    }

    @Test
    void packageOperation_normalizes_ids_with_encoded_canonical_when_version_not_id_safe() {
        // Arrange: create a Library with a version that contains a character not allowed in FHIR ids,
        // and a short canonical URL so the encoded form fits under the FHIR id length limit.
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library lib = new Library();
        lib.setUrl("http://x/L/W");
        lib.setVersion("1.0.0#1"); // '#' is not allowed in FHIR id values
        lib.setId("server-assigned-id");

        // Use a copy via the adapter as the manifest artifact
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(lib.copy());

        // Act
        Parameters params = parameters();
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        // Assert
        assertNotNull(packaged);
        assertTrue(packaged.hasEntry());
        var manifest = (Library) packaged.getEntryFirstRep().getResource();
        String id = manifest.getIdElement().getIdPart();

        // Because the version is not FHIR-id-safe but the identity string is short,
        // we expect the encoded canonical form with cv- prefix.
        assertTrue(id.startsWith("cv-"), "Expected encoded canonical id prefix cv- for non-id-safe version");
        assertTrue(id.length() <= 64, "FHIR id must not exceed 64 characters");
        assertTrue(id.substring(3).matches("[A-Za-z0-9\\-.]+"), "Encoded id should only use FHIR id-safe characters");

        // Decode the cv-encoded id back to the original canonical|version identity string
        String decodedIdentity = decodeCanonicalFromCvId(id);
        assertEquals(
                lib.getUrl() + "|" + lib.getVersion(),
                decodedIdentity,
                "Decoded cv-encoded id should round-trip to canonical|version");
    }

    @Test
    void packageOperation_retains_existing_id_and_adds_warning_when_canonical_cannot_be_normalized() {
        // Arrange: create a Library with a long canonical and a non-id-safe version so that the
        // canonical|version cannot be represented non-lossily within FHIR id constraints.
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        Library lib = new Library();
        lib.setUrl("http://example.org/fhir/Library/" + "VeryLongWeirdName".repeat(5));
        lib.setVersion("1.0.0#1"); // non-id-safe due to '#'
        lib.setId("server-assigned-id");

        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(lib.copy());

        // Act
        Parameters params = parameters();
        Bundle packaged = (Bundle) libraryAdapter.accept(packageVisitor, params);

        // Assert: the manifest Library should retain the existing server-assigned id
        assertNotNull(packaged);
        assertTrue(packaged.hasEntry());
        var manifest = (Library) packaged.getEntryFirstRep().getResource();
        String id = manifest.getIdElement().getIdPart();
        assertEquals(
                "server-assigned-id",
                id,
                "Expected manifest Library to retain existing id when canonical cannot be normalized non-lossily");

        // And the manifest artifact should have a cqf-messages extension with a warning recorded
        assertTrue(
                libraryAdapter.hasExtension(ILibraryAdapter.CQF_MESSAGES_EXT_URL),
                "Expected manifest adapter to have cqf-messages extension when id normalization fails");
        assertTrue(
                libraryAdapter.hasContained(),
                "Expected manifest adapter to contain an OperationOutcome warning when id normalization fails");
        var oo = (OperationOutcome) libraryAdapter.getContained().get(0);
        assertTrue(
                oo.getIssueFirstRep().getDiagnostics().contains("could not be normalized from canonical"),
                "Expected warning OperationOutcome to describe failed canonical-based id normalization");
    }

    private IEndpointAdapter createEndpoint(String authoritativeSource) {
        var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.r4.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.r4.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.r4.model.StringType("username")));
        endpoint.addExtension(new org.hl7.fhir.r4.model.Extension(
                Constants.APIKEY, new org.hl7.fhir.r4.model.StringType("password")));
        return endpoint;
    }

    @Test
    void adds_proposed_usage_context_when_non_equal_to_existing() throws Exception {
        PackageVisitor visitor = new PackageVisitor(repo);
        var adapterFactoryMock = mock(AdapterFactory.class);
        var afField = visitor.getClass().getDeclaredField("adapterFactory");
        afField.setAccessible(true);
        afField.set(visitor, adapterFactoryMock);

        IBaseBundle bundleMock = mock(Bundle.class);
        ValueSet entryResource = new ValueSet();

        @SuppressWarnings("unchecked")
        var staticBundleHelper = org.mockito.Mockito.mockStatic(org.opencds.cqf.fhir.utility.BundleHelper.class);
        staticBundleHelper
                .when(() -> org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources(bundleMock))
                .thenReturn(List.of(entryResource));

        // Create IValueSetAdapter that will be returned by adapterFactory.createValueSet(...)
        IValueSetAdapter vsAdapterMock = mock(IValueSetAdapter.class);
        when(adapterFactoryMock.createValueSet(entryResource)).thenReturn(vsAdapterMock);
        when(vsAdapterMock.getUrl()).thenReturn("http://example.org/ValueSet/foo");
        when(vsAdapterMock.hasVersion()).thenReturn(false);

        // existing UseContext on the ValueSet (model object)
        UsageContext existingUc = new UsageContext();
        existingUc.setCode(new Coding().setCode("focus"));
        existingUc.setValue(new CodeableConcept().addCoding(new Coding().setCode("C1")));
        when(vsAdapterMock.getUseContext()).thenReturn(List.of(existingUc));

        // manifest and dependency wiring
        var manifestMock = mock(org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter.class);
        var dependency = mock(org.opencds.cqf.fhir.utility.adapter.IDependencyInfo.class);
        when(manifestMock.getDependencies()).thenReturn(List.of(dependency));
        when(dependency.getReference()).thenReturn("http://example.org/ValueSet/foo");

        // proposed UsageContext in extension
        UsageContext proposedUc = new UsageContext();
        proposedUc.setCode(new Coding().setCode("priority"));
        proposedUc.setValue(new CodeableConcept().addCoding(new Coding().setCode("routine")));
        Extension ext = new Extension(CRMI_INTENDED_USAGE_CONTEXT_URL, proposedUc);
        when(dependency.getExtension()).thenReturn(List.of(ext));

        var existingAdapter = mock(org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter.class);
        var proposedAdapter = mock(org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter.class);
        when(adapterFactoryMock.createUsageContext(existingUc)).thenReturn(existingAdapter);
        when(adapterFactoryMock.createUsageContext(ext.getValue())).thenReturn(proposedAdapter);

        when(existingAdapter.equalsDeep(proposedAdapter)).thenReturn(false);
        when(adapterFactoryMock.createValueSet(any())).thenReturn(vsAdapterMock);

        var method = visitor.getClass()
                .getDeclaredMethod(
                        "applyManifestUsageContextsToValueSets",
                        org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter.class,
                        IBaseBundle.class);
        method.setAccessible(true);
        method.invoke(visitor, manifestMock, bundleMock);

        verify(vsAdapterMock, times(1)).addUseContext(proposedAdapter);

        staticBundleHelper.close();
    }

    @Test
    void skips_adding_when_existing_equals_proposed() throws Exception {
        PackageVisitor visitor = new PackageVisitor(repo);
        var adapterFactoryMock = mock(AdapterFactory.class);
        var afField = visitor.getClass().getDeclaredField("adapterFactory");
        afField.setAccessible(true);
        afField.set(visitor, adapterFactoryMock);

        IBaseBundle bundleMock = mock(Bundle.class);
        ValueSet entryResource = new ValueSet();

        // mock static BundleHelper to return the resource
        @SuppressWarnings("unchecked")
        var staticBundleHelper = org.mockito.Mockito.mockStatic(org.opencds.cqf.fhir.utility.BundleHelper.class);
        staticBundleHelper
                .when(() -> org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources(bundleMock))
                .thenReturn(List.of(entryResource));

        // Create IValueSetAdapter that will be returned by adapterFactory.createValueSet(...)
        IValueSetAdapter vsAdapterMock = mock(IValueSetAdapter.class);
        when(adapterFactoryMock.createValueSet(entryResource)).thenReturn(vsAdapterMock);
        when(vsAdapterMock.getUrl()).thenReturn("http://example.org/ValueSet/foo");
        when(vsAdapterMock.hasVersion()).thenReturn(false);

        // existing UseContext on the ValueSet (model object)
        UsageContext existingUc = new UsageContext();
        existingUc.setCode(new Coding().setCode("priority"));
        existingUc.setValue(new CodeableConcept().addCoding(new Coding().setCode("routine")));
        when(vsAdapterMock.getUseContext()).thenReturn(List.of(existingUc));

        // manifest and dependency wiring
        var manifestMock = mock(org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter.class);
        var dependency = mock(org.opencds.cqf.fhir.utility.adapter.IDependencyInfo.class);
        when(manifestMock.getDependencies()).thenReturn(List.of(dependency));
        when(dependency.getReference()).thenReturn("http://example.org/ValueSet/foo");

        // proposed UsageContext in extension (same as existing)
        UsageContext proposedUc = new UsageContext();
        proposedUc.setCode(new Coding().setCode("priority"));
        proposedUc.setValue(new CodeableConcept().addCoding(new Coding().setCode("routine")));
        Extension ext = new Extension(CRMI_INTENDED_USAGE_CONTEXT_URL, proposedUc);
        when(dependency.getExtension()).thenReturn(List.of(ext));

        var existingAdapter = mock(org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter.class);
        var proposedAdapter = mock(org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter.class);
        when(adapterFactoryMock.createUsageContext(existingUc)).thenReturn(existingAdapter);
        when(adapterFactoryMock.createUsageContext(ext.getValue())).thenReturn(proposedAdapter);

        when(existingAdapter.equalsDeep(proposedAdapter)).thenReturn(true);

        doAnswer(invocation -> {
                    throw new AssertionError("addUseContext should not be called when existing equals proposed");
                })
                .when(vsAdapterMock)
                .addUseContext(any());

        var method = visitor.getClass()
                .getDeclaredMethod(
                        "applyManifestUsageContextsToValueSets",
                        org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter.class,
                        IBaseBundle.class);
        method.setAccessible(true);
        method.invoke(visitor, manifestMock, bundleMock);

        verify(vsAdapterMock, times(0)).addUseContext(any());

        staticBundleHelper.close();
    }

    @Test
    void packageOnly_should_include_only_owned_components() {
        // Create a library with owned and non-owned dependencies
        Library library = new Library();
        library.setId("test-library");
        library.setUrl("http://example.org/Library/test");
        library.setVersion("1.0.0");
        library.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        // Add an owned component (composed-of with artifact-isOwned extension)
        org.hl7.fhir.r4.model.RelatedArtifact ownedComponent = new org.hl7.fhir.r4.model.RelatedArtifact();
        ownedComponent.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
        ownedComponent.setResource("http://example.org/Library/owned-component");
        ownedComponent.addExtension(
                "http://hl7.org/fhir/StructureDefinition/artifact-isOwned",
                new org.hl7.fhir.r4.model.BooleanType(true));
        library.addRelatedArtifact(ownedComponent);

        // Add a dependency (depends-on, should be excluded with packageOnly)
        org.hl7.fhir.r4.model.RelatedArtifact dependency = new org.hl7.fhir.r4.model.RelatedArtifact();
        dependency.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        dependency.setResource("http://example.org/ValueSet/dependency");
        library.addRelatedArtifact(dependency);

        // Create owned component library
        Library ownedLib = new Library();
        ownedLib.setId("owned-component");
        ownedLib.setUrl("http://example.org/Library/owned-component");
        ownedLib.setVersion("1.0.0");
        ownedLib.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        // Create dependency ValueSet
        ValueSet dependencyVs = new ValueSet();
        dependencyVs.setId("dependency");
        dependencyVs.setUrl("http://example.org/ValueSet/dependency");
        dependencyVs.setVersion("1.0.0");
        dependencyVs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        repo.create(library);
        repo.create(ownedLib);
        repo.create(dependencyVs);

        PackageVisitor packageVisitor = new PackageVisitor(repo);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(part("packageOnly", new org.hl7.fhir.r4.model.BooleanType(true)));

        Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);

        assertNotNull(packagedBundle);
        // Should include: root library + owned component only (not the dependency)
        assertEquals(2, packagedBundle.getEntry().size());

        // Verify root library is included
        assertTrue(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof Library
                        && ((Library) e.getResource()).getUrl().equals("http://example.org/Library/test")));

        // Verify owned component is included
        assertTrue(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof Library
                        && ((Library) e.getResource()).getUrl().equals("http://example.org/Library/owned-component")));

        // Verify dependency is NOT included
        assertFalse(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof ValueSet
                        && ((ValueSet) e.getResource()).getUrl().equals("http://example.org/ValueSet/dependency")));
    }

    @Test
    void excludePackageId_should_exclude_dependencies_from_specified_packages() {
        // Create a library with dependencies from different packages
        Library library = new Library();
        library.setId("test-library");
        library.setUrl("http://example.org/Library/test");
        library.setVersion("1.0.0");
        library.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        org.hl7.fhir.r4.model.RelatedArtifact dep1 = new org.hl7.fhir.r4.model.RelatedArtifact();
        dep1.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        dep1.setResource("http://hl7.org/fhir/ValueSet/core-vs");
        library.addRelatedArtifact(dep1);

        org.hl7.fhir.r4.model.RelatedArtifact dep2 = new org.hl7.fhir.r4.model.RelatedArtifact();
        dep2.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        dep2.setResource("http://example.org/ValueSet/custom-vs");
        library.addRelatedArtifact(dep2);

        // Create ValueSet from FHIR core with package-source extension
        ValueSet coreVs = new ValueSet();
        coreVs.setId("core-vs");
        coreVs.setUrl("http://hl7.org/fhir/ValueSet/core-vs");
        coreVs.setVersion("1.0.0");
        coreVs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
        coreVs.addExtension(Constants.PACKAGE_SOURCE, new StringType("hl7.fhir.r4.core#4.0.1"));

        // Create custom ValueSet without package-source (should be included)
        ValueSet customVs = new ValueSet();
        customVs.setId("custom-vs");
        customVs.setUrl("http://example.org/ValueSet/custom-vs");
        customVs.setVersion("1.0.0");
        customVs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        repo.create(library);
        repo.create(coreVs);
        repo.create(customVs);

        PackageVisitor packageVisitor = new PackageVisitor(repo);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(part("excludePackageId", "hl7.fhir.r4.core"));

        Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);

        assertNotNull(packagedBundle);

        // Verify core ValueSet is NOT included
        assertFalse(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof ValueSet
                        && ((ValueSet) e.getResource()).getUrl().equals("http://hl7.org/fhir/ValueSet/core-vs")));

        // Verify custom ValueSet IS included (no package-source, so not excluded)
        assertTrue(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof ValueSet
                        && ((ValueSet) e.getResource()).getUrl().equals("http://example.org/ValueSet/custom-vs")));
    }

    @Test
    void include_key_should_filter_to_key_dependencies_only() {
        // Create a library with relatedArtifacts annotated with crmi-dependencyRole
        Library library = new Library();
        library.setId("test-library");
        library.setUrl("http://example.org/Library/test");
        library.setVersion("1.0.0");
        library.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        // Add key dependency
        org.hl7.fhir.r4.model.RelatedArtifact keyDep = new org.hl7.fhir.r4.model.RelatedArtifact();
        keyDep.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        keyDep.setResource("http://example.org/ValueSet/key-vs");
        keyDep.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r4.model.CodeType("key"));
        library.addRelatedArtifact(keyDep);

        // Add default dependency
        org.hl7.fhir.r4.model.RelatedArtifact defaultDep = new org.hl7.fhir.r4.model.RelatedArtifact();
        defaultDep.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        defaultDep.setResource("http://example.org/ValueSet/default-vs");
        defaultDep.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r4.model.CodeType("default"));
        library.addRelatedArtifact(defaultDep);

        // Create ValueSets
        ValueSet keyVs = new ValueSet();
        keyVs.setId("key-vs");
        keyVs.setUrl("http://example.org/ValueSet/key-vs");
        keyVs.setVersion("1.0.0");
        keyVs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        ValueSet defaultVs = new ValueSet();
        defaultVs.setId("default-vs");
        defaultVs.setUrl("http://example.org/ValueSet/default-vs");
        defaultVs.setVersion("1.0.0");
        defaultVs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        repo.create(library);
        repo.create(keyVs);
        repo.create(defaultVs);

        PackageVisitor packageVisitor = new PackageVisitor(repo);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(part("include", "key"));

        Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);

        assertNotNull(packagedBundle);

        // Verify key ValueSet IS included
        assertTrue(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof ValueSet
                        && ((ValueSet) e.getResource()).getUrl().equals("http://example.org/ValueSet/key-vs")));

        // Verify default ValueSet is NOT included
        assertFalse(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof ValueSet
                        && ((ValueSet) e.getResource()).getUrl().equals("http://example.org/ValueSet/default-vs")));
    }

    @Test
    void exclude_test_should_filter_out_test_dependencies() {
        // Create a library with test and non-test dependencies
        Library library = new Library();
        library.setId("test-library");
        library.setUrl("http://example.org/Library/test");
        library.setVersion("1.0.0");
        library.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        // Add test dependency
        org.hl7.fhir.r4.model.RelatedArtifact testDep = new org.hl7.fhir.r4.model.RelatedArtifact();
        testDep.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        testDep.setResource("http://example.org/Library/test-cases");
        testDep.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r4.model.CodeType("test"));
        library.addRelatedArtifact(testDep);

        // Add default dependency
        org.hl7.fhir.r4.model.RelatedArtifact defaultDep = new org.hl7.fhir.r4.model.RelatedArtifact();
        defaultDep.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        defaultDep.setResource("http://example.org/ValueSet/production-vs");
        defaultDep.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r4.model.CodeType("default"));
        library.addRelatedArtifact(defaultDep);

        // Create libraries/valuesets
        Library testLib = new Library();
        testLib.setId("test-cases");
        testLib.setUrl("http://example.org/Library/test-cases");
        testLib.setVersion("1.0.0");
        testLib.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        ValueSet productionVs = new ValueSet();
        productionVs.setId("production-vs");
        productionVs.setUrl("http://example.org/ValueSet/production-vs");
        productionVs.setVersion("1.0.0");
        productionVs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

        repo.create(library);
        repo.create(testLib);
        repo.create(productionVs);

        PackageVisitor packageVisitor = new PackageVisitor(repo);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(part("exclude", "test"));

        Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);

        assertNotNull(packagedBundle);

        // Verify test library is NOT included
        assertFalse(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof Library
                        && ((Library) e.getResource()).getUrl().equals("http://example.org/Library/test-cases")));

        // Verify production ValueSet IS included
        assertTrue(packagedBundle.getEntry().stream()
                .anyMatch(e -> e.getResource() instanceof ValueSet
                        && ((ValueSet) e.getResource()).getUrl().equals("http://example.org/ValueSet/production-vs")));
    }

    @Test
    void packageOperation_should_report_version_mismatch_and_exclude_resource() {
        // Create a ValueSet with version "1.0.0"
        ValueSet valueSet = new ValueSet();
        valueSet.setId("test-valueset");
        valueSet.setUrl("http://example.org/fhir/ValueSet/test-valueset");
        valueSet.setVersion("1.0.0");
        valueSet.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
        valueSet.setName("TestValueSet");

        // Create a Library that depends on version "2.0.0" of the same ValueSet
        Library library = new Library();
        library.setId("test-library");
        library.setUrl("http://example.org/fhir/Library/test-library");
        library.setVersion("1.0.0");
        library.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
        library.setName("TestLibrary");
        library.setType(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                        .setCode("asset-collection")));

        // Add a dependency on the ValueSet with the wrong version (2.0.0 instead of 1.0.0)
        library.addRelatedArtifact()
                .setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/fhir/ValueSet/test-valueset|2.0.0");

        // Store resources in repository
        repo.create(valueSet);
        repo.create(library);

        // Run package operation
        PackageVisitor packageVisitor = new PackageVisitor(repo);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();

        Bundle packagedBundle = (Bundle) libraryAdapter.accept(packageVisitor, params);

        // Verify the bundle was created
        assertNotNull(packagedBundle);

        // Verify the ValueSet is NOT in the packaged bundle (only the Library should be there)
        long valueSetCount = packagedBundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.ValueSet)
                .count();
        assertEquals(0, valueSetCount, "Expected ValueSet with wrong version to be excluded from package");

        // Get the Library from the bundle (it has the messages, not the original libraryAdapter)
        Library bundledLibrary = packagedBundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.Library)
                .map(entry -> (Library) entry.getResource())
                .findFirst()
                .orElse(null);

        assertNotNull(bundledLibrary, "Expected Library to be in the packaged bundle");

        // Create an adapter from the bundled library to check for messages
        ILibraryAdapter bundledAdapter = new AdapterFactory().createLibrary(bundledLibrary);

        // Verify that an error message was added to the OperationOutcome
        assertTrue(
                bundledAdapter.hasExtension(ILibraryAdapter.CQF_MESSAGES_EXT_URL),
                "Expected manifest adapter to have cqf-messages extension for version mismatch");
        assertTrue(
                bundledAdapter.hasContained(),
                "Expected manifest adapter to contain an OperationOutcome for version mismatch");

        var oo = (OperationOutcome) bundledAdapter.getContained().get(0);
        String diagnostics = oo.getIssueFirstRep().getDiagnostics();

        assertTrue(
                diagnostics.contains("Requested version '2.0.0'"),
                "Expected error message to mention requested version 2.0.0");
        assertTrue(
                diagnostics.contains("http://example.org/fhir/ValueSet/test-valueset"),
                "Expected error message to mention the ValueSet URL");
        assertTrue(
                diagnostics.contains("not found in repository"),
                "Expected error message to indicate version not found");
        assertTrue(
                diagnostics.contains("will not be included in package"),
                "Expected error message to indicate resource exclusion");

        // Verify the error severity is "error"
        assertEquals(
                OperationOutcome.IssueSeverity.ERROR,
                oo.getIssueFirstRep().getSeverity(),
                "Expected error severity for version mismatch");
    }

    /**
     * Decode a cv-encoded canonical id back into the original canonical|version identity string.
     * This reverses the mapping performed in PackageVisitor.encodeToIdSafeBase64:
     *   - replace '-' with '+' and '.' with '/'
     *   - restore Base64 padding
     *   - Base64-decode to a UTF-8 string
     */
    private static String decodeCanonicalFromCvId(String id) {
        String body = id.substring(3); // strip "cv-"
        String base64 = body.replace('-', '+').replace('.', '/');
        int mod = base64.length() % 4;
        if (mod != 0) {
            base64 = base64 + "====".substring(mod);
        }
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
