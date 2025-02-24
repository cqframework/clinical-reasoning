package ca.uhn.fhir.cr.common;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;

/**
 * This interface takes a RequestDetails object and uses it to create a Repository which is passed to the constructor of the processor class being instantiated.
 */
@FunctionalInterface
public interface IActivityDefinitionProcessorFactory {
    ActivityDefinitionProcessor create(RequestDetails requestDetails);
}
