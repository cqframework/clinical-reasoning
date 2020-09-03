package org.opencds.cqf.cql.evaluator.builder;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseCoding;

public class EndpointInfo {
    private String address;
    private IBaseCoding type;
    private List<String> headers;

    public String getAddress() {
        return this.address;
    }

    public EndpointInfo setAddress(String address) {
        this.address = address;
        return this;
    }

    public IBaseCoding getType() {
        return this.type;
    }

    public EndpointInfo setType(IBaseCoding type) {
        this.type = type;
        return this;
    }

    public List<String> getHeaders() {
        return this.headers;
    }

    public EndpointInfo setHeaders(List<String> headers) {
        this.headers = headers;
        return this;
    }
}