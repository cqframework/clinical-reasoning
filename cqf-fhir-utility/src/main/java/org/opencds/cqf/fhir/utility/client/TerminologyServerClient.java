package org.opencds.cqf.fhir.utility.client;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.adapter.EndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.ParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;

/**
 * This class currently serves as a VSAC Terminology Server client as it expects the Endpoint provided to contain a VSAC username and api key.
 * Future enhancements include adding support for multiple endpoints
 */
public class TerminologyServerClient {

    private final FhirContext ctx;

    public TerminologyServerClient(FhirContext ctx) {
        this.ctx = ctx;
    }

    public IBaseResource expand(ValueSetAdapter valueSet, EndpointAdapter endpoint, ParametersAdapter parameters) {
        checkNotNull(valueSet, "expected non-null value for valueSet");
        checkNotNull(endpoint, "expected non-null value for endpoint");
        checkNotNull(parameters, "expected non-null value for parameters");
        var username = endpoint.getExtensionsByUrl(Constants.VSAC_USERNAME).stream()
                .findFirst()
                .map(ext -> ext.getValue().toString())
                .orElseThrow(() -> new UnprocessableEntityException("Cannot expand ValueSet without VSAC Username."));
        var apiKey = endpoint.getExtensionsByUrl(Constants.APIKEY).stream()
                .findFirst()
                .map(ext -> ext.getValue().toString())
                .orElseThrow(() -> new UnprocessableEntityException("Cannot expand ValueSet without VSAC API Key."));
        IGenericClient fhirClient = ctx.newRestfulGenericClient(getAddressBase(endpoint.getAddress()));
        Clients.registerAdditionalRequestHeadersAuth(fhirClient, username, apiKey);

        if (parameters.getParameter("url") == null) {
            parameters.addParameter(
                    valueSet.get().getStructureFhirVersionEnum().equals(FhirVersionEnum.DSTU3)
                            ? Parameters.newUriPart(ctx, "url", valueSet.getUrl())
                            : Parameters.newUrlPart(ctx, "url", valueSet.getUrl()));
        }
        if (valueSet.hasVersion() && parameters.getParameter("valueSetVersion") == null) {
            parameters.addParameter(Parameters.newStringPart(ctx, "valueSetVersion", valueSet.getVersion()));
        }
        // Invoke on the type using the url parameter
        return fhirClient
                .operation()
                .onType("ValueSet")
                .named("$expand")
                .withParameters((IBaseParameters) parameters.get())
                .returnResourceType(valueSet.get().getClass())
                .execute();
    }

    // Strips resource and id from the endpoint address URL, these are not needed as the client constructs the URL.
    // Converts http URLs to https
    private String getAddressBase(String address) {
        address = address.substring(0, address.indexOf(Objects.requireNonNull(Canonicals.getResourceType(address))));
        if (address.startsWith("http://")) {
            address = address.replaceFirst("http://", "https://");
        }
        return address;
    }
}
