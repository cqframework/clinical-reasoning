package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.client.Clients;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherDSTU3;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR4;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR5;

public class Repositories {

    private Repositories() {
        // intentionally empty
    }

    public static ProxyRepository proxy(IRepository data, IRepository content, IRepository terminology) {
        return new ProxyRepository(data, content, terminology);
    }

    private static IGenericClient createClient(FhirContext fhirContext, IBaseResource endpoint) {
        var fhirVersion = fhirContext.getVersion().getVersion();
        return switch (fhirVersion) {
            case DSTU3 -> Clients.forEndpoint(fhirContext, (org.hl7.fhir.dstu3.model.Endpoint) endpoint);
            case R4 -> Clients.forEndpoint(fhirContext, (org.hl7.fhir.r4.model.Endpoint) endpoint);
            case R5 -> Clients.forEndpoint(fhirContext, (org.hl7.fhir.r5.model.Endpoint) endpoint);
            default ->
                throw new IllegalArgumentException(
                        "unsupported FHIR version: %s".formatted(fhirVersion.getFhirVersionString()));
        };
    }

    public static IRepository createRestRepository(FhirContext fhirContext, IBaseResource endpoint) {
        return endpoint == null ? null : new RestRepository(createClient(fhirContext, endpoint));
    }

    public static IRepository proxy(
            IRepository localRepository,
            Boolean useLocalData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return proxy(
                localRepository,
                useLocalData,
                createRestRepository(localRepository.fhirContext(), dataEndpoint),
                createRestRepository(localRepository.fhirContext(), contentEndpoint),
                createRestRepository(localRepository.fhirContext(), terminologyEndpoint));
    }

    public static IRepository proxy(
            IRepository localRepository,
            Boolean useServerData,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        var useLocalData = useServerData == null ? Boolean.TRUE : useServerData;
        if (dataRepository == null
                && contentRepository == null
                && terminologyRepository == null
                && Boolean.TRUE.equals(useLocalData)) {
            return localRepository;
        }
        return new ProxyRepository(
                localRepository, useLocalData, dataRepository, contentRepository, terminologyRepository);
    }

    public static ResourceMatcher getResourceMatcher(FhirContext context) {
        var fhirVersion = context.getVersion().getVersion();
        return switch (fhirVersion) {
            case DSTU3 -> new ResourceMatcherDSTU3();
            case R4 -> new ResourceMatcherR4();
            case R5 -> new ResourceMatcherR5();
            default ->
                throw new NotImplementedException("Resource matching is not implemented for FHIR version: %s"
                        .formatted(fhirVersion.getFhirVersionString()));
        };
    }
}
