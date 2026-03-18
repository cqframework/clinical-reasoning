package org.opencds.cqf.fhir.cr;

import ca.uhn.fhir.repository.IRepository;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.NoOpRepositoryProxyFactory;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessorFactory;

public class ActivityDefinitionProcessorFactory implements IActivityDefinitionProcessorFactory {

    @Override
    public IActivityDefinitionProcessor create(IRepository repository) {
        return new ActivityDefinitionProcessor(
                repository, CrSettings.getDefault(), null, null, new NoOpRepositoryProxyFactory());
    }
}
