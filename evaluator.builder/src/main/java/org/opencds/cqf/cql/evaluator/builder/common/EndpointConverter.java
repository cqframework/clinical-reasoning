package org.opencds.cqf.cql.evaluator.builder.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ResourceAdapter;

public class EndpointConverter implements org.opencds.cqf.cql.evaluator.builder.EndpointConverter {

    private AdapterFactory adapterFactory;

    @Inject
    public EndpointConverter(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    public EndpointInfo getEndpointInfo(IBaseResource endpoint) {
        Objects.requireNonNull(endpoint, "endpoint can not be null");

        if (endpoint.fhirType() == null || !endpoint.fhirType().equals("Endpoint")) {
            throw new IllegalArgumentException("endpoint is not a FHIR Endpoint resource.");
        }

        ResourceAdapter adapter = this.adapterFactory.createResource(endpoint);

        IPrimitiveType<?> url = (IPrimitiveType<?>)adapter.getSingleProperty("address");
        IBase[] headerArray = adapter.getProperty("header");
        IBaseCoding connectionType = (IBaseCoding)adapter.getSingleProperty("connectionType");

        if (url == null) {
            throw new IllegalArgumentException("endpoint does not address specified");
        }

        List<String> headers = null;
        if (headerArray != null && headerArray.length > 0) {
            headers = new ArrayList<>();
            for (int i = 0; i < headerArray.length; i++) {
                headers.add(headerArray[i].toString());
            }
        }

        return new EndpointInfo().setAddress(url.getValueAsString()).setType(connectionType).setHeaders(headers);
    }
}