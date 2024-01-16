package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class TaskResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public TaskResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Task resolve(ICpgRequest request) {
        var task = new Task();
        task.setFor(new Reference(request.getSubjectId()));

        if (request.hasEncounterId()) {
            task.setEncounter(new Reference(request.getEncounterId()));
        }

        if (request.hasPractitionerId()) {
            task.setRequester(new Reference(request.getPractitionerId()));
        }

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
            task.setInstantiatesCanonical(activityDefinition.getUrl()
                    + (activityDefinition.hasVersion() ? String.format("|%s", activityDefinition.getVersion()) : ""));
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
