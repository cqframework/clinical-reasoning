package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4ReleaseManifestService;

@FunctionalInterface
public interface IReleaseManifestServiceFactory {
    R4ReleaseManifestService create(RequestDetails requestDetails);
}
