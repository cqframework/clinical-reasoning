package org.opencds.cqf.fhir.cql.cql2elm.content;

import org.cqframework.cql.cql2elm.MimeType;

public enum ModelInfoContentType implements MimeType {
    XML("application/xml"),
    JSON("application/json");

    private final String mimeType;

    private ModelInfoContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return this.mimeType;
    }
}