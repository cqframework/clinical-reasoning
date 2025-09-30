package org.opencds.cqf.fhir.cr.hapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.repository.HapiFhirRepository;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import com.google.common.collect.ImmutableMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Claim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// Admittedly, these aren't fantastic tests, but they prove we don't lose the _count parameter
class ClinicalIntelligenceHapiFhirRepositoryTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 10;

    @Mock
    private DaoRegistry daoRegistry;

    @Mock
    private IFhirResourceDao<Claim> claimDao;

    private AutoCloseable mocksCloseable;

    @BeforeEach
    void beforeEach() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void afterEach() throws Exception {
        mocksCloseable.close();
    }

    @Test
    void testLessResourcesThanDefaultNullCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 4, 4);
    }

    @Test
    void testMoreResourcesThanDefaultNullCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 11, DEFAULT_PAGE_SIZE);
    }

    @Test
    void testSameResourcesAsDefaultNullCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 5, DEFAULT_PAGE_SIZE);
    }

    @Test
    void testLessResourcesThanDefaultHighCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, 50, 4, 4);
    }

    @Test
    void testMoreResourcesThanDefaultHighCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, 50, 11, 11);
    }

    @Test
    void testSameResourcesAsDefaultHighCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, 50, 5, 5);
    }

    @Test
    void testLessResourcesThanDefaultLowCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, 2, 4, 4);
    }

    @Test
    void testMoreResourcesThanDefaultLowCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, 2, 11, MAX_PAGE_SIZE);
    }

    @Test
    void testSameResourcesAsDefaultLowCountParam() {
        triggerTestCase(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, 2, 5, 5);
    }

    @Test
    void testLessResourcesThanHighDefaultLowCountParam() {
        triggerTestCase(50, 100, 2, 4, 4);
    }

    @Test
    void testMoreResourcesThanHighDefaultLowCountParam() {
        triggerTestCase(50, 100, 2, 11, 11);
    }

    @Test
    void testSameResourcesAsHighDefaultLowCountParam() {
        triggerTestCase(50, 100, 5, 100, 100);
    }

    @Test
    void testLessResourcesThanHighDefaultNullCountParam() {
        triggerTestCase(50, 100, null, 4, 4);
    }

    @Test
    void testMoreResourcesThanHighDefaultNullCountParam() {
        triggerTestCase(50, 100, null, 11, 11);
    }

    @Test
    void testSameResourcesAsHighDefaultNullCountParam() {
        triggerTestCase(50, 100, null, 100, 50);
    }

    @ParameterizedTest
    @MethodSource("bundleProviders")
    void testSanitizeBundleProvider(IBundleProvider bundleProvider) {
        var testSubject = getTestSubject(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 0);

        IBundleProvider result = testSubject.sanitizeBundleProvider(bundleProvider);
        assertNotNull(result);
    }

    @Test
    void testExtractIncludesFromRequestParameters_NoIncludes() {
        var testSubject = getTestSubject(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 0);

        Map<String, String[]> parameters = createRequestParametersWithIncludes(null);
        Set<Include> includes = testSubject.extractIncludesFromRequestParameters(parameters);

        assertNotNull(includes);
        assertEquals(0, includes.size());
    }

    @Test
    void testExtractIncludesFromRequestParameters_SingleInclude() {
        var testSubject = getTestSubject(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 0);

        Map<String, String[]> parameters = createRequestParametersWithIncludes(new String[] {"Patient:name"});
        Set<Include> includes = testSubject.extractIncludesFromRequestParameters(parameters);

        assertNotNull(includes);
        assertEquals(1, includes.size());
        assertEquals("Patient:name", includes.iterator().next().getValue());
    }

    @Test
    void testExtractIncludesFromRequestParameters_MultipleIncludes() {
        var testSubject = getTestSubject(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 0);

        Map<String, String[]> parameters =
                createRequestParametersWithIncludes(new String[] {"Patient:name", "Observation:subject"});
        Set<Include> includes = testSubject.extractIncludesFromRequestParameters(parameters);

        assertNotNull(includes);
        assertEquals(2, includes.size());
        assertTrue(includes.stream().anyMatch(include -> include.getValue().equals("Patient:name")));
        Assertions.assertTrue(
                includes.stream().anyMatch(include -> include.getValue().equals("Observation:subject")));
    }

    @Test
    void testExtractBundleTypeFromRequestParameters_Default() {
        var testSubject = getTestSubject(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 0);

        Map<String, String[]> parameters = createRequestParametersWithBundleType(null);
        BundleTypeEnum bundleType = testSubject.extractBundleTypeFromRequestParameters(parameters);

        assertNotNull(bundleType);
        assertEquals(BundleTypeEnum.SEARCHSET, bundleType, "Default bundle type should be SEARCHSET");
    }

    @ParameterizedTest
    @EnumSource(BundleTypeEnum.class)
    void testExtractBundleTypeFromRequestParameters_withExplicitValue(BundleTypeEnum bundleType) {
        var testSubject = getTestSubject(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 0);

        String typeStr = bundleType.getCode().toLowerCase();
        Map<String, String[]> parameters = createRequestParametersWithBundleType(typeStr);
        BundleTypeEnum extractedBundleType = testSubject.extractBundleTypeFromRequestParameters(parameters);

        assertNotNull(extractedBundleType);
        assertEquals(bundleType, extractedBundleType, "Bundle type should match explicitly provided value");
    }

    @Test
    void testExtractBundleTypeFromRequestParameters_InvalidValue() {
        var testSubject = getTestSubject(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, null, 0);

        Map<String, String[]> parameters = createRequestParametersWithBundleType("invalid");
        BundleTypeEnum bundleType = testSubject.extractBundleTypeFromRequestParameters(parameters);

        assertNotNull(bundleType);
        assertEquals(BundleTypeEnum.SEARCHSET, bundleType, "Default bundle type should be SEARCHSET");
    }

    private Map<String, String[]> createRequestParametersWithIncludes(String[] includes) {
        HashMap<String, String[]> retVal = new HashMap<>();
        retVal.put(Constants.PARAM_INCLUDE, includes);
        return retVal;
    }

    private Map<String, String[]> createRequestParametersWithBundleType(String bundleType) {
        HashMap<String, String[]> retVal = new HashMap<>();
        retVal.put(Constants.PARAM_BUNDLETYPE, new String[] {bundleType});
        return retVal;
    }

    private static Stream<Arguments> bundleProviders() {
        return Stream.of(Arguments.of((IBundleProvider) null), Arguments.of(new SimpleBundleProvider()));
    }

    private void triggerTestCase(
            int defaultPageSize,
            int maxPageSize,
            @Nullable Integer countParameter,
            int numResourcesInDatabase,
            int expectedNumberOfQueriedResources) {

        var testSubject = getTestSubject(defaultPageSize, maxPageSize, countParameter, numResourcesInDatabase);

        var searchedForResources = searchForResources(testSubject);

        assertEquals(expectedNumberOfQueriedResources, searchedForResources.size());
    }

    @Nonnull
    private List<Claim> searchForResources(HapiFhirRepository testSubject) {
        var searchResult = testSubject.search(Bundle.class, Claim.class, ImmutableMultimap.of(), Map.of());

        assertNotNull(searchResult);

        return searchResult.getEntry().stream()
                .map(BundleEntryComponent::getResource)
                .filter(Claim.class::isInstance)
                .map(Claim.class::cast)
                .toList();
    }

    @Nonnull
    private ClinicalIntelligenceHapiFhirRepository getTestSubject(
            int defaultPageSize, int maximumPageSize, @Nullable Integer countParameter, int numResourcesInDatabase) {

        return new ClinicalIntelligenceHapiFhirRepository(
                mockDaoRegistry(numResourcesInDatabase),
                setupRequestDetails(countParameter),
                setupRestfulServer(defaultPageSize, maximumPageSize));
    }

    private DaoRegistry mockDaoRegistry(int numResourcesInDatabase) {
        var bundleProvider = new SimpleBundleProvider(getClaims(numResourcesInDatabase));

        when(claimDao.search(any(), any())).thenReturn(bundleProvider);
        when(daoRegistry.getFhirContext()).thenReturn(FHIR_CONTEXT);
        when(daoRegistry.getResourceDao(Claim.class)).thenReturn(claimDao);

        return daoRegistry;
    }

    @Nonnull
    private SystemRequestDetails setupRequestDetails(@Nullable Integer countParameter) {
        var requestDetails = new SystemRequestDetails();

        Optional.ofNullable(countParameter)
                .ifPresent(count -> requestDetails.addParameter(
                        Constants.PARAM_COUNT, new String[] {Integer.toString(countParameter)}));
        return requestDetails;
    }

    private static RestfulServer setupRestfulServer(int defaultPageSize, int maxPageSize) {
        var restfulServer = new RestfulServer(FHIR_CONTEXT);
        var databaseBackedPagingProvider = new DatabaseBackedPagingProvider();
        databaseBackedPagingProvider.setDefaultPageSize(defaultPageSize);
        databaseBackedPagingProvider.setMaximumPageSize(maxPageSize);
        restfulServer.setPagingProvider(databaseBackedPagingProvider);
        return restfulServer;
    }

    private static List<Claim> getClaims(int numClaimsToGenerate) {
        return IntStream.range(0, numClaimsToGenerate)
                .mapToObj(ClinicalIntelligenceHapiFhirRepositoryTest::getClaim)
                .toList();
    }

    private static Claim getClaim(int id) {
        return (Claim) new Claim().setId("claim_" + id);
    }
}
