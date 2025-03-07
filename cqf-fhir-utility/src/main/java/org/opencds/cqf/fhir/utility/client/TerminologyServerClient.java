package org.opencds.cqf.fhir.utility.client;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class currently serves as a VSAC Terminology Server client as it expects the Endpoint provided to contain a VSAC username and api key.
 * Future enhancements include adding support for multiple endpoints
 */
public class TerminologyServerClient {
    private static final Logger myLogger = LoggerFactory.getLogger(TerminologyServerClient.class);

    private final FhirContext ctx;
    public static final String versionParamName = "valueSetVersion";
    public static final String urlParamName = "url";

    public TerminologyServerClient(FhirContext ctx) {
        this.ctx = ctx;
    }

    public IBaseResource expand(IValueSetAdapter valueSet, IEndpointAdapter endpoint, IParametersAdapter parameters) {
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

    public IBaseResource expand(IEndpointAdapter endpoint, IParametersAdapter parameters, FhirVersionEnum fhirVersion) {
        checkNotNull(endpoint, "expected non-null value for endpoint");
        checkNotNull(parameters, "expected non-null value for parameters");
        checkNotNull(fhirVersion, "expected non-null value for fhirVersion");
        checkNotNull(parameters.getParameter(urlParamName), "expected non-null value for 'url' expansion parameter");
        return expand(endpoint, parameters, null, null, fhirVersion);
    }

    public IBaseResource expand(
            IEndpointAdapter endpoint,
            IParametersAdapter parameters,
            String url,
            String valueSetVersion,
            FhirVersionEnum fhirVersion) {
        checkNotNull(endpoint, "expected non-null value for endpoint");
        checkNotNull(parameters, "expected non-null value for parameters");
        var fhirClient = initializeClientWithAuth(endpoint);
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

    private IGenericClient initializeClientWithAuth(IEndpointAdapter endpoint) {
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
        return fhirClient;
    }

    public java.util.Optional<IDomainResource> getResource(
            IEndpointAdapter endpoint, String url, FhirVersionEnum versionEnum) {
        return IKnowledgeArtifactAdapter.findLatestVersion(initializeClientWithAuth(endpoint)
                .search()
                .forResource(getValueSetClass(versionEnum))
                .where(Searches.byCanonical(url))
                .execute());
    }

    public java.util.Optional<IDomainResource> getLatestNonDraftResource(
            IEndpointAdapter endpoint, String url, FhirVersionEnum versionEnum) {
        var urlParams = Searches.byCanonical(url);
        var statusParam = Searches.exceptStatus("draft");
        urlParams.putAll(statusParam);
        return IKnowledgeArtifactAdapter.findLatestVersion(initializeClientWithAuth(endpoint)
                .search()
                .forResource(getValueSetClass(versionEnum))
                .where(urlParams)
                .execute());
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

    private String getAddressBase(String address) {
        return getAddressBase(address, this.ctx);
    }
    // Strips resource and id from the endpoint address URL, these are not needed as the client constructs the URL.
    // Converts http URLs to https
    public static String getAddressBase(String address, FhirContext ctx) {
        Objects.requireNonNull(address, "address must not be null");
        if (address.startsWith("http://")) {
            address = address.replaceFirst("http://", "https://");
        }
        // remove trailing slashes
        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        // check if URL is in the format [base URL]/[resource type]/[id]
        var maybeFhirType = Canonicals.getResourceType(address);
        if (maybeFhirType != null && StringUtils.isNotBlank(maybeFhirType)) {
            IBaseEnumFactory<?> factory = TerminologyServerClient.getEnumFactory(ctx);
            try {
                factory.fromCode(maybeFhirType);
            } catch (IllegalArgumentException e) {
                // check if URL is in the format [base URL]/[resource type]
                var lastSlashIndex = address.lastIndexOf("/");
                if (lastSlashIndex > 0) {
                    maybeFhirType = address.substring(lastSlashIndex + 1, address.length());
                    try {
                        factory.fromCode(maybeFhirType);
                    } catch (IllegalArgumentException e2) {
                        return address;
                    }
                } else {
                    return address;
                }
            }
            address = address.substring(0, address.indexOf(maybeFhirType) - 1);
        }
        return address;
    }

    public static IBaseEnumFactory<?> getEnumFactory(FhirContext ctx) {
        switch (ctx.getVersion().getVersion()) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Enumerations.ResourceTypeEnumFactory();

            case R4:
                return new org.hl7.fhir.r4.model.Enumerations.ResourceTypeEnumFactory();

            case R5:
                return new org.hl7.fhir.r5.model.Enumerations.ResourceTypeEnumEnumFactory();

            default:
                throw new UnprocessableEntityException("unsupported FHIR version: "
                        + ctx.getVersion().getVersion().toString());
        }
    }
}
