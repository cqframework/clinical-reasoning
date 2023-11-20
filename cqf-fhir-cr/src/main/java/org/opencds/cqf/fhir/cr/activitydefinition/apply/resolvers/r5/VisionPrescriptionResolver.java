package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class VisionPrescriptionResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public VisionPrescriptionResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public IBaseResource resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }
}
