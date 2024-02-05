package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CodeableReference;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.SupplyRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class SupplyRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public SupplyRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public SupplyRequest resolve(ICpgRequest request) {
        var supplyRequest = new SupplyRequest();

        supplyRequest.setStatus(SupplyRequest.SupplyRequestStatus.DRAFT);

        if (request.hasPractitionerId()) {
            supplyRequest.setRequester(new Reference(request.getPractitionerId()));
        }

        if (request.hasPractitionerId()) {
            supplyRequest.setRequester(new Reference(request.getPractitionerId()));
        }

        if (activityDefinition.hasQuantity()) {
            supplyRequest.setQuantity(activityDefinition.getQuantity());
        } else {
            throw new FHIRException("Missing required orderedItem.quantity property");
        }

        if (activityDefinition.hasCode()) {
            supplyRequest.setItem(new CodeableReference(activityDefinition.getCode()));
        }

        return supplyRequest;
    }
}
