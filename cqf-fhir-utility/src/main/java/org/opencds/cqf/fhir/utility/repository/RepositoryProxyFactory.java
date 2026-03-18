package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Factory for creating proxy repositories that route data, content, and terminology
 * requests to different endpoints. This abstraction allows tests to substitute
 * in-memory repositories instead of requiring live FHIR servers.
 */
public interface RepositoryProxyFactory {

    /**
     * Creates a proxy repository from endpoint resources (e.g., FHIR Endpoint).
     * Each non-null endpoint is converted to a repository via
     * {@link Repositories#createRestRepository}.
     */
    IRepository proxy(
            IRepository localRepository,
            Boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint);
}
