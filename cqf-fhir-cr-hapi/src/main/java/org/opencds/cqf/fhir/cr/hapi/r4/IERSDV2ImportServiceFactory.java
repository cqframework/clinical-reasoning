package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.ecr.r4.R4ERSDTransformService;

@FunctionalInterface
public interface IERSDV2ImportServiceFactory {
    R4ERSDTransformService create(RequestDetails requestDetails);
}
