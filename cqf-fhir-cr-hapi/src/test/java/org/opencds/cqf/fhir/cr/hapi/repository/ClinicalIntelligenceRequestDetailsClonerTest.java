package org.opencds.cqf.fhir.cr.hapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IRestfulResponse;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRestfulResponse;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ClinicalIntelligenceRequestDetailsClonerTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();

    record RequestDetailsVariant(String label, RequestDetails requestDetails) {
        @Override
        @Nonnull
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
                        variant.requestDetails(), FHIR_CONTEXT, Measure.class)
                .create();

        assertEquals(RequestTypeEnum.POST, result.getRequestType());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_ResetsOperationResourceAndNames(RequestDetailsVariant variant) {
        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), FHIR_CONTEXT, Measure.class)
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
                        variant.requestDetails(), FHIR_CONTEXT, Measure.class)
                .create();

        assertEquals("test-tenant", result.getTenantId());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_PreservesResponse(RequestDetailsVariant variant) {
        IRestfulResponse expectedResponse = new SystemRestfulResponse(null);
        variant.requestDetails().setResponse(expectedResponse);

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), FHIR_CONTEXT, Measure.class)
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
                        variant.requestDetails(), FHIR_CONTEXT, Measure.class)
                .create();

        assertNotSame(originalParams, result.getParameters());
        assertEquals(originalParams, result.getParameters());
    }

    @ParameterizedTest
    @MethodSource("requestDetailsVariants")
    void startWith_PreservesRequestDetailsType(RequestDetailsVariant variant) {
        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        variant.requestDetails(), FHIR_CONTEXT, Measure.class)
                .create();

        assertInstanceOf(variant.requestDetails().getClass(), result);
    }

    @Test
    void startWith_SystemRequestDetails_PreservesRequestPartitionId() {
        var partitionId = RequestPartitionId.fromPartitionId(123);
        var system = new SystemRequestDetails();
        system.setParameters(new HashMap<>());
        system.setRequestPartitionId(partitionId);

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(system, FHIR_CONTEXT, Measure.class)
                .create();

        assertInstanceOf(SystemRequestDetails.class, result);
        assertEquals(partitionId, ((SystemRequestDetails) result).getRequestPartitionId());
    }

    @Test
    void startWith_SystemRequestDetails_PreservesNullRequestPartitionId() {
        var system = new SystemRequestDetails();
        system.setParameters(new HashMap<>());

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(system, FHIR_CONTEXT, Measure.class)
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
                () -> ClinicalIntelligenceRequestDetailsCloner.startWith(unsupported, FHIR_CONTEXT, Measure.class));
    }

    record PartitionableResourceVariant(
            String label,
            FhirContext fhirContext,
            Class<? extends IBaseResource> resourceType,
            boolean expectedPartitionable) {
        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<PartitionableResourceVariant> partitionableResourceVariants() {
        return Stream.of(
                // R4 partitionable resources
                new PartitionableResourceVariant(
                        "R4 Patient", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.Patient.class, true),
                new PartitionableResourceVariant(
                        "R4 Observation", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.Observation.class, true),
                new PartitionableResourceVariant(
                        "R4 Measure", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.Measure.class, true),
                new PartitionableResourceVariant(
                        "R4 Encounter", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.Encounter.class, true),
                // R4 non-partitionable resources (all 11 types)
                new PartitionableResourceVariant(
                        "R4 Library", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.Library.class, false),
                new PartitionableResourceVariant(
                        "R4 ValueSet", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.ValueSet.class, false),
                new PartitionableResourceVariant(
                        "R4 StructureMap", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.StructureMap.class, false),
                new PartitionableResourceVariant(
                        "R4 StructureDefinition",
                        FhirContext.forR4Cached(),
                        org.hl7.fhir.r4.model.StructureDefinition.class,
                        false),
                new PartitionableResourceVariant(
                        "R4 Questionnaire",
                        FhirContext.forR4Cached(),
                        org.hl7.fhir.r4.model.Questionnaire.class,
                        false),
                new PartitionableResourceVariant(
                        "R4 NamingSystem", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.NamingSystem.class, false),
                new PartitionableResourceVariant(
                        "R4 CompartmentDefinition",
                        FhirContext.forR4Cached(),
                        org.hl7.fhir.r4.model.CompartmentDefinition.class,
                        false),
                new PartitionableResourceVariant(
                        "R4 SearchParameter",
                        FhirContext.forR4Cached(),
                        org.hl7.fhir.r4.model.SearchParameter.class,
                        false),
                new PartitionableResourceVariant(
                        "R4 ConceptMap", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.ConceptMap.class, false),
                new PartitionableResourceVariant(
                        "R4 OperationDefinition",
                        FhirContext.forR4Cached(),
                        org.hl7.fhir.r4.model.OperationDefinition.class,
                        false),
                new PartitionableResourceVariant(
                        "R4 CodeSystem", FhirContext.forR4Cached(), org.hl7.fhir.r4.model.CodeSystem.class, false),
                // DSTU3 partitionable
                new PartitionableResourceVariant(
                        "DSTU3 Patient", FhirContext.forDstu3Cached(), org.hl7.fhir.dstu3.model.Patient.class, true),
                new PartitionableResourceVariant(
                        "DSTU3 Observation",
                        FhirContext.forDstu3Cached(),
                        org.hl7.fhir.dstu3.model.Observation.class,
                        true),
                // DSTU3 non-partitionable
                new PartitionableResourceVariant(
                        "DSTU3 Library", FhirContext.forDstu3Cached(), org.hl7.fhir.dstu3.model.Library.class, false),
                new PartitionableResourceVariant(
                        "DSTU3 ValueSet", FhirContext.forDstu3Cached(), org.hl7.fhir.dstu3.model.ValueSet.class, false),
                new PartitionableResourceVariant(
                        "DSTU3 CodeSystem",
                        FhirContext.forDstu3Cached(),
                        org.hl7.fhir.dstu3.model.CodeSystem.class,
                        false),
                // R5 partitionable
                new PartitionableResourceVariant(
                        "R5 Patient", FhirContext.forR5Cached(), org.hl7.fhir.r5.model.Patient.class, true),
                new PartitionableResourceVariant(
                        "R5 Observation", FhirContext.forR5Cached(), org.hl7.fhir.r5.model.Observation.class, true),
                // R5 non-partitionable
                new PartitionableResourceVariant(
                        "R5 Library", FhirContext.forR5Cached(), org.hl7.fhir.r5.model.Library.class, false),
                new PartitionableResourceVariant(
                        "R5 ValueSet", FhirContext.forR5Cached(), org.hl7.fhir.r5.model.ValueSet.class, false),
                new PartitionableResourceVariant(
                        "R5 CodeSystem", FhirContext.forR5Cached(), org.hl7.fhir.r5.model.CodeSystem.class, false));
    }

    @ParameterizedTest
    @MethodSource("partitionableResourceVariants")
    void startWith_SystemRequestDetails_PreservesPartitionIdOnlyForPartitionableResource(
            PartitionableResourceVariant variant) {
        var partitionId = RequestPartitionId.fromPartitionId(123);
        var system = new SystemRequestDetails();
        system.setParameters(new HashMap<>());
        system.setRequestPartitionId(partitionId);

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        system, variant.fhirContext(), variant.resourceType())
                .create();

        assertInstanceOf(SystemRequestDetails.class, result);
        if (variant.expectedPartitionable()) {
            assertEquals(partitionId, ((SystemRequestDetails) result).getRequestPartitionId());
        } else {
            assertNull(((SystemRequestDetails) result).getRequestPartitionId());
        }
    }

    @ParameterizedTest
    @MethodSource("partitionableResourceVariants")
    void startWith_SystemRequestDetails_PreservesTenantIdRegardlessOfPartitionability(
            PartitionableResourceVariant variant) {
        // Tenant ID is always preserved for SystemRequestDetails because the
        // constructor copies it, regardless of whether the resource is partitionable
        var system = new SystemRequestDetails();
        system.setParameters(new HashMap<>());
        system.setTenantId("test-tenant");

        RequestDetails result = ClinicalIntelligenceRequestDetailsCloner.startWith(
                        system, variant.fhirContext(), variant.resourceType())
                .create();

        assertEquals("test-tenant", result.getTenantId());
    }
}
