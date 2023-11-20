package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.AppointmentResponse;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class AppointmentResponseResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResponseResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public AppointmentResponse resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }
}
