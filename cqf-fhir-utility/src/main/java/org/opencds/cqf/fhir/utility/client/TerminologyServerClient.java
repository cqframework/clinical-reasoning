package org.opencds.cqf.fhir.utility.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.opencds.cqf.fhir.utility.Canonicals;

public class TerminologyServerClient {

    private final FhirContext ctx;

    public TerminologyServerClient(FhirContext ctx) {
        this.ctx = ctx;
    }

    public org.hl7.fhir.dstu3.model.ValueSet expand(
            org.hl7.fhir.dstu3.model.ValueSet valueSet,
            String authoritativeSource,
            org.hl7.fhir.dstu3.model.Parameters expansionParameters,
            String username,
            String apiKey) {
        IGenericClient fhirClient = ctx.newRestfulGenericClient(getAuthoritativeSourceBase(authoritativeSource));
        Clients.registerAdditionalRequestHeadersAuth(fhirClient, username, apiKey);
        if (expansionParameters == null) {
            expansionParameters = new org.hl7.fhir.dstu3.model.Parameters();
        }
        if (!expansionParameters.getParameter().stream()
                .anyMatch(p -> p.getName().equals("url"))) {
            expansionParameters
                    .addParameter()
                    .setName("url")
                    .setValue(new org.hl7.fhir.dstu3.model.UriType(valueSet.getUrl()));
        }
        if (valueSet.hasVersion()
                && !expansionParameters.getParameter().stream()
                        .anyMatch(p -> p.getName().equals("valueSetVersion"))) {
            expansionParameters
                    .addParameter()
                    .setName("valueSetVersion")
                    .setValue(new org.hl7.fhir.dstu3.model.StringType(valueSet.getVersion()));
        }
        // Invoke on the type using the url parameter
        return fhirClient
                .operation()
                .onType("ValueSet")
                .named("$expand")
                .withParameters(expansionParameters)
                .returnResourceType(org.hl7.fhir.dstu3.model.ValueSet.class)
                .execute();
    }

    public org.hl7.fhir.r4.model.ValueSet expand(
            org.hl7.fhir.r4.model.ValueSet valueSet,
            String authoritativeSource,
            org.hl7.fhir.r4.model.Parameters expansionParameters,
            String username,
            String apiKey) {
        IGenericClient fhirClient = ctx.newRestfulGenericClient(getAuthoritativeSourceBase(authoritativeSource));
        Clients.registerAdditionalRequestHeadersAuth(fhirClient, username, apiKey);
        if (expansionParameters == null) {
            expansionParameters = new org.hl7.fhir.r4.model.Parameters();
        }
        if (!expansionParameters.hasParameter("url")) {
            expansionParameters.addParameter("url", new org.hl7.fhir.r4.model.UriType(valueSet.getUrl()));
        }
        if (valueSet.hasVersion() && !expansionParameters.hasParameter("valueSetVersion")) {
            expansionParameters.addParameter(
                    "valueSetVersion", new org.hl7.fhir.r4.model.StringType(valueSet.getVersion()));
        }
        // Invoke on the type using the url parameter
        return fhirClient
                .operation()
                .onType("ValueSet")
                .named("$expand")
                .withParameters(expansionParameters)
                .returnResourceType(org.hl7.fhir.r4.model.ValueSet.class)
                .execute();
    }

    public org.hl7.fhir.r5.model.ValueSet expand(
            org.hl7.fhir.r5.model.ValueSet valueSet,
            String authoritativeSource,
            org.hl7.fhir.r5.model.Parameters expansionParameters,
            String username,
            String apiKey) {
        IGenericClient fhirClient = ctx.newRestfulGenericClient(getAuthoritativeSourceBase(authoritativeSource));
        Clients.registerAdditionalRequestHeadersAuth(fhirClient, username, apiKey);

        if (expansionParameters == null) {
            expansionParameters = new org.hl7.fhir.r5.model.Parameters();
        }
        if (!expansionParameters.hasParameter("url")) {
            expansionParameters.addParameter("url", new org.hl7.fhir.r5.model.UriType(valueSet.getUrl()));
        }
        if (valueSet.hasVersion() && !expansionParameters.hasParameter("valueSetVersion")) {
            expansionParameters.addParameter(
                    "valueSetVersion", new org.hl7.fhir.r5.model.StringType(valueSet.getVersion()));
        }
        // Invoke on the type using the url parameter
        return fhirClient
                .operation()
                .onType("ValueSet")
                .named("$expand")
                .withParameters(expansionParameters)
                .returnResourceType(org.hl7.fhir.r5.model.ValueSet.class)
                .execute();
    }

    // Strips resource and id from the authoritative source URL, these are not needed as the client constructs the URL.
    // Converts http URLs to https
    public String getAuthoritativeSourceBase(String authoritativeSource) {
        Objects.requireNonNull(authoritativeSource, "authoritativeSource must not be null");
        if (authoritativeSource.startsWith("http://")) {
            authoritativeSource = authoritativeSource.replaceFirst("http://", "https://");
        }
        // remove trailing slashes
        if (authoritativeSource.endsWith("/")) {
            authoritativeSource = authoritativeSource.substring(0, authoritativeSource.length() - 1);
        }
        // check if URL is in the format [base URL]/[resource type]/[id]
        var maybeFhirType = Canonicals.getResourceType(authoritativeSource);
        if (maybeFhirType != null && !maybeFhirType.isBlank()) {
            IBaseEnumFactory<?> factory = getEnumFactory();
            try {
                factory.fromCode(maybeFhirType);
            } catch (IllegalArgumentException e) {
                // check if URL is in the format [base URL]/[resource type]
                var lastSlashIndex = authoritativeSource.lastIndexOf("/");
                if (lastSlashIndex > 0) {
                    maybeFhirType = authoritativeSource.substring(
                        lastSlashIndex + 1,
                        authoritativeSource.length());
                        try {
                            factory.fromCode(maybeFhirType);
                        } catch (IllegalArgumentException e2) {
                            return authoritativeSource;
                        }  
                } else {
                    return authoritativeSource;
                }
            }
            authoritativeSource = authoritativeSource.substring(
                0,
                authoritativeSource.indexOf(maybeFhirType) - 1);
        }
        return authoritativeSource;
    }
    private IBaseEnumFactory<?> getEnumFactory() {
            switch (this.ctx.getVersion().getVersion()) {
                case DSTU3:
                    return new org.hl7.fhir.dstu3.model.Enumerations.ResourceTypeEnumFactory();
                    
                case R4:
                    return new org.hl7.fhir.r4.model.Enumerations.ResourceTypeEnumFactory();
                    
                case R5:
                    return new org.hl7.fhir.r5.model.Enumerations.ResourceTypeEnumEnumFactory();
                    
                default:
                    throw new UnprocessableEntityException("unsupported FHIR version: " + this.ctx.getVersion().getVersion().toString());
            }
    }
}
