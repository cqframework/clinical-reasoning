package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4DraftService;

@FunctionalInterface
public interface IDraftServiceFactory {
    R4DraftService create(RequestDetails requestDetails);
}
