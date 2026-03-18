package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.repository.RepositoryProxyFactory;

/**
 * A RepositoryProxyFactory that always returns the local Repository for tests only.
 */
public class NoOpRepositoryProxyFactory implements RepositoryProxyFactory {

    @Override
    public IRepository proxy(
            IRepository localRepository,
            Boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return localRepository;
    }
}
