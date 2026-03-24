package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureService;

@FunctionalInterface
public interface R4MeasureServiceFactory {
    R4MeasureService create(RequestDetails requestDetails);
}
