package org.opencds.cqf.fhir.utility.client.terminology;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.client.Clients;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

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
        return address.matches("https*://cts.nlm.nih.gov/fhir.*");
    }

    /**
     * VSAC-specific expansion handling.
     * VSAC requires the version to be part of the canonical URL (url|version) for ExpandRunner
     * to construct the correct resource ID (ValueSet/id-version).
     *
     * @param fhirClient the FHIR client to use for expansion
     * @param url the ValueSet URL (without version)
     * @param parameters the expansion parameters (may contain version parameter)
     * @return the expanded ValueSet
     */
    @Override
    public IBaseResource expand(IGenericClient fhirClient, String url, IBaseParameters parameters) {
        // Extract version from parameters if present
        var parametersAdapter = (IParametersAdapter) IAdapterFactory.createAdapterForResource(parameters);
        String version = null;

        if (parametersAdapter.hasParameter(versionParamName)) {
            var versionParam = parametersAdapter.getParameter(versionParamName);
            if (versionParam != null && versionParam.hasValue()) {
                var value = versionParam.getValue();
                if (value instanceof IPrimitiveType<?> primitive) {
                    version = String.valueOf(primitive.getValue());
                }
            }
        }

        // For VSAC, construct a versioned canonical URL (url|version)
        // This allows ExpandRunner.buildResourceIdForExpand() to extract the version
        // and build the correct VSAC resource ID: ValueSet/id-version
        String versionedCanonical = version != null ? url + "|" + version : url;

        // Call parent with versioned canonical
        return super.expand(fhirClient, versionedCanonical, parameters);
    }
}
