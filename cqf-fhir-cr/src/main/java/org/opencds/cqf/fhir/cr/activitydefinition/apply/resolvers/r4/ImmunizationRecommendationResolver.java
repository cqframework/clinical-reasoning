package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class ImmunizationRecommendationResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ImmunizationRecommendationResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ImmunizationRecommendation resolve(
            IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        if (activityDefinition == null) {
            return null;
        }

        return null;
    }
}
