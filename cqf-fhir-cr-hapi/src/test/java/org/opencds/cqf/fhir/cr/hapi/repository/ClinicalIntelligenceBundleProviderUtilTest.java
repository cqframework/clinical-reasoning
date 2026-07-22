package org.opencds.cqf.fhir.cr.hapi.repository;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.IRestfulServer;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.util.BundleUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClinicalIntelligenceBundleProviderUtilTest {

    public enum PagingStyle {
        OFFSET,
        PAGE_ID,
        SEARCH_ID
    }

    private static final Logger log = LoggerFactory.getLogger(ClinicalIntelligenceBundleProviderUtilTest.class);
    // fhir context does not matter for this test; but a fhircontext is required
    private final FhirContext context = FhirContext.forR4Cached();

    private RequestDetails reqDetails;

    private IRestfulServer<?> restfulServer;

    private IBundleProvider bundleProvider;

    @BeforeEach
    public void before() {
        reqDetails = new SystemRequestDetails();
        reqDetails.setFhirServerBase("http://localhost:8000/");

        restfulServer = mock(IRestfulServer.class);

        bundleProvider = mock(IBundleProvider.class);

        // probably always needed
        lenient().when(restfulServer.getFhirContext()).thenReturn(context);
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    // limit, linkSelf, offset, bundleType, searchId, offset source
                    """
        null, null, TRANSACTION, null
        null, null, TRANSACTION, searchID
        null, http://root.com/self, TRANSACTION, null
        2, null, TRANSACTION, null
        2, http://root.com/self, TRANSACTION, null
        2, null, TRANSACTION, searchID
        null, http://root.com/self, TRANSACTION, searchID
        2, http://root.com.self, TRANSACTION, searchID
        """,
            nullValues = "null")
    public void createBundleFromBundleProvider_basicParametersCoverageTest(
            Integer limit, String linkSelf, BundleTypeEnum bundleType, String searchId) {
        // setup
        int pagesize = 2;
        int offset = 0;
        int count = 3;
        List<Library> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library library = new Library();
            library.setId("Library/123");

            libraries.add(library);
        }
        bundleProvider = new SimpleBundleProvider(libraries);

        IPagingProvider pagingProvider = mock(IPagingProvider.class);

        // when
        when(restfulServer.getDefaultPageSize()).thenReturn(pagesize);
        when(restfulServer.getPagingProvider()).thenReturn(pagingProvider);

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer, reqDetails, limit, linkSelf, Set.of(), bundleProvider, offset, bundleType, searchId);

        // verify
        assertNotNull(resource);
        assertInstanceOf(Bundle.class, resource);
        Bundle bundle = (Bundle) resource;
        List<IBaseResource> resources = BundleUtil.toListOfResources(context, bundle);

        if (limit != null && !isEmpty(searchId)) {
            assertNotNull(bundle.getLink("next"));
        }

        if (limit == null) {
            assertEquals(pagesize, resources.size());
        } else {
            assertEquals(limit, resources.size());
        }

        // if there's a self link, it should be provided
        if (!isEmpty(linkSelf)) {
            assertNotNull(bundle.getLink("self"));
        }
    }

    @Test
    public void createBundleFromBundleProvider_nullEntries_coverageTest() {
        // setup
        int count = 2;
        List<Library> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library library = new Library();
            library.setId("Library/123");

            libraries.add(library);
        }
        libraries.add(null);
        bundleProvider = new SimpleBundleProvider(libraries);

        ((SimpleBundleProvider) bundleProvider).setCurrentPageOffset(0);
        ((SimpleBundleProvider) bundleProvider).setCurrentPageSize(count + 1); // +1 to get that null entry

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer,
                reqDetails,
                null, // limit
                null, // link self
                Set.of(), // includes
                bundleProvider,
                0, // offset
                BundleTypeEnum.TRANSACTION,
                null // searchId
                );

        // validate
        assertNotNull(resource);
        assertInstanceOf(Bundle.class, resource); // because we're using R4
        Bundle bundle = (Bundle) resource;
        // no null value
        assertEquals(count, bundle.getTotal());
    }

    @ParameterizedTest
    @EnumSource(value = BundleTypeEnum.class)
    public void createBundleFromBundleProvider_allBundleTypes_coverageTest(BundleTypeEnum bundleType) {
        // setup
        int count = 3;
        List<Library> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library lib = new Library();
            lib.setId("Library/1");

            libraries.add(lib);
        }

        bundleProvider = new SimpleBundleProvider(libraries);

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer,
                reqDetails,
                null, // limit
                null, // link self
                Set.of(), // includes
                bundleProvider,
                0, // offset
                bundleType,
                null // searchId
                );

        // validate
        assertNotNull(resource);
        assertInstanceOf(Bundle.class, resource); // because we're using R4
        Bundle bundle = (Bundle) resource;
        assertEquals(count, bundle.getTotal());
        assertEquals(bundleType.getCode(), bundle.getType().toCode());
    }

    @ParameterizedTest
    @ValueSource(ints = 0)
    @NullSource
    public void createBundleFromBundleProvider_emptyResults_coverageTest(Integer size) {
        // setup
        bundleProvider = new SimpleBundleProvider(List.of());

        // some providers return null, some return 0
        ((SimpleBundleProvider) bundleProvider).setSize(size);

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer,
                reqDetails,
                null, // limit
                null, // link self
                Set.of(), // includes
                bundleProvider,
                0, // offset
                BundleTypeEnum.TRANSACTION,
                null // searchId
                );

        // validate
        assertNotNull(resource);
        assertInstanceOf(Bundle.class, resource);
        Bundle bundle = (Bundle) resource;
        assertEquals(0, bundle.getEntry().size());
        assertNull(bundle.getLink("next"));
    }

    @Test
    public void createBundleFromBundleProvider_withNextPageOfset_hasCorrectPageLinkOffsets() {
        // setup
        int offset = 10;
        int limit = 5;
        List<IBaseResource> measures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Measure measure = new Measure();
            measure.setId(new IdType("Measure/" + i));
            measure.setUrl("http://something.com/" + i);
            measure.setName("measure" + i);

            measures.add(measure);
        }

        bundleProvider = mock(IBundleProvider.class);
        when(bundleProvider.getCurrentPageOffset()).thenReturn(offset);
        when(bundleProvider.getCurrentPageSize()).thenReturn(limit);
        when(bundleProvider.getNextPageId()).thenReturn("next");
        when(bundleProvider.getPreviousPageId()).thenReturn("prev");
        when(bundleProvider.getResources(anyInt(), anyInt())).thenReturn(measures);

        // test
        IBaseResource baseResource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer,
                reqDetails,
                null,
                null,
                Set.of(),
                bundleProvider,
                offset,
                BundleTypeEnum.SEARCHSET,
                null);

        // verify
        assertNotNull(baseResource);
        assertInstanceOf(Bundle.class, baseResource);
        Bundle bundle = (Bundle) baseResource;
        BundleLinkComponent nextLink = bundle.getLink("next");
        BundleLinkComponent prevLink = bundle.getLink("previous");
        assertNotNull(nextLink);
        assertNotNull(prevLink);
        // offset should be 15 = offset + limit
        assertTrue(nextLink.getUrl().contains(String.format("%s=%d", Constants.PARAM_OFFSET, 15)), nextLink.getUrl());
        // offset should be 5 = offset - limit
        assertTrue(prevLink.getUrl().contains(String.format("%s=%d", Constants.PARAM_OFFSET, 5)), prevLink.getUrl());
    }

    @ParameterizedTest
    @EnumSource(PagingStyle.class)
    public void createBundleFromBundleProvider_secondPage_coverageTest(PagingStyle style) {
        // setup
        int offset = 1;
        String searchId = null;
        int count = 4;
        List<IBaseResource> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library lib = new Library();
            lib.setId("Library/1");

            libraries.add(lib);
        }

        bundleProvider = mock(IBundleProvider.class);

        // mock
        when(bundleProvider.getNextPageId()).thenReturn("nextId");
        when(bundleProvider.getPreviousPageId()).thenReturn("previousId");
        when(bundleProvider.getResources(anyInt(), anyInt())).thenReturn(libraries);

        switch (style) {
            case OFFSET -> {
                when(bundleProvider.getCurrentPageOffset()).thenReturn(1);
            }
            case PAGE_ID -> {
                IPagingProvider pagingProvider = mock(IPagingProvider.class);
                when(restfulServer.getPagingProvider()).thenReturn(pagingProvider);

                when(bundleProvider.getCurrentPageOffset()).thenReturn(null);
                when(restfulServer.canStoreSearchResults()).thenReturn(true);
                when(bundleProvider.getCurrentPageId()).thenReturn("current");
                when(pagingProvider.getDefaultPageSize()).thenReturn(count); // just to get everything
            }
            case SEARCH_ID -> {
                searchId = "searchId";
                IPagingProvider pagingProvider = mock(IPagingProvider.class);
                when(restfulServer.getPagingProvider()).thenReturn(pagingProvider);

                when(bundleProvider.size()).thenReturn(count + offset + 1); // +1 so there's thought to be a "next"

                when(bundleProvider.getCurrentPageOffset()).thenReturn(null);
                when(restfulServer.canStoreSearchResults()).thenReturn(true);

                when(bundleProvider.getUuid()).thenReturn("SearchIdGuid");
            }
        }

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer,
                reqDetails,
                count, // limit
                null, // link self
                Set.of(), // includes
                bundleProvider,
                offset, // offset
                BundleTypeEnum.TRANSACTION,
                searchId // searchId
                );

        // validate
        assertNotNull(resource);
        assertInstanceOf(Bundle.class, resource);
        Bundle bundle = (Bundle) resource;
        assertEquals(count, bundle.getEntry().size());
        // make sure links exist
        assertNotNull(bundle.getLink("next"));
        assertNotNull(bundle.getLink("previous"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void createBundleFromBundleProvider_pageOffset_coverageTest(int offset) {
        // setup
        Integer pageSize = null;
        int count = 4;
        List<Library> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library lib = new Library();
            lib.setId("Library/1");

            libraries.add(lib);
        }

        bundleProvider = new SimpleBundleProvider(libraries);

        // mock
        when(restfulServer.canStoreSearchResults()).thenReturn(true);
        {
            IPagingProvider provider = mock(IPagingProvider.class);
            when(restfulServer.getPagingProvider()).thenReturn(provider);
            pageSize = 2;
            when(provider.getDefaultPageSize()).thenReturn(pageSize);

            if (offset > 0) {
                SimpleBundleProvider sp = (SimpleBundleProvider) bundleProvider;
                sp.setCurrentPageOffset(offset);
                sp.setCurrentPageSize(pageSize);

                /*
                 * If we provide a default page size and it's not
                 * the first page (ie, offset > 0), then we
                 * will later ignore the pagesize and just return
                 * "everything that was passed back".
                 *
                 * But, we still require the defaultpagesize (even though it'll be ignored)
                 * so it's just the test that needs to ignore this later
                 */
                pageSize = null;
            }
        }

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer,
                reqDetails,
                pageSize, // limit
                null, // link self
                Set.of(), // includes
                bundleProvider,
                offset, // offset
                BundleTypeEnum.TRANSACTION,
                null // searchId
                );

        // validate
        assertNotNull(resource);
        assertInstanceOf(Bundle.class, resource);
        Bundle bundle = (Bundle) resource;
        int usedPageSize = pageSize != null ? pageSize : 0;
        assertEquals(count - usedPageSize, bundle.getEntry().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void createBundleFromBundleProvider_invalidResourceList_coverageTest(boolean hasId) {
        // setup
        int count = 1;
        List<Library> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library lib = new Library();
            if (hasId) {
                // has id == empty id
                lib.setId("");
            }

            libraries.add(lib);
        }

        bundleProvider = new SimpleBundleProvider(libraries);

        // test
        try {
            ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                    restfulServer,
                    reqDetails,
                    count, // limit
                    null, // link self
                    Set.of(), // includes
                    bundleProvider,
                    0, // offset
                    BundleTypeEnum.TRANSACTION,
                    null // searchId
                    );
            fail();
        } catch (InternalErrorException ex) {
            assertTrue(
                    ex.getLocalizedMessage().contains("Server method returned resource of type"),
                    ex.getLocalizedMessage());
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = RestOperationTypeEnum.class,
            names = {"EXTENDED_OPERATION_TYPE", "EXTENDED_OPERATION_INSTANCE"})
    public void createBundleFromBundleProvider_everythingOp_coverageTest(RestOperationTypeEnum opType) {
        // setup
        int count = 1;
        List<Library> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library lib = new Library();
            lib.setId("Library/1");

            libraries.add(lib);
        }

        bundleProvider = new SimpleBundleProvider(libraries);

        reqDetails.setRestOperationType(opType);
        reqDetails.setOperation("$everything");

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer,
                reqDetails,
                count, // limit
                null, // link self
                Set.of(), // includes
                bundleProvider,
                0, // offset
                BundleTypeEnum.TRANSACTION,
                null // searchId
                );

        // validate
        assertNotNull(resource);
        assertInstanceOf(Bundle.class, resource);
        Bundle bundle = (Bundle) resource;
        assertEquals(count, bundle.getEntry().size());
    }
}
