package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.dstu2016may.model.Task;
import org.hl7.fhir.dstu2016may.model.Task.TaskStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class CustomActivityResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CustomActivityResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public IBaseResource resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        // Do custom logic

        var task = new Task();
        task.setStatus(TaskStatus.COMPLETED);
        task.addOutput(new Task.TaskOutputComponent().setName("Work completed"));

        return task;
    }
    
}
