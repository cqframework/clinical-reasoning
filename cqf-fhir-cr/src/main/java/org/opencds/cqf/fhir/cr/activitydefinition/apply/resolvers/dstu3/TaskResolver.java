package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.Task.TaskRequesterComponent;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class TaskResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public TaskResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Task resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        var task = new Task();
        task.setFor(new Reference(request.getSubjectId()));

        if (request.hasEncounterId()) {
            task.setContext(new Reference(request.getEncounterId()));
        }

        if (request.hasPractitionerId()) {
            task.setRequester(new TaskRequesterComponent(new Reference(request.getPractitionerId())));
        }

        if (activityDefinition.hasExtension()) {
            var value = activityDefinition
                    .getExtensionsByUrl(TARGET_STATUS_URL)
                    .get(0)
                    .getValue();
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

        task.setIntent(Task.TaskIntent.PROPOSAL);

        if (activityDefinition.hasUrl()) {
            task.setDefinition(activityDefinition.getUrlElement());
        }

        if (activityDefinition.hasCode()) {
            task.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasDescription()) {
            task.setDescription(activityDefinition.getDescription());
        }
        return task;
    }
}
