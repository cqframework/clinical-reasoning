package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.cpg.r4.R4CqlExecutionService;

@FunctionalInterface
public interface ICqlExecutionServiceFactory {
    R4CqlExecutionService create(RequestDetails requestDetails);
}
