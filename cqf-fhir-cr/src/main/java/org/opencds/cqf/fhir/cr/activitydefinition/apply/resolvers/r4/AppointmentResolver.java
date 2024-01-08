package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyRequest;

public class AppointmentResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Appointment resolve(IApplyRequest request) {
        var appointment = new Appointment();

        appointment.setStatus(AppointmentStatus.PROPOSED);
        var patientParticipant = new AppointmentParticipantComponent().setStatus(ParticipationStatus.NEEDSACTION);
        patientParticipant.setActor(new Reference(request.getSubjectId()));
        appointment.addParticipant(patientParticipant);

        return appointment;
    }
}
