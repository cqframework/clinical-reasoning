package org.opencds.cqf.fhir.cr.hapi.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.google.common.collect.ImmutableMultimap;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// Admittedly, these aren't fantastic tests, but they prove we don't lose the _count parameter
class ClinicalIntelligenceHapiFhirRepositoryTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();

    @Mock
    private DaoRegistry daoRegistry;

    @Mock
    private IFhirResourceDao<Claim> claimDao;

    @Mock
    private RestfulServer restfulServer;

    @Mock
    private IBundleProvider bundleProvider;

    private AutoCloseable mocksCloseable;

    @BeforeEach
    void beforeEach() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        // This ensures we're able to capture the _count parameter in the verify call to getResources()
        when(bundleProvider.getCurrentPageOffset()).thenReturn(null);
        when(claimDao.search(any(), any())).thenReturn(bundleProvider);
        when(daoRegistry.getFhirContext()).thenReturn(FHIR_CONTEXT);
        when(daoRegistry.getResourceDao(Claim.class)).thenReturn(claimDao);
        when(restfulServer.getFhirContext()).thenReturn(FHIR_CONTEXT);
    }

    @AfterEach
    void afterEach() throws Exception {
        mocksCloseable.close();
    }

    @Test
    void ensureCountParameterIsPreservedInSearch() {
        int countParameter = 12345; // arbitrarily high number to set and assert on
        var requestDetails = new SystemRequestDetails();

        requestDetails.addParameter(Constants.PARAM_COUNT, new String[] {Integer.toString(countParameter)});

        var testSubject = setupRepo(requestDetails);

        assertNotNull(testSubject.search(Bundle.class, Claim.class, ImmutableMultimap.of(), Map.of()));

        verify(bundleProvider, times(1)).getResources(0, countParameter);
    }

    @Test
    void ensureMissingCountParameterDoesNotCauseProblems() {
        var requestDetails = new SystemRequestDetails();

        var testSubject = setupRepo(requestDetails);

        assertNotNull(testSubject.search(Bundle.class, Claim.class, ImmutableMultimap.of(), Map.of()));

        // In this case, the bundleProvider will return an empty Collection, and not attempt to trigger getResources()
        // with any count
        verify(bundleProvider, times(0)).getResources(anyInt(), anyInt());
    }

    private ClinicalIntelligenceHapiFhirRepository setupRepo(RequestDetails requestDetails) {
        return new ClinicalIntelligenceHapiFhirRepository(daoRegistry, requestDetails, restfulServer);
    }
}
