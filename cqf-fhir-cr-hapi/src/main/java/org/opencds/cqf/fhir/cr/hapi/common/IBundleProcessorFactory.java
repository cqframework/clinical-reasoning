package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.bundle.BundleProcessor;

@FunctionalInterface
public interface IBundleProcessorFactory {
    BundleProcessor create(RequestDetails requestDetails);

}
