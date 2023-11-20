package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Appointment;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class AppointmentResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Appointment resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        return null;
    }
}
