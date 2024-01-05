package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CommunicationRequest;
import org.hl7.fhir.r5.model.Enumerations.RequestStatus;
import org.hl7.fhir.r5.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyOperationRequest;

public class CommunicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CommunicationRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public CommunicationRequest resolve(IApplyOperationRequest request) {
        var communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(RequestStatus.DRAFT);
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

        if (activityDefinition.hasCode()) {
            communicationRequest.addPayload().setContent(activityDefinition.getCode());
        }

        return communicationRequest;
    }
}
