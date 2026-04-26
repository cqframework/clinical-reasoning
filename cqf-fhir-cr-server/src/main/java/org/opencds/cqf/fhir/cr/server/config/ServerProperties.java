package org.opencds.cqf.fhir.cr.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Server-level configuration. Exposed under the {@code cqf.server.*} prefix in
 * {@code application.yml}.
 *
 * <pre>
 * cqf:
 *   server:
 *     base-path: /fhir          # servlet mount path
 *     fhir-version: R4          # R4 or DSTU3
 * </pre>
 */
@ConfigurationProperties(prefix = "cqf.server")
public class ServerProperties {

    private String basePath = "/fhir";
    private String fhirVersion = "R4";

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void setFhirVersion(String fhirVersion) {
        this.fhirVersion = fhirVersion;
    }
}
