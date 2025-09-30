package org.opencds.cqf.fhir.cr.hapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IRestfulResponse;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRestfulResponse;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.repository.ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder;

class ClinicalIntelligenceRequestDetailsClonerTest {

    @Test
    void testStartWithServletRequestDetails() {
        ServletRequestDetails mockDetails = mock(ServletRequestDetails.class);
        Map<String, String[]> mockParams = new HashMap<>();
        mockParams.put("key1", new String[] {"value1"});
        when(mockDetails.getParameters()).thenReturn(mockParams);

        DetailsBuilder builder = ClinicalIntelligenceRequestDetailsCloner.startWith(mockDetails);
        RequestDetails result = builder.create();

        assertNotNull(result);
        assertEquals(RequestTypeEnum.POST, result.getRequestType());
        assertNull(result.getResource());
        assertNull(result.getOperation());
        assertEquals(mockParams, result.getParameters());
    }

    @Test
    void testStartWithSystemRequestDetails() {
        RequestDetails mockDetails = mock(SystemRequestDetails.class);
        Map<String, String[]> mockParams = new HashMap<>();
        mockParams.put("key2", new String[] {"value2"});
        when(mockDetails.getParameters()).thenReturn(mockParams);

        DetailsBuilder builder = ClinicalIntelligenceRequestDetailsCloner.startWith(mockDetails);
        RequestDetails result = builder.create();

        assertNotNull(result);
        assertEquals(RequestTypeEnum.POST, result.getRequestType());
        assertNull(result.getResource());
        assertNull(result.getOperation());
        assertEquals(mockParams, result.getParameters());
    }

    @Test
    void testStartWithSetsResponseFromOriginalRequest() {
        RequestDetails mockDetails = mock(RequestDetails.class);
        IRestfulResponse expectedResponse = new SystemRestfulResponse(null);
        when(mockDetails.getResponse()).thenReturn(expectedResponse);

        DetailsBuilder builder = ClinicalIntelligenceRequestDetailsCloner.startWith(mockDetails);
        RequestDetails result = builder.create();

        assertNotNull(result);
        assertEquals(expectedResponse, result.getResponse());
    }

    @Test
    void testStartWithResetsResourceNameAndCompartmentName() {
        RequestDetails mockDetails = mock(RequestDetails.class);

        DetailsBuilder builder = ClinicalIntelligenceRequestDetailsCloner.startWith(mockDetails);
        RequestDetails result = builder.create();

        assertNotNull(result);
        assertNull(result.getResourceName());
        assertNull(result.getCompartmentName());
    }

    @Test
    void testStartWithClonesParametersCorrectly() {
        RequestDetails mockDetails = mock(RequestDetails.class);
        Map<String, String[]> originalParams = new HashMap<>();
        originalParams.put("param1", new String[] {"value1", "value2"});
        when(mockDetails.getParameters()).thenReturn(originalParams);

        DetailsBuilder builder = ClinicalIntelligenceRequestDetailsCloner.startWith(mockDetails);
        RequestDetails result = builder.create();

        assertNotNull(result);
        Map<String, String[]> clonedParams = result.getParameters();
        assertNotSame(originalParams, clonedParams);
        assertEquals(originalParams, clonedParams);
    }
}
