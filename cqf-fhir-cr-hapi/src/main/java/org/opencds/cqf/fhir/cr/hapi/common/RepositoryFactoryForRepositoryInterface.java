package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * Factory interface to return a {@link IRepository} from a {@link RequestDetails}
 *
 * @deprecated Use class from HAPI
 */
@Deprecated(since = "3.25.0", forRemoval = true)
@FunctionalInterface
public interface RepositoryFactoryForRepositoryInterface {
    IRepository create(RequestDetails requestDetails);
}
