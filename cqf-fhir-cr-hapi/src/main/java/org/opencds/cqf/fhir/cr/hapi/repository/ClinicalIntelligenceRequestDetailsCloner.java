package org.opencds.cqf.fhir.cr.hapi.repository;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IRestfulResponse;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRestfulResponse;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * This is an exact copy of the RequestDetailsCloner from hapi-fhir, which is package-private, so
 * we can't access it.
 */
class ClinicalIntelligenceRequestDetailsCloner {

    private ClinicalIntelligenceRequestDetailsCloner() {}

    static <T extends IBaseResource> DetailsBuilder startWith(RequestDetails origRequestDetails) {
        final RequestDetails newDetails;

        if (origRequestDetails instanceof ServletRequestDetails servletDetails) {
            newDetails = new ServletRequestDetails(servletDetails);
        } else if (origRequestDetails instanceof SystemRequestDetails systemRequestDetails) {
            newDetails = new SystemRequestDetails(systemRequestDetails);
        } else {
            throw new InvalidRequestException("Unsupported request origRequestDetails type: %s"
                    .formatted(origRequestDetails.getClass().getName()));
        }

        IRestfulResponse response = origRequestDetails.getResponse();

        // we need IRestfulResponse because RestfulServer uses it during extended operation processing.
        if (response == null && origRequestDetails instanceof SystemRequestDetails systemDetails) {
            response = new SystemRestfulResponse(systemDetails);
        }
        newDetails.setResponse(response);

        // LUKETODO:  verify all of these:
        //        newDetails.setRequestType(RequestTypeEnum.POST);
        //        newDetails.setOperation(null);
        //        newDetails.setResource(null);
        //        newDetails.setParameters(new HashMap<>(origRequestDetails.getParameters()));
        //        newDetails.setResourceName(null);
        //        newDetails.setCompartmentName(null);

        return new DetailsBuilder(newDetails);
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
