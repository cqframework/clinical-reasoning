package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.utility.Constants.REQUEST_DO_NOT_PERFORM;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.Task;
import org.hl7.fhir.r5.model.Task.TaskStatus;
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
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
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
            if (value instanceof StringType type) {
                task.setStatus(Task.TaskStatus.valueOf(type.asStringValue().toUpperCase()));
            } else {
                logger.debug(
                        "Extension {} should have a value of type {}", TARGET_STATUS_URL, StringType.class.getName());
            }
        } else {
            task.setStatus(TaskStatus.READY);
        }

        task.setIntent(
                activityDefinition.hasIntent()
                        ? Task.TaskIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : Task.TaskIntent.PROPOSAL);

        if (activityDefinition.hasUrl()) {
            task.setInstantiatesCanonical(activityDefinition.getUrl()
                    + (activityDefinition.hasVersion() ? "|%s".formatted(activityDefinition.getVersion()) : ""));
        }

        if (activityDefinition.hasCode()) {
            task.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasDescription()) {
            task.setDescription(activityDefinition.getDescription());
        }

        if (activityDefinition.getDoNotPerform()) {
            task.addModifierExtension(
                    new Extension(REQUEST_DO_NOT_PERFORM).setValue(activityDefinition.getDoNotPerformElement()));
        }

        return task;
    }
}
