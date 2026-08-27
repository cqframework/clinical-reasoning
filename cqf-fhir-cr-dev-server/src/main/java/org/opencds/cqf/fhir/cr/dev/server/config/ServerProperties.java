package org.opencds.cqf.fhir.cr.dev.server.config;

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

    // S1075 ("hardcoded URI") doesn't apply: this *is* the customizable parameter — the default
    // for a @ConfigurationProperties field overridden via cqf.server.base-path in application.yml.
    @SuppressWarnings("java:S1075")
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
