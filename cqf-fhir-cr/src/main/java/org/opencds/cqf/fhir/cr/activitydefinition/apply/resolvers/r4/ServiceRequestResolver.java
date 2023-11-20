package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import java.util.Collections;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class ServiceRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ServiceRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ServiceRequest resolve(String subjectId, String encounterId, String practitionerId, String organizationId) {
        // status, intent, code, and subject are required
        var serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        serviceRequest.setIntent(
                activityDefinition.hasIntent()
                        ? ServiceRequest.ServiceRequestIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            serviceRequest.setInstantiatesCanonical(
                    Collections.singletonList(new CanonicalType(activityDefinition.getUrl())));
        }

        if (practitionerId != null) {
            serviceRequest.setRequester(new Reference(practitionerId));
        } else if (organizationId != null) {
            serviceRequest.setRequester(new Reference(organizationId));
        }

        if (activityDefinition.hasExtension()) {
            serviceRequest.setExtension(activityDefinition.getExtension());
        }

        // code can be set as a dynamicValue
        if (activityDefinition.hasCode()) {
            serviceRequest.setCode(activityDefinition.getCode());
        } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new FHIRException(MISSING_CODE_PROPERTY);
        }

        if (activityDefinition.hasBodySite()) {
            serviceRequest.setBodySite(activityDefinition.getBodySite());
        }

        if (activityDefinition.hasDoNotPerform()) {
            serviceRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
        }

        if (activityDefinition.hasProduct()) {
            throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return serviceRequest;
    }
}
