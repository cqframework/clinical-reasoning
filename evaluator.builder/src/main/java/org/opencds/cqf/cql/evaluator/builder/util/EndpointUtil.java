package org.opencds.cqf.cql.evaluator.builder.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.api.model.ConnectionType;
import org.opencds.cqf.cql.evaluator.fhir.api.ResourceAdapter;

import ca.uhn.fhir.context.FhirContext;

import static org.opencds.cqf.cql.evaluator.fhir.common.AdapterFactory.resourceAdapterFor;

public class EndpointUtil {
    public static EndpointInfo getEndpointInfo (FhirContext fhirContext, IBaseResource endpoint) {
        Objects.requireNonNull(endpoint, "endpoint can not be null");

        if (endpoint.fhirType() == null || !endpoint.fhirType().equals("Endpoint")) {
            throw new IllegalArgumentException("endpoint is not a FHIR Endpoint resource.");
        }

        if (!fhirContext.getVersion().getVersion().equals(endpoint.getStructureFhirVersionEnum())) {
            throw new IllegalArgumentException("the FHIR versions of fhirContext and endpoint do not match");
        }

        ResourceAdapter adapter = resourceAdapterFor(endpoint);

        IBase connectionType = adapter.getSingleProperty(endpoint, "connectionType");
        IBase url = adapter.getSingleProperty(endpoint, "address");
        IBase[] headerArray = adapter.getProperty(endpoint, "header");

        if (url == null) {
            throw new IllegalArgumentException("endpoint does not address specified");
        }

        ConnectionType type = null;
        if (connectionType != null) {
            type = ConnectionType.valueOf(connectionType.toString());
        }

        List<String> headers = new ArrayList<>();
        if (headerArray != null) {
            for (int i = 0; i < headerArray.length; i++) {
                headers.add(headerArray[i].toString());
            }
        }

        return new EndpointInfo().setUrl(url.toString()).setType(type).setHeaders(headers);
    }
}