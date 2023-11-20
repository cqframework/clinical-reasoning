package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class AppointmentResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Appointment resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        if (activityDefinition == null) {
            return null;
        }
        var appointment = new Appointment();

        appointment.setStatus(AppointmentStatus.PROPOSED);
        // appointment.addParticipant(new Appointment.AppointmentParticipantComponent(status).);
        // appointment.setParticipant(new  new Reference(subjectId));

        return appointment;
    }
}
