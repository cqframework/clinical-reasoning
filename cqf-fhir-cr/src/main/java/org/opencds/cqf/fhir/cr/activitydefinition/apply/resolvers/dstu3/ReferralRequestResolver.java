package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import java.util.Collections;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class ReferralRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ReferralRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ReferralRequest resolve(
            IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        // status, intent, code, and subject are required
        var referralRequest = new ReferralRequest();
        referralRequest.setStatus(ReferralRequest.ReferralRequestStatus.DRAFT);
        referralRequest.setIntent(ReferralRequest.ReferralCategory.ORDER);
        referralRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            referralRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (practitionerId != null) {
            referralRequest.setRequester(new ReferralRequestRequesterComponent(new Reference(practitionerId)));
        } else if (organizationId != null) {
            referralRequest.setRequester(new ReferralRequestRequesterComponent(new Reference(organizationId)));
        }

        // code can be set as a dynamicValue
        if (activityDefinition.hasCode()) {
            referralRequest.setServiceRequested(Collections.singletonList(activityDefinition.getCode()));
        } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new FHIRException("Missing required code property");
        }

        if (activityDefinition.hasBodySite()) {
            throw new FHIRException(BODYSITE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasProduct()) {
            throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return referralRequest;
    }
}
