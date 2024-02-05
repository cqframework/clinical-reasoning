package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SupplyRequest;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestOrderedItemComponent;
import org.hl7.fhir.dstu3.model.SupplyRequest.SupplyRequestRequesterComponent;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class SupplyRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public SupplyRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public SupplyRequest resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        var supplyRequest = new SupplyRequest();

        if (request.hasPractitionerId()) {
            supplyRequest.setRequester(new SupplyRequestRequesterComponent(new Reference(request.getPractitionerId())));
        }

        if (request.hasOrganizationId()) {
            supplyRequest.setRequester(new SupplyRequestRequesterComponent(new Reference(request.getOrganizationId())));
        }

        if (activityDefinition.hasCode()) {
            supplyRequest.setOrderedItem(new SupplyRequestOrderedItemComponent(activityDefinition.getQuantity())
                    .setItem(activityDefinition.getCode()));
        }

        return supplyRequest;
    }
}
