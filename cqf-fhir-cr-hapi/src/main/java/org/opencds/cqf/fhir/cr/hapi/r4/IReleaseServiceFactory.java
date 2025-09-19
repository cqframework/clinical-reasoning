package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4ReleaseService;

@FunctionalInterface
public interface IReleaseServiceFactory {
    R4ReleaseService create(RequestDetails requestDetails);
}
