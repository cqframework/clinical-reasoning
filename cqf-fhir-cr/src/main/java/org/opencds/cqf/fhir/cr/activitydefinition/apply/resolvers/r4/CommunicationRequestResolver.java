package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.CommunicationRequest.CommunicationRequestStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class CommunicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CommunicationRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public CommunicationRequest resolve(
            IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        var communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(CommunicationRequestStatus.DRAFT);
        communicationRequest.setSubject(new Reference(subjectId));

        if (encounterId != null && !encounterId.isEmpty()) {
            communicationRequest.setEncounter(new Reference(encounterId));
        }

        if (practitionerId != null && !practitionerId.isEmpty()) {
            communicationRequest.setRequester(new Reference(practitionerId));
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
