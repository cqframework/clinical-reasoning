package org.opencds.cqf.fhir.benchmark;

import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessorFactory;

public class ActivityDefinitionProcessorFactory implements IActivityDefinitionProcessorFactory {

    @Override
    public IActivityDefinitionProcessor create(Repository repository) {
        return new ActivityDefinitionProcessor(repository);
    }
}
