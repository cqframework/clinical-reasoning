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
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        // status, intent, code, and subject are required
        var serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(
                activityDefinition.hasIntent()
                        ? ServiceRequest.ServiceRequestIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasUrl()) {
            serviceRequest.setInstantiatesCanonical(
                    Collections.singletonList(new CanonicalType(activityDefinition.getUrl()
                            + (activityDefinition.hasVersion()
                                    ? "|%s".formatted(activityDefinition.getVersion())
                                    : ""))));
        }

        if (request.hasEncounterId()) {
            serviceRequest.setEncounter(new Reference(request.getEncounterId()));
        }

        if (request.hasPractitionerId()) {
            serviceRequest.setRequester(new Reference(request.getPractitionerId()));
        } else if (request.hasOrganizationId()) {
            serviceRequest.setRequester(new Reference(request.getOrganizationId()));
        }

        if (activityDefinition.hasCode()) {
            serviceRequest.setCode(activityDefinition.getCode());
        } else if (!activityDefinition.hasDynamicValue()) {
            throw new FHIRException(MISSING_CODE_PROPERTY.formatted("ServiceRequest"));
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
