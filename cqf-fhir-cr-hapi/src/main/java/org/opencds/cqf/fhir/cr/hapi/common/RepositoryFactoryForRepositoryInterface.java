package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.repository.Repository;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * Factory interface to return a {@link Repository} from a {@link RequestDetails}
 */
@FunctionalInterface
public interface RepositoryFactoryForRepositoryInterface {
    Repository create(RequestDetails requestDetails);
}
