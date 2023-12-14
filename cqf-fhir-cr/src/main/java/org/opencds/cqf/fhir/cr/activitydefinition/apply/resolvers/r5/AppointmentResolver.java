package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r5.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r5.model.Appointment.ParticipationStatus;
import org.hl7.fhir.r5.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class AppointmentResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Appointment resolve(IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        var appointment = new Appointment();

        appointment.setStatus(AppointmentStatus.PROPOSED);
        var patientParticipant = new AppointmentParticipantComponent().setStatus(ParticipationStatus.NEEDSACTION);
        patientParticipant.setActor(new Reference(subjectId));
        appointment.addParticipant(patientParticipant);

        return appointment;
    }
}
