package org.opencds.cqf.fhir.utility.client.terminology;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.client.Clients;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

import static org.opencds.cqf.fhir.utility.Constants.VSAC_BASE_URL;

public class VsacTerminologyServerClient extends GenericTerminologyServerClient {

    public VsacTerminologyServerClient(FhirContext fhirContext) {
        super(fhirContext);
    }

    public VsacTerminologyServerClient(
            FhirContext fhirContext, TerminologyServerClientSettings terminologyServerClientSettings) {
        super(fhirContext, terminologyServerClientSettings);
    }

    @Override
    public IGenericClient initializeClientWithAuth(IEndpointAdapter endpoint) {
        var fhirClient = super.initializeClientWithAuth(endpoint);

        // This smacks of reinventing the wheel... Should be passed in with the Endpoint resource (Endpoint.header
        // element)... e.g. Basic {username:password (base64 encoded)}
        if (endpoint.hasExtension(Constants.VSAC_USERNAME) && endpoint.hasExtension(Constants.APIKEY)) {
            var username = endpoint.getExtensionsByUrl(Constants.VSAC_USERNAME).stream()
                    .findFirst()
                    .map(ext -> ext.getValue().toString())
                    .orElseThrow(() -> new UnprocessableEntityException(
                            String.format("Found a %s extension with no value", Constants.VSAC_USERNAME)));
            var apiKey = endpoint.getExtensionsByUrl(Constants.APIKEY).stream()
                    .findFirst()
                    .map(ext -> ext.getValue().toString())
                    .orElseThrow(() -> new UnprocessableEntityException(
                            String.format("Found a %s extension with no value", Constants.APIKEY)));
            Clients.registerAdditionalRequestHeadersAuth(fhirClient, username, apiKey);
        }

        return fhirClient;
    }

    @Override
    public boolean isCanonicalMatch(String address) {
        return address.startsWith(VSAC_BASE_URL);
    }
}
