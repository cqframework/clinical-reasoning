package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.google.common.annotations.Beta;
import org.opencds.cqf.fhir.cr.hapi.repo.HapiFhirRepository;

@FunctionalInterface
@Beta
public interface IRepositoryFactory {
    HapiFhirRepository create(RequestDetails requestDetails);
}
