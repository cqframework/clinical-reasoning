package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.RequestGroup;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyOperationRequest;

public class RequestGroupResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public RequestGroupResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public RequestGroup resolve(IApplyOperationRequest request) {
        var requestGroup = new RequestGroup();

        return requestGroup;
    }
}
