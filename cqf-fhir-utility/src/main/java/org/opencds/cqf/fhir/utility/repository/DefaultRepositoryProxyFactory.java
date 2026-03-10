package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Default implementation that delegates to the static methods in {@link Repositories}.
 */
public class DefaultRepositoryProxyFactory implements RepositoryProxyFactory {

    @Override
    public IRepository proxy(
            IRepository localRepository,
            Boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return Repositories.proxy(localRepository, useServerData, dataEndpoint, contentEndpoint, terminologyEndpoint);
    }
}
