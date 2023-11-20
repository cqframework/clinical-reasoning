package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.CommunicationRequest;
import org.hl7.fhir.dstu3.model.CommunicationRequest.CommunicationRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class CommunicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CommunicationRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public CommunicationRequest resolve(
            String subjectId, String encounterId, String practitionerId, String organizationId) {
        var communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(CommunicationRequest.CommunicationRequestStatus.DRAFT);
        communicationRequest.setSubject(new Reference(subjectId));

        if (encounterId != null && !encounterId.isEmpty()) {
            communicationRequest.setContext(new Reference(encounterId));
        }

        if (practitionerId != null && !practitionerId.isEmpty()) {
            communicationRequest.setRequester(
                    new CommunicationRequestRequesterComponent(new Reference(practitionerId)));
        }

        if (activityDefinition.hasCode() && activityDefinition.getCode().hasText()) {
            communicationRequest
                    .addPayload()
                    .setContent(new StringType(activityDefinition.getCode().getText()));
        }

        return communicationRequest;
    }
}
