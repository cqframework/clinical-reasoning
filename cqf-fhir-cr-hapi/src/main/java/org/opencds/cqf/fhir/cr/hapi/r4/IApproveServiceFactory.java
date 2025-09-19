package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4ApproveService;

@FunctionalInterface
public interface IApproveServiceFactory {
    R4ApproveService create(RequestDetails requestDetails);
}
