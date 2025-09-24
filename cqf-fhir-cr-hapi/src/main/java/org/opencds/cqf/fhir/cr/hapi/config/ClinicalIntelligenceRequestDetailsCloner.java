package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * This is an exact copy of the RequestDetailsCloner from hapi-fhir, which is package-private, so
 * we can't access it.
 */
class ClinicalIntelligenceRequestDetailsCloner {

    private ClinicalIntelligenceRequestDetailsCloner() {}

    static DetailsBuilder startWith(RequestDetails details) {
        RequestDetails newDetails;
        if (details instanceof ServletRequestDetails servletDetails) {
            newDetails = new ServletRequestDetails(servletDetails);
        } else {
            newDetails = new SystemRequestDetails(details);
        }
        newDetails.setRequestType(RequestTypeEnum.POST);
        newDetails.setOperation(null);
        newDetails.setResource(null);
        newDetails.setParameters(new HashMap<>(details.getParameters()));
        newDetails.setResourceName(null);
        newDetails.setCompartmentName(null);
        newDetails.setResponse(details.getResponse());

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

        DetailsBuilder setParameters(IBaseParameters parameters) {
            IParser parser = details.getServer().getFhirContext().newJsonParser();
            details.setRequestContents(parser.encodeResourceToString(parameters).getBytes());

            return this;
        }

        DetailsBuilder setParameters(Map<String, String[]> parameters) {
            details.setParameters(parameters);

            return this;
        }

        DetailsBuilder withRestOperationType(RequestTypeEnum type) {
            details.setRequestType(type);

            return this;
        }

        DetailsBuilder setOperation(String operation) {
            details.setOperation(operation);

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
