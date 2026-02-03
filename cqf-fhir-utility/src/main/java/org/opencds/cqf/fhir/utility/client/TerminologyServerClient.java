package org.opencds.cqf.fhir.utility.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * This class currently serves as a VSAC Terminology Server client as it expects the Endpoint provided to contain a VSAC username and api key.
 * Future enhancements include adding support for multiple endpoints
 */
public class TerminologyServerClient {
    private final FhirContext fhirContext;
    private final TerminologyServerClientSettings terminologyServerClientSettings;
    public static final String versionParamName = "valueSetVersion";
    public static final String urlParamName = "url";

    public TerminologyServerClient(FhirContext fhirContext) {
        this(fhirContext, null);
    }

    public TerminologyServerClient(
            FhirContext fhirContext, TerminologyServerClientSettings terminologyServerClientSettings) {
        this.fhirContext = requireNonNull(fhirContext);
        this.terminologyServerClientSettings = terminologyServerClientSettings != null
                ? terminologyServerClientSettings
                : TerminologyServerClientSettings.getDefault();
    }

    public TerminologyServerClientSettings getTerminologyServerClientSettings() {
        return this.terminologyServerClientSettings;
    }

    public org.hl7.fhir.r4.model.TerminologyCapabilities getR4TerminologyCapabilities(IEndpointAdapter endpoint) {
        var fhirClient = initializeClientWithAuth(endpoint);

        return fhirClient.fetchResourceFromUrl(
                org.hl7.fhir.r4.model.TerminologyCapabilities.class, "/metadata?mode=terminology");
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
                            ? Parameters.newUriPart(fhirContext, urlParamName, url)
                            : Parameters.newUrlPart(fhirContext, urlParamName, url));
        }
        if (parameters.getParameter(versionParamName) == null && valueSetVersion != null) {
            parameters.addParameter(Parameters.newStringPart(fhirContext, versionParamName, valueSetVersion));
        }
        return expand(fhirClient, url, (IBaseParameters) parameters.get());
    }

    public IBaseResource expand(IGenericClient fhirClient, String url, IBaseParameters parameters) {
        var expandRunner = new ExpandRunner(fhirClient, terminologyServerClientSettings, url, parameters);
        return expandRunner.expandValueSet();
    }

    public IGenericClient initializeClientWithAuth(IEndpointAdapter endpoint) {
        var fhirClient = fhirContext.newRestfulGenericClient(getAddressBase(endpoint.getAddress()));

        if (endpoint.hasHeaders()) {
            Clients.registerHeaders(fhirClient, endpoint.getHeaders());
        }

        fhirClient
                .getFhirContext()
                .getRestfulClientFactory()
                .setSocketTimeout(terminologyServerClientSettings.getSocketTimeout() * 1000);

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

    public java.util.Optional<IDomainResource> getValueSetResource(IEndpointAdapter endpoint, String url) {
        var bundle = initializeClientWithAuth(endpoint)
                .search()
                .forResource(getValueSetClass())
                .where(Searches.byCanonical(url))
                .execute();

        // If version is specified in canonical URL, find exact match; otherwise find latest
        return findResourceByVersion(bundle, url);
    }

    public java.util.Optional<IDomainResource> getCodeSystemResource(IEndpointAdapter endpoint, String url) {
        var bundle = initializeClientWithAuth(endpoint)
                .search()
                .forResource(getCodeSystemClass())
                .where(Searches.byCanonical(url))
                .execute();

        // If version is specified in canonical URL, find exact match; otherwise find latest
        return findResourceByVersion(bundle, url);
    }

    public java.util.Optional<IDomainResource> getLatestValueSetResource(IEndpointAdapter endpoint, String url) {
        var urlParams = Searches.byCanonical(url);
        return IKnowledgeArtifactAdapter.findLatestVersion(initializeClientWithAuth(endpoint)
                .search()
                .forResource(getValueSetClass())
                .where(urlParams)
                .execute());
    }

    /**
     * Finds a resource in the bundle matching the version specified in the canonical URL.
     * If no version is specified, returns the latest version.
     * If a version is specified but not found, returns empty.
     *
     * @param bundle The search result bundle
     * @param canonical The canonical URL (may include version)
     * @return Optional containing the matching resource, or empty if not found
     */
    private java.util.Optional<IDomainResource> findResourceByVersion(
            org.hl7.fhir.instance.model.api.IBaseBundle bundle, String canonical) {
        var parts = Canonicals.getParts(canonical);
        String requestedVersion = parts.version();

        if (requestedVersion == null) {
            // No version specified, return latest
            return IKnowledgeArtifactAdapter.findLatestVersion(bundle);
        }

        // Version specified, find exact match
        var resources = org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources(bundle);
        for (var resource : resources) {
            if (resource instanceof IDomainResource domainResource) {
                var adapter = org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.forFhirVersion(
                                fhirContext.getVersion().getVersion())
                        .createKnowledgeArtifactAdapter(domainResource);

                if (adapter.hasVersion() && adapter.getVersion().equals(requestedVersion)) {
                    return java.util.Optional.of(domainResource);
                }
            }
        }

        // Requested version not found
        return java.util.Optional.empty();
    }

    public Class<? extends IBaseResource> getValueSetClass() {
        return Resources.getClassForTypeAndVersion(
                "ValueSet", fhirContext.getVersion().getVersion());
    }

    public Class<? extends IBaseResource> getCodeSystemClass() {
        return Resources.getClassForTypeAndVersion(
                "CodeSystem", fhirContext.getVersion().getVersion());
    }

    private String getAddressBase(String address) {
        return getAddressBase(address, this.fhirContext);
    }

    // Strips resource and id from the endpoint address URL, these are not needed as the client constructs the URL.
    // Converts http URLs to https
    public static String getAddressBase(String address, FhirContext ctx) {
        requireNonNull(address, "address must not be null");
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
