package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class SupplyRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public SupplyRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public SupplyRequest resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        var supplyRequest = new SupplyRequest();

        supplyRequest.setStatus(SupplyRequest.SupplyRequestStatus.DRAFT);

        if (practitionerId != null) {
            supplyRequest.setRequester(new Reference(practitionerId));
        }

        if (organizationId != null) {
            supplyRequest.setRequester(new Reference(organizationId));
        }

        if (activityDefinition.hasQuantity()) {
            supplyRequest.setQuantity(activityDefinition.getQuantity());
        } else {
            throw new FHIRException("Missing required orderedItem.quantity property");
        }

        if (activityDefinition.hasCode()) {
            supplyRequest.setItem(activityDefinition.getCode());
        }

        if (activityDefinition.hasProduct()) {
            throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasBodySite()) {
            throw new FHIRException(BODYSITE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return supplyRequest;
    }
}
