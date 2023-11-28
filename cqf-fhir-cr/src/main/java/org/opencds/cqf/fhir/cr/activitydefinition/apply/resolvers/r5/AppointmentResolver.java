package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Appointment;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class AppointmentResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public Appointment resolve(IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        return null;
    }
}
