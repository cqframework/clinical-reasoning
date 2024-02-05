package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import java.util.Collections;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class ReferralRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ReferralRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ReferralRequest resolve(ICpgRequest request) {
        // status, intent, code, and subject are required
        var referralRequest = new ReferralRequest();
        referralRequest.setStatus(ReferralRequest.ReferralRequestStatus.DRAFT);
        referralRequest.setIntent(ReferralRequest.ReferralCategory.ORDER);
        referralRequest.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasUrl()) {
            referralRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (request.hasPractitionerId()) {
            referralRequest.setRequester(
                    new ReferralRequestRequesterComponent(new Reference(request.getPractitionerId())));
        } else if (request.hasOrganizationId() != null) {
            referralRequest.setRequester(
                    new ReferralRequestRequesterComponent(new Reference(request.getOrganizationId())));
        }

        // code can be set as a dynamicValue
        if (activityDefinition.hasCode()) {
            referralRequest.setServiceRequested(Collections.singletonList(activityDefinition.getCode()));
        } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new FHIRException("Missing required code property");
        }

        return referralRequest;
    }
}
