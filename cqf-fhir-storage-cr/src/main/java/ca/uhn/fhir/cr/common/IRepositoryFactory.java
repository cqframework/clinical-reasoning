package ca.uhn.fhir.cr.common;

import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.google.common.annotations.Beta;

@FunctionalInterface
@Beta
public interface IRepositoryFactory {
    HapiFhirRepository create(RequestDetails requestDetails);
}
