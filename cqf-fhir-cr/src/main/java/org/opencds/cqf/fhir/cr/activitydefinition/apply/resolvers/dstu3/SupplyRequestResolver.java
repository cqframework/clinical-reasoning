package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SupplyRequest;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestOrderedItemComponent;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestRequesterComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class SupplyRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public SupplyRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public SupplyRequest resolve(ICpgRequest request) {
        var supplyRequest = new SupplyRequest();

        if (request.hasPractitionerId()) {
            supplyRequest.setRequester(new SupplyRequestRequesterComponent(new Reference(request.getPractitionerId())));
        }

        if (request.hasOrganizationId()) {
            supplyRequest.setRequester(new SupplyRequestRequesterComponent(new Reference(request.getOrganizationId())));
        }

        if (activityDefinition.hasCode()) {
            if (!activityDefinition.hasQuantity()) {
                throw new FHIRException("Missing required orderedItem.quantity property");
            }
            supplyRequest.setOrderedItem(new SupplyRequestOrderedItemComponent(activityDefinition.getQuantity())
                    .setItem(activityDefinition.getCode()));
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
