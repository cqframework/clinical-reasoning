package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.measure.r4.npm.R4FhirOrNpmResourceProvider;

/**
 * Factory to create an {@link R4FhirOrNpmResourceProvider} from a {@link RequestDetails}
 */
@FunctionalInterface
public interface R4FhirOrNpmResourceProviderFactory {
    R4FhirOrNpmResourceProvider create(RequestDetails requestDetails);
}
