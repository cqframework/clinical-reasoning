package org.opencds.cqf.fhir.utility.client.terminology;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.Clients;
import org.opencds.cqf.fhir.utility.client.ExpandRunner;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.search.Searches;

public class GenericTerminologyServerClient extends BaseTerminologyProvider implements
    ITerminologyServerClient {
    protected final TerminologyServerClientSettings terminologyServerClientSettings;

    public GenericTerminologyServerClient(FhirContext fhirContext) {
        this(fhirContext, null);
    }

    public GenericTerminologyServerClient(
            FhirContext fhirContext, TerminologyServerClientSettings terminologyServerClientSettings) {
        super(fhirContext);
        this.terminologyServerClientSettings = terminologyServerClientSettings != null
            ? terminologyServerClientSettings
            : TerminologyServerClientSettings.getDefault();
    }

    @Override
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

    @Override
    public IBaseResource expand(IEndpointAdapter endpoint, IParametersAdapter parameters, FhirVersionEnum fhirVersion) {
        checkNotNull(endpoint, "expected non-null value for endpoint");
        checkNotNull(parameters, "expected non-null value for parameters");
        checkNotNull(fhirVersion, "expected non-null value for fhirVersion");
        checkNotNull(parameters.getParameter(urlParamName), "expected non-null value for 'url' expansion parameter");
        return expand(endpoint, parameters, null, null, fhirVersion);
    }

    @Override
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

    @Override
    public IBaseResource expand(IGenericClient fhirClient, String url, IBaseParameters parameters) {
        var expandRunner = new ExpandRunner(fhirClient, terminologyServerClientSettings, url, parameters);
        return expandRunner.expandValueSet();
    }

    public org.hl7.fhir.r4.model.TerminologyCapabilities getR4TerminologyCapabilities(IEndpointAdapter endpoint) {
        var fhirClient = initializeClientWithAuth(endpoint);

        return fhirClient.fetchResourceFromUrl(
            org.hl7.fhir.r4.model.TerminologyCapabilities.class, "/metadata?mode=terminology");
    }

    @Override
    public TerminologyServerClientSettings getTerminologyServerClientSettings() {
        return this.terminologyServerClientSettings;
    }

    @Override
    public IGenericClient initializeClientWithAuth(IEndpointAdapter endpoint) {
        var fhirClient = fhirContext.newRestfulGenericClient(getAddressBase(endpoint.getAddress()));

        if (endpoint.hasHeaders()) {
            Clients.registerHeaders(fhirClient, endpoint.getHeaders());
        }

        fhirClient
                .getFhirContext()
                .getRestfulClientFactory()
                .setSocketTimeout(terminologyServerClientSettings.getSocketTimeout() * 1000);

        return fhirClient;
    }

    @Override
    public java.util.Optional<IDomainResource> getCodeSystemResource(IEndpointAdapter endpoint, String url) {
        return IKnowledgeArtifactAdapter.findLatestVersion(initializeClientWithAuth(endpoint)
                .search()
                .forResource(getCodeSystemClass())
                .where(Searches.byCanonical(url))
                .execute());
    }

    @Override
    public java.util.Optional<IDomainResource> getLatestValueSetResource(IEndpointAdapter endpoint, String url) {
        var urlParams = Searches.byCanonical(url);
        return IKnowledgeArtifactAdapter.findLatestVersion(initializeClientWithAuth(endpoint)
                .search()
                .forResource(getValueSetClass())
                .where(urlParams)
                .execute());
    }

    @Override
    public java.util.Optional<IDomainResource> getValueSetResource(IEndpointAdapter endpoint, String url) {
        return IKnowledgeArtifactAdapter.findLatestVersion(initializeClientWithAuth(endpoint)
            .search()
            .forResource(getValueSetClass())
            .where(Searches.byCanonical(url))
            .execute());
    }
}
