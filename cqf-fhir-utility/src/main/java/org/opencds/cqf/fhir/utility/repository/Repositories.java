package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.client.Clients;
import org.opencds.cqf.fhir.utility.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.matcher.BaseResourceMatcher;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherDSTU3;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR4;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR5;

public class Repositories {

    private Repositories() {
        // intentionally empty
    }

    public static ProxyRepository proxy(Repository data, Repository content, Repository terminology) {
        return new ProxyRepository(data, content, terminology);
    }

    private static IGenericClient getClient(FhirContext fhirContext, IBaseResource endpoint) {
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return Clients.forEndpoint(fhirContext, (org.hl7.fhir.dstu3.model.Endpoint) endpoint);
            case R4:
                return Clients.forEndpoint(fhirContext, (org.hl7.fhir.r4.model.Endpoint) endpoint);
            case R5:
                return Clients.forEndpoint(fhirContext, (org.hl7.fhir.r5.model.Endpoint) endpoint);
            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s", fhirContext));
        }
    }

    public static Repository proxy(
            Repository localRepository,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        Repository data = dataEndpoint == null
                ? null
                : new RestRepository(getClient(localRepository.fhirContext(), dataEndpoint));
        Repository content = contentEndpoint == null
                ? null
                : new RestRepository(getClient(localRepository.fhirContext(), contentEndpoint));
        Repository terminology = terminologyEndpoint == null
                ? null
                : new RestRepository(getClient(localRepository.fhirContext(), terminologyEndpoint));

        return new ProxyRepository(localRepository, data, content, terminology);
    }

    public static BaseResourceMatcher getResourceMatcher(FhirContext context) {
        var fhirVersion = context.getVersion().getVersion();
        switch (fhirVersion) {
            case DSTU3:
                return new ResourceMatcherDSTU3(FhirModelResolverCache.resolverForVersion(fhirVersion));
            case R4:
                return new ResourceMatcherR4(FhirModelResolverCache.resolverForVersion(fhirVersion));
            case R5:
                return new ResourceMatcherR5(FhirModelResolverCache.resolverForVersion(fhirVersion));
            default:
                throw new NotImplementedException(
                        "Resource matching is not implemented for FHIR version " + fhirVersion.getFhirVersionString());
        }
    }
}
