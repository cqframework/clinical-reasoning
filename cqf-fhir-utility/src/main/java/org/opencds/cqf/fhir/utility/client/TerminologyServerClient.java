package org.opencds.cqf.fhir.utility.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Objects;
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

        // Invoke by Value Set ID
        return fhirClient
                .operation()
                .onInstance(valueSet.getId())
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

        // Invoke by Value Set ID
        return fhirClient
                .operation()
                .onInstance(valueSet.getId())
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

        // Invoke by Value Set ID
        return fhirClient
                .operation()
                .onInstance(valueSet.getId())
                .named("$expand")
                .withParameters(expansionParameters)
                .returnResourceType(org.hl7.fhir.r5.model.ValueSet.class)
                .execute();
    }

    // Strips resource and id from the authoritative source URL, these are not needed as the client constructs the URL.
    // Converts http URLs to https
    private String getAuthoritativeSourceBase(String authoritativeSource) {
        authoritativeSource = authoritativeSource.substring(
                0,
                authoritativeSource.indexOf(Objects.requireNonNull(Canonicals.getResourceType(authoritativeSource))));
        if (authoritativeSource.startsWith("http://")) {
            authoritativeSource = authoritativeSource.replaceFirst("http://", "https://");
        }
        return authoritativeSource;
    }
}
