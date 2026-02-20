package org.opencds.cqf.fhir.cr.hapi.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ResourceType;

/**
 * This is an exact copy of the RequestDetailsCloner from hapi-fhir, which is package-private, so
 * we can't access it.
 */
class ClinicalIntelligenceRequestDetailsCloner {

    // Note: These are R4 resource types, so might need to be eventually update for R5,R6,etc
    private static final Set<ResourceType> NON_PARTITIONABLE_RESOURCE_TYPES = Set.of(
            ResourceType.Library,
            ResourceType.ValueSet,
            ResourceType.StructureMap,
            ResourceType.StructureDefinition,
            ResourceType.Questionnaire,
            ResourceType.NamingSystem,
            ResourceType.CompartmentDefinition,
            ResourceType.SearchParameter,
            ResourceType.ConceptMap,
            ResourceType.OperationDefinition,
            ResourceType.CodeSystem);

    private static final Set<String> NON_PARTITIONABLE_RESOURCE_TYPE_STRINGS =
            NON_PARTITIONABLE_RESOURCE_TYPES.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());

    private ClinicalIntelligenceRequestDetailsCloner() {}

    static <T extends IBaseResource> DetailsBuilder startWith(
            RequestDetails origRequestDetails, FhirContext fhirContext, Class<T> resourceType) {
        final RequestDetails newDetails;

        final boolean isPartitionableResource = isPartitionableResource(fhirContext, resourceType);

        if (origRequestDetails instanceof ServletRequestDetails servletDetails) {
            newDetails = new ServletRequestDetails(servletDetails);
        } else if (origRequestDetails instanceof SystemRequestDetails systemRequestDetails) {
            final SystemRequestDetails clonedSystemRequestDetails = new SystemRequestDetails(origRequestDetails);
            if (isPartitionableResource) {
                clonedSystemRequestDetails.setRequestPartitionId(systemRequestDetails.getRequestPartitionId());
            }
            newDetails = clonedSystemRequestDetails;
        } else {
            throw new InvalidRequestException("Unsupported request origRequestDetails type: %s"
                    .formatted(origRequestDetails.getClass().getName()));
        }
        if (isPartitionableResource) {
            newDetails.setTenantId(origRequestDetails.getTenantId());
        }
        newDetails.setRequestType(RequestTypeEnum.POST);
        newDetails.setOperation(null);
        newDetails.setResource(null);
        newDetails.setParameters(new HashMap<>(origRequestDetails.getParameters()));
        newDetails.setResourceName(null);
        newDetails.setCompartmentName(null);
        newDetails.setResponse(origRequestDetails.getResponse());

        return new DetailsBuilder(newDetails);
    }

    // note that parallel logic is maintained in hapi-fhir BaseRequestPartitionHelperSvc#isResourcePartitionable
    private static <T extends IBaseResource> boolean isPartitionableResource(
            FhirContext fhirContext, Class<T> resourceType) {
        final String resourceTypeString = fhirContext.getResourceType(resourceType);

        return !NON_PARTITIONABLE_RESOURCE_TYPE_STRINGS.contains(resourceTypeString);
    }

    static class DetailsBuilder {

        private final RequestDetails details;

        DetailsBuilder(RequestDetails details) {
            this.details = details;
        }

        DetailsBuilder setAction(RestOperationTypeEnum restOperationType) {
            details.setRestOperationType(restOperationType);
            return this;
        }

        DetailsBuilder addHeaders(Map<String, String> headers) {
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    details.addHeader(entry.getKey(), entry.getValue());
                }
            }

            return this;
        }

        DetailsBuilder setParameters(Map<String, String[]> parameters) {
            details.setParameters(parameters);
            return this;
        }

        DetailsBuilder setResourceType(String resourceName) {
            details.setResourceName(resourceName);

            return this;
        }

        DetailsBuilder setId(IIdType id) {
            details.setId(id);

            return this;
        }

        RequestDetails create() {
            return details;
        }
    }
}
