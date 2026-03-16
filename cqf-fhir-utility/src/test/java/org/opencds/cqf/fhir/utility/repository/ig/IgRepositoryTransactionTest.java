package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.SearchParameter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.test.Resources;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.search.Searches.SearchBuilder;

class IgRepositoryTransactionTest {

    private static final Map<String, String> HEADERS_EMPTY = Collections.emptyMap();

    private static IgRepository repository;

    @TempDir
    static Path tempDir;

    record ResourceFixture(
            String label, Class<? extends IBaseResource> resourceClass, String resourceType, String urlPrefix) {

        Resource create(String id, String urlSuffix) {
            return switch (label) {
                case "Library" -> {
                    var lib = new Library();
                    lib.setId(id);
                    lib.setUrl(urlPrefix + urlSuffix);
                    yield lib;
                }
                case "SearchParameter" -> {
                    var sp = newSearchParameter();
                    sp.setId(id);
                    sp.setUrl(urlPrefix + urlSuffix);
                    yield sp;
                }
                default -> throw new IllegalArgumentException("Unknown fixture: " + label);
            };
        }

        Resource createWithoutId(String urlSuffix) {
            return switch (label) {
                case "Library" -> {
                    var lib = new Library();
                    lib.setUrl(urlPrefix + urlSuffix);
                    yield lib;
                }
                case "SearchParameter" -> {
                    var sp = newSearchParameter();
                    sp.setUrl(urlPrefix + urlSuffix);
                    yield sp;
                }
                default -> throw new IllegalArgumentException("Unknown fixture: " + label);
            };
        }

        @Override
        @Nonnull
        public String toString() {
            return label;
        }
    }

    private static final ResourceFixture LIBRARY_FIXTURE =
            new ResourceFixture("Library", Library.class, "Library", "http://example.com/Library/");

    private static final ResourceFixture SEARCH_PARAMETER_FIXTURE = new ResourceFixture(
            "SearchParameter", SearchParameter.class, "SearchParameter", "http://example.com/SearchParameter/");

    static Stream<ResourceFixture> resourceFixtures() {
        return Stream.of(LIBRARY_FIXTURE, SEARCH_PARAMETER_FIXTURE);
    }

    private static SearchParameter newSearchParameter() {
        return new SearchParameter()
                .setCode("test-code")
                .setType(Enumerations.SearchParamType.REFERENCE)
                .addBase("MeasureReport")
                .setStatus(Enumerations.PublicationStatus.ACTIVE);
    }

