package org.opencds.cqf.fhir.cr.hapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IRestfulResponse;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRestfulResponse;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ClinicalIntelligenceRequestDetailsClonerTest {

    record RequestDetailsVariant(String label, RequestDetails requestDetails) {
        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<RequestDetailsVariant> requestDetailsVariants() {
        var system = new SystemRequestDetails();
        system.setParameters(new HashMap<>());

        var servlet = new ServletRequestDetails();
        servlet.setParameters(new HashMap<>());

        return Stream.of(
                new RequestDetailsVariant("SystemRequestDetails", system),
                new RequestDetailsVariant("ServletRequestDetails", servlet));
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_SetsRequestTypeToPost(RequestDetailsVariant variant) {
        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), Measure.class)
                .create();

        assertEquals(RequestTypeEnum.POST, result.getRequestType());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_ResetsOperationResourceAndNames(RequestDetailsVariant variant) {
        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), Measure.class)
                .create();

        assertNull(result.getOperation());
        assertNull(result.getResource());
        assertNull(result.getResourceName());
        assertNull(result.getCompartmentName());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_PreservesTenantId(RequestDetailsVariant variant) {
        variant.requestDetails().setTenantId("test-tenant");

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), Measure.class)
                .create();

        assertEquals("test-tenant", result.getTenantId());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_PreservesResponse(RequestDetailsVariant variant) {
        IRestfulResponse expectedResponse = new SystemRestfulResponse(null);
        variant.requestDetails().setResponse(expectedResponse);

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), Measure.class)
                .create();

        assertEquals(expectedResponse, result.getResponse());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_ClonesParameters(RequestDetailsVariant variant) {
        Map<String, String[]> originalParams = new HashMap<>();
        originalParams.put("param1", new String[] {"value1", "value2"});
        variant.requestDetails().setParameters(originalParams);

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), Measure.class)
                .create();

        assertNotSame(originalParams, result.getParameters());
        assertEquals(originalParams, result.getParameters());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_PreservesRequestDetailsType(RequestDetailsVariant variant) {
        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), Measure.class)
                .create();

        assertInstanceOf(variant.requestDetails().getClass(), result);
    }

    @Test
    void startWith_SystemRequestDetails_PreservesRequestPartitionId() {
        var partitionId = RequestPartitionId.fromPartitionId(123);
        var system = new SystemRequestDetails();
        system.setParameters(new HashMap<>());
        system.setRequestPartitionId(partitionId);

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(system, Measure.class)
                .create();

        assertInstanceOf(SystemRequestDetails.class, result);
        assertEquals(partitionId, ((SystemRequestDetails) result).getRequestPartitionId());
    }

    @Test
    void startWith_SystemRequestDetails_PreservesNullRequestPartitionId() {
        var system = new SystemRequestDetails();
        system.setParameters(new HashMap<>());

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(system, Measure.class)
                .create();

        assertInstanceOf(SystemRequestDetails.class, result);
        assertNull(((SystemRequestDetails) result).getRequestPartitionId());
    }

    @Test
    void startWith_UnsupportedType_ThrowsInvalidRequestException() {
        // Mock creates a proxy that is neither SystemRequestDetails nor ServletRequestDetails
        RequestDetails unsupported = mock(RequestDetails.class);

        assertThrows(
                InvalidRequestException.class,
                () -> ClinicalIntelligenceRequestDetailsCloner.startWith(unsupported, Measure.class));
    }
}
