package org.opencds.cqf.fhir.cr.hapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;

class DetailsBuilderTest {

    @Test
    void testSetParametersWithValidMap() {
        // Arrange
        RequestDetails systemRequestDetails = new SystemRequestDetails();
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(systemRequestDetails);

        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("param1", new String[] {"value1"});
        detailsBuilder.setParameters(parameters);

        // Act
        RequestDetails requestDetails = detailsBuilder.create();
        Map<String, String[]> clonedParameters = requestDetails.getParameters();

        // Assert
        assertNotNull(clonedParameters);
        assertEquals(1, clonedParameters.size());
        assertTrue(clonedParameters.containsKey("param1"));
        assertEquals(1, clonedParameters.get("param1").length);
        assertEquals("value1", clonedParameters.get("param1")[0]);
    }

    @Test
    void testSetActionWithValidOperationType() {
        // Arrange
        RequestDetails systemRequestDetails = new SystemRequestDetails();
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(systemRequestDetails);
        RestOperationTypeEnum operationType = RestOperationTypeEnum.CREATE;

        detailsBuilder.setAction(operationType);

        // Act
        RequestDetails requestDetails = detailsBuilder.create();

        // Assert
        assertEquals(operationType, requestDetails.getRestOperationType());
    }

    @Test
    void testSetId() {
        // Arrange
        RequestDetails systemRequestDetails = new SystemRequestDetails();
        IIdType mockId = mock(IIdType.class);
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(systemRequestDetails);

        detailsBuilder.setId(mockId);

        // Act
        RequestDetails requestDetails = detailsBuilder.create();

        // Assert
        assertEquals(mockId, requestDetails.getId());
    }

    @Test
    void testAddHeadersWithValidHeaders() {
        // Arrange
        RequestDetails mockRequestDetails = mock(RequestDetails.class);
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(mockRequestDetails);

        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "value1");
        headers.put("header2", "value2");

        // Act
        detailsBuilder.addHeaders(headers);

        // Assert
        verify(mockRequestDetails, times(1)).addHeader("header1", "value1");
        verify(mockRequestDetails, times(1)).addHeader("header2", "value2");
    }

    @Test
    void testAddHeadersWithNullHeaders() {
        // Arrange
        RequestDetails mockRequestDetails = mock(RequestDetails.class);
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(mockRequestDetails);

        // Act
        detailsBuilder.addHeaders(null);

        // Assert
        verifyNoInteractions(mockRequestDetails);
    }

    @Test
    void testAddHeadersWithEmptyHeaders() {
        // Arrange
        RequestDetails mockRequestDetails = mock(RequestDetails.class);
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(mockRequestDetails);

        Map<String, String> headers = new HashMap<>();

        // Act
        detailsBuilder.addHeaders(headers);

        // Assert
        verifyNoInteractions(mockRequestDetails);
    }

    @Test
    void testCreateWithPopulatedDetails() {
        // Arrange
        RequestDetails mockRequestDetails = mock(RequestDetails.class);
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(mockRequestDetails);

        RestOperationTypeEnum operationType = RestOperationTypeEnum.UPDATE;
        String resourceType = "Patient";

        detailsBuilder.setAction(operationType).setResourceType(resourceType);

        // Act
        RequestDetails createdDetails = detailsBuilder.create();

        // Assert
        verify(mockRequestDetails).setRestOperationType(operationType);
        verify(mockRequestDetails).setResourceName(resourceType);
        assertSame(mockRequestDetails, createdDetails);
    }

    @Test
    void testCreateWithEmptyDetails() {
        // Arrange
        RequestDetails mockRequestDetails = mock(RequestDetails.class);
        ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder detailsBuilder =
                new ClinicalIntelligenceRequestDetailsCloner.DetailsBuilder(mockRequestDetails);

        // Act
        RequestDetails createdDetails = detailsBuilder.create();

        // Assert
        assertSame(mockRequestDetails, createdDetails);
    }
}
