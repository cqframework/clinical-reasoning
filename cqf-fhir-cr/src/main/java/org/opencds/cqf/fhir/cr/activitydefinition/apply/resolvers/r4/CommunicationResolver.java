package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import java.util.Collections;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Communication.CommunicationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class CommunicationResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CommunicationResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Communication resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        var communication = new Communication();

        communication.setStatus(CommunicationStatus.UNKNOWN);
        communication.setSubject(new Reference(subjectId));

        if (activityDefinition.hasCode()) {
            communication.setReasonCode(Collections.singletonList(activityDefinition.getCode()));
        }

        if (activityDefinition.hasRelatedArtifact()) {
            for (var artifact : activityDefinition.getRelatedArtifact()) {
                if (artifact.hasUrl()) {
                    var attachment = new Attachment().setUrl(artifact.getUrl());
                    if (artifact.hasDisplay()) {
                        attachment.setTitle(artifact.getDisplay());
                    }

                    var payload = new org.hl7.fhir.r4.model.Communication.CommunicationPayloadComponent();
                    payload.setContent(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                    communication.setPayload(Collections.singletonList(payload));
                }
            }
        }

        return communication;
    }
}
