package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class ServiceRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ServiceRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ServiceRequest resolve(ICpgRequest request) {
        // status, intent, code, and subject are required
        var serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        serviceRequest.setIntent(
                activityDefinition.hasIntent()
                        ? ServiceRequest.ServiceRequestIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasUrl()) {
            var url = activityDefinition.getUrl();
            url = activityDefinition.hasVersion() ? String.format("%s|%s", url, activityDefinition.getVersion()) : url;
            serviceRequest.setInstantiatesCanonical(Collections.singletonList(new CanonicalType(url)));
        }

        if (request.hasEncounterId()) {
            serviceRequest.setEncounter(new Reference(request.getEncounterId()));
        }

        if (request.hasPractitionerId()) {
            serviceRequest.setRequester(new Reference(request.getPractitionerId()));
        } else if (request.hasOrganizationId()) {
            serviceRequest.setRequester(new Reference(request.getOrganizationId()));
        }

        // code can be set as a dynamicValue
        if (activityDefinition.hasCode()) {
            serviceRequest.setCode(activityDefinition.getCode());
        } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new FHIRException(String.format(MISSING_CODE_PROPERTY, "ServiceRequest"));
        }

        if (activityDefinition.hasBodySite()) {
            serviceRequest.setBodySite(activityDefinition.getBodySite());
        }

        if (activityDefinition.hasDoNotPerform()) {
            serviceRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
        }

        return serviceRequest;
    }
}
