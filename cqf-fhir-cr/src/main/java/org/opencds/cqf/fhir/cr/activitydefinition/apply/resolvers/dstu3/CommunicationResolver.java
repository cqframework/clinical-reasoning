package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import java.util.Collections;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class CommunicationResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CommunicationResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Communication resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        var communication = new Communication();

        communication.setStatus(Communication.CommunicationStatus.UNKNOWN);
        communication.setSubject(new Reference(request.getSubjectId()));

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

                    var payload = new Communication.CommunicationPayloadComponent();
                    payload.setContent(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                    communication.setPayload(Collections.singletonList(payload));
                }
            }
        }

        return communication;
    }
}
