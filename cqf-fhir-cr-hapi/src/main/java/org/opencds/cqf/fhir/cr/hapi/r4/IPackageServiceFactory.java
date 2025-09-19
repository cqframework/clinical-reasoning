package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4PackageService;

@FunctionalInterface
public interface IPackageServiceFactory {
    R4PackageService create(RequestDetails requestDetails);
}
