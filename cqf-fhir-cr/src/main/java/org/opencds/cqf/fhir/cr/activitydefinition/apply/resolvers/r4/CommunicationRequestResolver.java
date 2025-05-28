package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.CommunicationRequest.CommunicationRequestStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class CommunicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CommunicationRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public CommunicationRequest resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        var communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(CommunicationRequestStatus.ACTIVE);
        communicationRequest.setSubject(new Reference(request.getSubjectId()));

        if (request.hasEncounterId()) {
            communicationRequest.setEncounter(new Reference(request.getEncounterId()));
        }

        if (request.hasPractitionerId()) {
            communicationRequest.setRequester(new Reference(request.getPractitionerId()));
        }

        if (activityDefinition.hasDoNotPerform()) {
            communicationRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
        }

        if (activityDefinition.hasCode() && activityDefinition.getCode().hasText()) {
            communicationRequest
                    .addPayload()
                    .setContent(new StringType(activityDefinition.getCode().getText()));
        }

        return communicationRequest;
    }
}
