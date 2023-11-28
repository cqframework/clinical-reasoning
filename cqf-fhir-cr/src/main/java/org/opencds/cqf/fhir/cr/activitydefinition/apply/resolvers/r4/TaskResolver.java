package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class TaskResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public TaskResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Task resolve(IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        var task = new Task();
        if (activityDefinition.hasExtension(TARGET_STATUS_URL)) {
            var value = activityDefinition.getExtensionByUrl(TARGET_STATUS_URL).getValue();
            if (value instanceof StringType) {
                task.setStatus(Task.TaskStatus.valueOf(
                        ((StringType) value).asStringValue().toUpperCase()));
            } else {
                logger.debug(
                        "Extension {} should have a value of type {}", TARGET_STATUS_URL, StringType.class.getName());
            }
        } else {
            task.setStatus(Task.TaskStatus.DRAFT);
        }

        task.setIntent(
                activityDefinition.hasIntent()
                        ? Task.TaskIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : Task.TaskIntent.PROPOSAL);

        if (activityDefinition.hasUrl()) {
            task.setInstantiatesCanonical(activityDefinition.getUrl());
        }

        if (activityDefinition.hasCode()) {
            task.setCode(activityDefinition.getCode());
        }

        // if (activityDefinition.hasExtension()) {
        //     task.setExtension(activityDefinition.getExtension());
        // }

        if (activityDefinition.hasDescription()) {
            task.setDescription(activityDefinition.getDescription());
        }
        return task;
    }
}
