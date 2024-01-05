package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.RequestOrchestration;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyOperationRequest;

public class RequestOrchestrationResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public RequestOrchestrationResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public RequestOrchestration resolve(IApplyOperationRequest request) {
        var requestOrchestration = new RequestOrchestration();

        return requestOrchestration;
    }
}