    @BeforeAll
    static void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        Resources.copyFromJar("/sampleIgs/flat", tempDir);
        repository = new IgRepository(FhirContext.forR4Cached(), tempDir);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceFixtures")
    void transactionPost_createsResourceVisibleInReadAndSearch(ResourceFixture fixture) {
        var id = "tx-post-" + fixture.label;
        var resource = fixture.create(id, id);

        var txBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        txBundle.addEntry()
                .setResource(resource)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(fixture.resourceType);

        var result = repository.transaction(txBundle, Collections.emptyMap());
        assertNotNull(result);
        assertEquals(1, result.getEntry().size());

        var read = repository.read(fixture.resourceClass, Ids.newId(fixture.resourceClass, id));
        assertEquals(id, read.getIdElement().getIdPart());

        var searchResult = repository.search(
                Bundle.class,
                fixture.resourceClass,
                new SearchBuilder().withUriParam("url", fixture.urlPrefix + id).build());
        assertEquals(1, searchResult.getEntry().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceFixtures")
    void transactionPost_assignsIdWhenMissing(ResourceFixture fixture) {
        var resource = fixture.createWithoutId("tx-post-no-id-" + fixture.label);

        var txBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        txBundle.addEntry()
                .setResource(resource)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(fixture.resourceType);

        repository.transaction(txBundle, Collections.emptyMap());

        assertTrue(resource.getIdElement().hasIdPart());
        var read = repository.read(fixture.resourceClass, resource.getIdElement());
        assertNotNull(read);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceFixtures")
    void transactionPut_updatesResourceVisibleInReadAndSearch(ResourceFixture fixture) {
        var id = "tx-put-" + fixture.label;
        var resource = fixture.create(id, id);

        var txBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        txBundle.addEntry()
                .setResource(resource)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(fixture.resourceType + "/" + id);

        var result = repository.transaction(txBundle, Collections.emptyMap());
        assertNotNull(result);
        assertEquals(1, result.getEntry().size());

        var read = repository.read(fixture.resourceClass, Ids.newId(fixture.resourceClass, id));
        assertEquals(id, read.getIdElement().getIdPart());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceFixtures")
    void transactionPut_overwritesExistingResource(ResourceFixture fixture) {
        var id = "tx-put-overwrite-" + fixture.label;
        var resource = fixture.create(id, id + "-v1");

        var postBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        postBundle
                .addEntry()
                .setResource(resource)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(fixture.resourceType);
        repository.transaction(postBundle, Collections.emptyMap());

        var updated = fixture.create(id, id + "-v2");

        var putBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        putBundle
                .addEntry()
                .setResource(updated)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(fixture.resourceType + "/" + id);
        repository.transaction(putBundle, Collections.emptyMap());

        var searchV1 = repository.search(
                Bundle.class,
                fixture.resourceClass,
                new SearchBuilder()
                        .withUriParam("url", fixture.urlPrefix + id + "-v1")
                        .build());
        assertEquals(0, searchV1.getEntry().size());

        var searchV2 = repository.search(
                Bundle.class,
                fixture.resourceClass,
                new SearchBuilder()
                        .withUriParam("url", fixture.urlPrefix + id + "-v2")
                        .build());
        assertEquals(1, searchV2.getEntry().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceFixtures")
    void transactionDelete_removesResource(ResourceFixture fixture) {
        var id = "tx-delete-" + fixture.label;
        var resource = fixture.create(id, id);

        var postBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        postBundle
                .addEntry()
                .setResource(resource)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(fixture.resourceType);
        repository.transaction(postBundle, Collections.emptyMap());

        var read = repository.read(fixture.resourceClass, Ids.newId(fixture.resourceClass, id));
        assertNotNull(read);

        var deleteBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        deleteBundle.addEntry().getRequest().setMethod(Bundle.HTTPVerb.DELETE).setUrl(fixture.resourceType + "/" + id);

        var result = repository.transaction(deleteBundle, Collections.emptyMap());
        assertNotNull(result);
        assertEquals(1, result.getEntry().size());

        var searchResult = repository.search(
                Bundle.class,
                fixture.resourceClass,
                new SearchBuilder().withUriParam("url", fixture.urlPrefix + id).build());
        assertEquals(0, searchResult.getEntry().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceFixtures")
    void transactionDelete_throwsWhenNotFound(ResourceFixture fixture) {
        var deleteBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        deleteBundle
                .addEntry()
                .getRequest()
                .setMethod(Bundle.HTTPVerb.DELETE)
                .setUrl(fixture.resourceType + "/does-not-exist-" + fixture.label);

        assertThrows(ResourceNotFoundException.class, () -> repository.transaction(deleteBundle, HEADERS_EMPTY));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resourceFixtures")
    void transactionMixed_postThenPutThenDelete(ResourceFixture fixture) {
        var suffix = fixture.label;
        var res1 = fixture.create("tx-mixed-1-" + suffix, "tx-mixed-1-" + suffix);
        var res2 = fixture.create("tx-mixed-2-" + suffix, "tx-mixed-2-" + suffix);

        var postBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        postBundle
                .addEntry()
                .setResource(res1)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(fixture.resourceType);
        postBundle
                .addEntry()
                .setResource(res2)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(fixture.resourceType);
        repository.transaction(postBundle, Collections.emptyMap());

        var res1Updated = fixture.create("tx-mixed-1-" + suffix, "tx-mixed-1-" + suffix + "-updated");
        var res3 = fixture.create("tx-mixed-3-" + suffix, "tx-mixed-3-" + suffix);

        var mixedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        mixedBundle
                .addEntry()
                .setResource(res1Updated)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(fixture.resourceType + "/tx-mixed-1-" + suffix);
        mixedBundle
                .addEntry()
                .getRequest()
                .setMethod(Bundle.HTTPVerb.DELETE)
                .setUrl(fixture.resourceType + "/tx-mixed-2-" + suffix);
        mixedBundle
                .addEntry()
                .setResource(res3)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(fixture.resourceType);

        var result = repository.transaction(mixedBundle, Collections.emptyMap());
        assertEquals(3, result.getEntry().size());

        var readRes1 = repository.read(fixture.resourceClass, Ids.newId(fixture.resourceClass, "tx-mixed-1-" + suffix));
        assertNotNull(readRes1);

        var searchRes1 = repository.search(
                Bundle.class,
                fixture.resourceClass,
                new SearchBuilder()
                        .withUriParam("url", fixture.urlPrefix + "tx-mixed-1-" + suffix + "-updated")
                        .build());
        assertEquals(1, searchRes1.getEntry().size());

        var searchRes2 = repository.search(
                Bundle.class,
                fixture.resourceClass,
                new SearchBuilder()
                        .withUriParam("url", fixture.urlPrefix + "tx-mixed-2-" + suffix)
                        .build());
        assertEquals(0, searchRes2.getEntry().size());

        var readRes3 = repository.read(fixture.resourceClass, Ids.newId(fixture.resourceClass, "tx-mixed-3-" + suffix));
        assertNotNull(readRes3);
    }

    @Test
    void transactionUnsupportedMethod_throws() {
        var txBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        txBundle.addEntry().getRequest().setMethod(Bundle.HTTPVerb.GET).setUrl("Library/123");

        assertThrows(NotImplementedOperationException.class, () -> repository.transaction(txBundle, HEADERS_EMPTY));
    }
}
