package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CoverageEligibilityRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyOperationRequest;

public class CoverageEligibilityRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public CoverageEligibilityRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public CoverageEligibilityRequest resolve(IApplyOperationRequest request) {
        var coverageEligibilityRequest = new CoverageEligibilityRequest();

        return coverageEligibilityRequest;
    }
}
