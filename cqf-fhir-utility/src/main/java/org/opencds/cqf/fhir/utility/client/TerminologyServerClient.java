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
    public static final String versionParamName = "valueSetVersion";
    public static final String urlParamName = "url";

    public TerminologyServerClient(FhirContext ctx) {
        this.ctx = ctx;
    }

    public IBaseResource expand(ValueSetAdapter valueSet, EndpointAdapter endpoint, ParametersAdapter parameters) {
        checkNotNull(valueSet, "expected non-null value for valueSet");
        checkNotNull(endpoint, "expected non-null value for endpoint");
        checkNotNull(parameters, "expected non-null value for parameters");
        return expand(
                endpoint,
                parameters,
                valueSet.getUrl(),
                valueSet.getVersion(),
                valueSet.get().getStructureFhirVersionEnum());
    }

    public IBaseResource expand(EndpointAdapter endpoint, ParametersAdapter parameters, FhirVersionEnum fhirVersion) {
        checkNotNull(endpoint, "expected non-null value for endpoint");
        checkNotNull(parameters, "expected non-null value for parameters");
        checkNotNull(fhirVersion, "expected non-null value for fhirVersion");
        checkNotNull(parameters.getParameter(urlParamName), "expected non-null value for 'url' expansion parameter");
        return expand(endpoint, parameters, null, null, fhirVersion);
    }

    public IBaseResource expand(
            EndpointAdapter endpoint,
            ParametersAdapter parameters,
            String url,
            String valueSetVersion,
            FhirVersionEnum fhirVersion) {
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

        if (parameters.getParameter(urlParamName) == null) {
            if (url == null) {
                throw new UnprocessableEntityException("No '" + urlParamName + "' expansion parameter present");
            }
            parameters.addParameter(
                    fhirVersion == FhirVersionEnum.DSTU3
                            ? Parameters.newUriPart(ctx, urlParamName, url)
                            : Parameters.newUrlPart(ctx, urlParamName, url));
        }
        if (parameters.getParameter(versionParamName) == null && valueSetVersion != null) {
            parameters.addParameter(Parameters.newStringPart(ctx, versionParamName, valueSetVersion));
        }
        // Invoke on the type using the url parameter
        return fhirClient
                .operation()
                .onType("ValueSet")
                .named("$expand")
                .withParameters((IBaseParameters) parameters.get())
                .returnResourceType(getValueSetClass(fhirVersion))
                .execute();
    }

    private Class<? extends IBaseResource> getValueSetClass(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return org.hl7.fhir.dstu3.model.ValueSet.class;
            case R4:
                return org.hl7.fhir.r4.model.ValueSet.class;
            case R5:
                return org.hl7.fhir.r5.model.ValueSet.class;
            default:
                throw new UnprocessableEntityException("Unknown ValueSet version");
        }
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
