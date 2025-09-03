package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.measure.r4.npm.R4RepositoryOrNpmResourceProvider;

/**
 * Factory to create an {@link R4RepositoryOrNpmResourceProvider} from a {@link RequestDetails}
 */
@FunctionalInterface
public interface R4RepositoryOrNpmResourceProviderFactory {
    R4RepositoryOrNpmResourceProvider create(RequestDetails requestDetails);
}
