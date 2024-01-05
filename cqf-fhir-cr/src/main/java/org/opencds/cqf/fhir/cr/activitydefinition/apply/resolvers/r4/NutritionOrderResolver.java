package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.NutritionOrder;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyOperationRequest;

public class NutritionOrderResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public NutritionOrderResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public NutritionOrder resolve(IApplyOperationRequest request) {
        var nutritionOrder = new NutritionOrder();

        return nutritionOrder;
    }
}
