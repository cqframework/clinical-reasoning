package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class CustomActivityResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CustomActivityResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Task resolve(IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        // Do custom logic

        var task = new Task();
        task.setStatus(TaskStatus.COMPLETED);
        task.addOutput(new Task.TaskOutputComponent());

        return task;
    }
}
