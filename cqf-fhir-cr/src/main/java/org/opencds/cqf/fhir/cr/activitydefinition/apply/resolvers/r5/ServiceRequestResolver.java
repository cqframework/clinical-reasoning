package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import java.util.Collections;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeableReference;
import org.hl7.fhir.r5.model.Enumerations.RequestIntent;
import org.hl7.fhir.r5.model.Enumerations.RequestStatus;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.ServiceRequest;
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
        serviceRequest.setStatus(RequestStatus.DRAFT);
        serviceRequest.setIntent(
                activityDefinition.hasIntent()
                        ? RequestIntent.fromCode(activityDefinition.getIntent().toCode())
                        : RequestIntent.ORDER);
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

        if (activityDefinition.hasCode()) {
            serviceRequest.setCode(new CodeableReference(activityDefinition.getCode()));
        }
        // code can be set as a dynamicValue
        else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
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
