package org.opencds.cqf.fhir.utility.client.terminology;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * This class is a router that links ValueSets + Endpoints to actual Terminology Servers.
 * Routing rules are derived from the CRMI IG,
 * https://hl7.org/fhir/uv/crmi/StructureDefinition-crmi-artifact-endpoint-configurable-operation.html
 */
public class FederatedTerminologyProviderRouter extends BaseTerminologyProvider implements ITerminologyProviderRouter {

    private final List<ITerminologyServerClient> clients;
    private final ITerminologyServerClient defaultClient;

    public FederatedTerminologyProviderRouter(FhirContext fhirContext) {
        this(fhirContext, null);
    }

    public FederatedTerminologyProviderRouter(
            FhirContext fhirContext, TerminologyServerClientSettings terminologyServerClientSettings) {
        super(fhirContext);

        defaultClient = new GenericTerminologyServerClient(fhirContext, terminologyServerClientSettings);
        clients = new ArrayList<>();
        clients.add(defaultClient);
        clients.add(new VsacTerminologyServerClient(fhirContext, terminologyServerClientSettings));
    }

    private ITerminologyServerClient getClient(String address) {
        return clients.stream()
                .filter(client -> client.isCanonicalMatch(address))
                .findFirst()
                .orElse(defaultClient);
    }

    /**
     * Returns a list of endpoints sorted such that the endpoint that matches the ValueSet's canonical URL is first
     * @param endpoints List of terminology server endpoints that can be used to lookup ValueSet resources
     * @param url       ValueSet's Canonical URL
     * @return          Sorted endpoints
     */
    private List<IEndpointAdapter> prioritizeEndpoints(List<IEndpointAdapter> endpoints, String url) {
        if (endpoints == null || endpoints.isEmpty()) {
            return List.of();
        }

        Comparator<IEndpointAdapter> byUrlMatch = Comparator.comparing((IEndpointAdapter endpoint) -> {
            var address = endpoint.getAddress();
            return url == null || address == null || !url.startsWith(address);
        });

        Comparator<IEndpointAdapter> byAddress =
                Comparator.comparing(IEndpointAdapter::getAddress, Comparator.nullsLast(Comparator.naturalOrder()));

        return endpoints.stream().sorted(byUrlMatch.thenComparing(byAddress)).toList();
    }

    @Override
    public IBaseResource expand(IValueSetAdapter valueSet, IEndpointAdapter endpoint, IParametersAdapter parameters) {
        return this.getClient(valueSet.getUrl()).expand(valueSet, endpoint, parameters);
    }

    @Override
    public IBaseResource expand(IEndpointAdapter endpoint, IParametersAdapter parameters, FhirVersionEnum fhirVersion) {
        return this.getClient(endpoint.getAddress()).expand(endpoint, parameters, fhirVersion);
    }

    @Override
    public IBaseResource expand(
            IEndpointAdapter endpoint,
            IParametersAdapter parameters,
            String url,
            String valueSetVersion,
            FhirVersionEnum fhirVersion) {
        return this.getClient(url).expand(endpoint, parameters, url, valueSetVersion, fhirVersion);
    }

    @Override
    public IBaseResource expand(IGenericClient fhirClient, String url, IBaseParameters parameters) {
        return this.getClient(url).expand(fhirClient, url, parameters);
    }

    @Override
    public IBaseResource expand(
            IValueSetAdapter valueSet, List<IEndpointAdapter> endpoints, IParametersAdapter parameters) {
        return prioritizeEndpoints(endpoints, valueSet.getUrl()).stream()
                .map(endpoint -> expand(valueSet, endpoint, parameters))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public IBaseResource expand(
            List<IEndpointAdapter> endpoints, IParametersAdapter parameters, FhirVersionEnum fhirVersion) {
        return endpoints.stream()
                .map(endpoint -> expand(endpoint, parameters, fhirVersion))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public IBaseResource expand(
            List<IEndpointAdapter> endpoints,
            IParametersAdapter parameters,
            String url,
            String valueSetVersion,
            FhirVersionEnum fhirVersion) {
        return prioritizeEndpoints(endpoints, url).stream()
                .map(endpoint -> expand(endpoint, parameters, url, valueSetVersion, fhirVersion))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Optional<IDomainResource> getCodeSystemResource(IEndpointAdapter endpoint, String url) {
        var client = this.getClient(endpoint.getAddress());
        return IKnowledgeArtifactAdapter.findLatestVersion(client.initializeClientWithAuth(endpoint)
                .search()
                .forResource(client.getCodeSystemClass())
                .where(Searches.byCanonical(url))
                .execute());
    }

    @Override
    public Optional<IDomainResource> getCodeSystemResource(List<IEndpointAdapter> endpoints, String url) {
        return prioritizeEndpoints(endpoints, url).stream()
                .map(endpoint -> getCodeSystemResource(endpoint, url))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public Optional<IDomainResource> getLatestValueSetResource(IEndpointAdapter endpoint, String url) {
        var client = this.getClient(endpoint.getAddress());
        return IKnowledgeArtifactAdapter.findLatestVersion(client.initializeClientWithAuth(endpoint)
                .search()
                .forResource(client.getValueSetClass())
                .where(Searches.byCanonical(url))
                .execute());
    }

    @Override
    public Optional<IDomainResource> getLatestValueSetResource(List<IEndpointAdapter> endpoints, String url) {
        return prioritizeEndpoints(endpoints, url).stream()
                .map(endpoint -> getLatestValueSetResource(endpoint, url))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public org.hl7.fhir.r4.model.TerminologyCapabilities getR4TerminologyCapabilities(IEndpointAdapter endpoint) {
        return getClient(endpoint.getAddress()).getR4TerminologyCapabilities(endpoint);
    }

    @Override
    public TerminologyServerClientSettings getTerminologyServerClientSettings(IEndpointAdapter endpoint) {
        return getClient(endpoint.getAddress()).getTerminologyServerClientSettings();
    }

    @Override
    public Optional<IDomainResource> getValueSetResource(IEndpointAdapter endpoint, String url) {
        var client = this.getClient(url);
        return IKnowledgeArtifactAdapter.findLatestVersion(client.initializeClientWithAuth(endpoint)
                .search()
                .forResource(client.getValueSetClass())
                .where(Searches.byCanonical(url))
                .execute());
    }

    @Override
    public Optional<IDomainResource> getValueSetResource(List<IEndpointAdapter> endpoints, String url) {
        return prioritizeEndpoints(endpoints, url).stream()
                .map(endpoint -> getValueSetResource(endpoint, url))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
