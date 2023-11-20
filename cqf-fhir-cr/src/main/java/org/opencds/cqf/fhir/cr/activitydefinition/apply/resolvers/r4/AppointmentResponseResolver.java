package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.AppointmentResponse;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class AppointmentResponseResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResponseResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public AppointmentResponse resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        if (activityDefinition == null) {
            return null;
        }
        var appointmentResponse = new AppointmentResponse();

        return appointmentResponse;
    }
}
