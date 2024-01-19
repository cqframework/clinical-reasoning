package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.client.Clients;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
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

    private static IGenericClient createClient(FhirContext fhirContext, IBaseResource endpoint) {
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

    public static Repository createRestRepository(FhirContext fhirContext, IBaseResource endpoint) {
        return endpoint == null ? null : new RestRepository(createClient(fhirContext, endpoint));
    }

    public static Repository proxy(
            Repository localRepository,
            Boolean useLocalData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        if (dataEndpoint == null && contentEndpoint == null && terminologyEndpoint == null) {
            return localRepository;
        }
        return new ProxyRepository(
                localRepository,
                useLocalData,
                createRestRepository(localRepository.fhirContext(), dataEndpoint),
                createRestRepository(localRepository.fhirContext(), contentEndpoint),
                createRestRepository(localRepository.fhirContext(), terminologyEndpoint));
    }

    public static Repository proxy(
            Repository localRepository,
            Boolean useServerData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
        if (dataRepository == null && contentRepository == null && terminologyRepository == null) {
            return localRepository;
        }
        return new ProxyRepository(
                localRepository,
                useServerData == null ? Boolean.TRUE : useServerData,
                dataRepository,
                contentRepository,
                terminologyRepository);
    }

    public static ResourceMatcher getResourceMatcher(FhirContext context) {
        var fhirVersion = context.getVersion().getVersion();
        switch (fhirVersion) {
            case DSTU3:
                return new ResourceMatcherDSTU3();
            case R4:
                return new ResourceMatcherR4();
            case R5:
                return new ResourceMatcherR5();
            default:
                throw new NotImplementedException(
                        "Resource matching is not implemented for FHIR version " + fhirVersion.getFhirVersionString());
        }
    }
}
