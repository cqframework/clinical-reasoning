package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4PublishService;

@FunctionalInterface
public interface IPublishServiceFactory {
    R4PublishService create(RequestDetails requestDetails);
}
