package org.opencds.cqf.fhir.cr.hapi.dstu3;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureService;

@FunctionalInterface
public interface IMeasureServiceFactory {
    Dstu3MeasureService create(RequestDetails requestDetails);
}
