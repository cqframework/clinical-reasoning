package org.opencds.cqf.fhir.cr.hapi;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4DeleteService;

@FunctionalInterface
public interface IDeleteServiceFactory {

    R4DeleteService create(RequestDetails requestDetails);
}
