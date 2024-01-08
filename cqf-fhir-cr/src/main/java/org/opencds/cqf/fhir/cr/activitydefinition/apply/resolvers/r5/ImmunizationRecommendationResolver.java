package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.ImmunizationRecommendation;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyRequest;

public class ImmunizationRecommendationResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ImmunizationRecommendationResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ImmunizationRecommendation resolve(IApplyRequest request) {
        var immunizationRecommendation = new ImmunizationRecommendation();

        return immunizationRecommendation;
    }
}
