package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CommunicationRequest;
import org.hl7.fhir.r5.model.Enumerations.RequestStatus;
import org.hl7.fhir.r5.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class CommunicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CommunicationRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public CommunicationRequest resolve(
            IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        var communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(RequestStatus.DRAFT);
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

        if (activityDefinition.hasCode()) {
            communicationRequest.addPayload().setContent(activityDefinition.getCode());
        }

        return communicationRequest;
    }
}
