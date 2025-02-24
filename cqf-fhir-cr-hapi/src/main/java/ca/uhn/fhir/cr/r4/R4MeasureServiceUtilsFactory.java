package ca.uhn.fhir.cr.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;

/**
 * Factory to create an {@link R4MeasureServiceUtils} from a {@link RequestDetails}
 */
@FunctionalInterface
public interface R4MeasureServiceUtilsFactory {
    R4MeasureServiceUtils create(RequestDetails requestDetails);
}
