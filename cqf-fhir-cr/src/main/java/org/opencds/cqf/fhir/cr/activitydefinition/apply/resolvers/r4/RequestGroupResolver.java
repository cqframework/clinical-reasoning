package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.RequestGroup;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class RequestGroupResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public RequestGroupResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public RequestGroup resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        if (activityDefinition == null) {
            return null;
        }

        return null;
    }
}
