package org.opencds.cqf.cql.evaluator.builder.api.model;

import java.util.List;

public class EndpointInfo {
    private String url;
    private ConnectionType type;
    private List<String> headers;

    public String getUrl() {
        return this.url;
    }

    public EndpointInfo setUrl(String url) {
        this.url = url;
        return this;
    }

    public ConnectionType getType() {
        return this.type;
    }

    public EndpointInfo setType(ConnectionType type) {
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