package org.opencds.cqf.fhir.cr.hapi.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.IRestfulServer;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClinicalIntelligenceBundleProviderUtilTest {

    // fhir context does not matter for this test; but a fhircontext is required
    private FhirContext context = FhirContext.forR4Cached();

    private RequestDetails reqDetails;

    private IRestfulServer<?> restfulServer;

    private IBundleProvider bundleProvider;

    @BeforeEach
    public void before() {
        reqDetails = new SystemRequestDetails();

        restfulServer = mock(IRestfulServer.class);

        bundleProvider = mock(IBundleProvider.class);
    }

    @ParameterizedTest
    @CsvSource(value =
        // limit, linkSelf, offset, bundleType, searchId
        """
        null, null, TRANSACTION, null
        """, nullValues = "null")
    public void createBundleFromBundleProvider_basicParametersCoverageTest(
        Integer limit,
        String linkSelf,
        BundleTypeEnum bundleType,
        String searchId
    ) {
        // setup
        int offset = 0;
        int count = 3;
        List<Library> libraries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Library library = new Library();
            library.setId("Library/123");

            libraries.add(library);
        }
        bundleProvider = new SimpleBundleProvider(libraries);

        // when
        when(restfulServer.getDefaultPageSize())
            .thenReturn(2);

        // test
        IBaseResource resource = ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
            restfulServer,
            reqDetails,
            limit,
            linkSelf,
            Set.of(),
            bundleProvider,
            offset,
            bundleType,
            searchId
        );

        // verify
        assertNotNull(resource);
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

        bundleProvider = new SimpleBundleProvider(
            libraries
        );

        // mock
        when(restfulServer.getFhirContext())
            .thenReturn(context);

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
        assertTrue(resource instanceof Bundle); // because we're using R4
        Bundle bundle = (Bundle)resource;
        assertEquals(count, bundle.getTotal());
        assertEquals(bundleType.getCode(), bundle.getType().toCode());
    }
}
