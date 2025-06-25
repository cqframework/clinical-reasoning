package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.measure.r4.R4MultiMeasureService;

@FunctionalInterface
public interface IMultiMeasureServiceFactory {
    R4MultiMeasureService create(RequestDetails requestDetails);
}
