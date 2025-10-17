package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4RetireService;

@FunctionalInterface
public interface IRetireServiceFactory {
    R4RetireService create(RequestDetails requestDetails);
}
