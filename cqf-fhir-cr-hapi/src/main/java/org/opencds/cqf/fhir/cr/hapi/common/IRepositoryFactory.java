package org.opencds.cqf.fhir.cr.hapi.common;

import org.opencds.cqf.fhir.cr.hapi.repo.HapiFhirRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.google.common.annotations.Beta;

@FunctionalInterface
@Beta
public interface IRepositoryFactory {
    HapiFhirRepository create(RequestDetails requestDetails);
}
