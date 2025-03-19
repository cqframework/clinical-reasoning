package org.opencds.cqf.fhir.cr.hapi.cdshooks;

public class CdsCrSettings {
    private static final String DEFAULT_CLIENT_ID_HEADER_NAME = "client_id";
    private String clientIdHeaderName;

    public static CdsCrSettings getDefault() {
        CdsCrSettings settings = new CdsCrSettings();
        settings.setClientIdHeaderName(DEFAULT_CLIENT_ID_HEADER_NAME);
        return settings;
    }

    public void setClientIdHeaderName(String name) {
        clientIdHeaderName = name;
    }

    public String getClientIdHeaderName() {
        return clientIdHeaderName;
    }

    public CdsCrSettings withClientIdHeaderName(String name) {
        clientIdHeaderName = name;
        return this;
    }
}
