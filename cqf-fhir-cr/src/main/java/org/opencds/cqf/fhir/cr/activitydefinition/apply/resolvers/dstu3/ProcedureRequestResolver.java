package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import java.util.Collections;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class ProcedureRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ProcedureRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ProcedureRequest resolve(
            String subjectId, String encounterId, String practitionerId, String organizationId) {
        // status, intent, code, and subject are required
        var procedureRequest = new ProcedureRequest();
        procedureRequest.setStatus(ProcedureRequest.ProcedureRequestStatus.DRAFT);
        procedureRequest.setIntent(ProcedureRequest.ProcedureRequestIntent.PROPOSAL);
        procedureRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            procedureRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (practitionerId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent().setAgent(new Reference(practitionerId)));
        } else if (organizationId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent().setAgent(new Reference(organizationId)));
        }

        // code can be set as a dynamicValue
        if (activityDefinition.hasCode()) {
            procedureRequest.setCode(activityDefinition.getCode());
        } else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new FHIRException(MISSING_CODE_PROPERTY);
        }

        if (activityDefinition.hasBodySite()) {
            procedureRequest.setBodySite(activityDefinition.getBodySite());
        }

        if (activityDefinition.hasProduct()) {
            throw new FHIRException(PRODUCT_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new FHIRException(DOSAGE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return procedureRequest;
    }
}
