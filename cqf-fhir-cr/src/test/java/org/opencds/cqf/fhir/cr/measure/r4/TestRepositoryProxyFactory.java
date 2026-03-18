package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.RepositoryProxyFactory;

/**
 * A test-only RepositoryProxyFactory that maps non-null endpoint sentinel objects
 * to pre-built InMemoryFhirRepository instances, avoiding the need for live FHIR servers.
 *
 * <p>For the endpoint-based proxy method, any non-null endpoint is treated as a sentinel
 * indicating that the corresponding pre-built repository should be used.
 */
class TestRepositoryProxyFactory implements RepositoryProxyFactory {
    private final IRepository dataRepository;
    private final IRepository contentRepository;
    private final IRepository terminologyRepository;

    TestRepositoryProxyFactory(
            IRepository dataRepository, IRepository contentRepository, IRepository terminologyRepository) {
        this.dataRepository = dataRepository;
        this.contentRepository = contentRepository;
        this.terminologyRepository = terminologyRepository;
    }

    @Override
    public IRepository proxy(
            IRepository localRepository,
            Boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return Repositories.proxy(
                localRepository,
                useServerData,
                dataEndpoint != null ? dataRepository : null,
                contentEndpoint != null ? contentRepository : null,
                terminologyEndpoint != null ? terminologyRepository : null);
    }
}
