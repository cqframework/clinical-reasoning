package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CarePlan;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class CarePlanResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CarePlanResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public CarePlan resolve(IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        var carePlan = new CarePlan();

        return carePlan;
    }
}
