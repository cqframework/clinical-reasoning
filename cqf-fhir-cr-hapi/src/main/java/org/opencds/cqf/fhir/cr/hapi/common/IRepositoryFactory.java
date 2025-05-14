package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.jpa.repository.HapiFhirRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.google.common.annotations.Beta;

/**
 * @deprecated Use class from HAPI
 */
@Deprecated(since = "3.25.0", forRemoval = true)
@FunctionalInterface
@Beta
public interface IRepositoryFactory {
    HapiFhirRepository create(RequestDetails requestDetails);
}
