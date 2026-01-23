package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4InferManifestParametersService;

@FunctionalInterface
public interface IInferManifestParametersServiceFactory {
    R4InferManifestParametersService create(RequestDetails requestDetails);
}
