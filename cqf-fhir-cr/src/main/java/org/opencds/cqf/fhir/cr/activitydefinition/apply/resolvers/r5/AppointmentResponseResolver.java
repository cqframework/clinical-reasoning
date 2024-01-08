package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.AppointmentResponse;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyRequest;

public class AppointmentResponseResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public AppointmentResponseResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public AppointmentResponse resolve(IApplyRequest request) {
        var appointmentResponse = new AppointmentResponse();

        return appointmentResponse;
    }
}
