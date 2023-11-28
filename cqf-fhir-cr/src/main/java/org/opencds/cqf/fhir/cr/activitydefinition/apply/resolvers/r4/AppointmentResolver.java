package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class AppointmentResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Appointment resolve(IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        if (activityDefinition == null) {
            return null;
        }
        var appointment = new Appointment();

        appointment.setStatus(AppointmentStatus.PROPOSED);
        var patientParticipant = new AppointmentParticipantComponent().setStatus(ParticipationStatus.NEEDSACTION);
        patientParticipant.setActor(new Reference(subjectId));
        appointment.addParticipant(patientParticipant);

        return appointment;
    }
}
