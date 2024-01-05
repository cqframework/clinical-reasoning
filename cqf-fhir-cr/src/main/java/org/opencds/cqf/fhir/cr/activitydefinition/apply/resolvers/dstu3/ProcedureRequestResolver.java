package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import java.util.Collections;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyOperationRequest;

public class ProcedureRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public ProcedureRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public ProcedureRequest resolve(IApplyOperationRequest request) {
        // status, intent, code, and subject are required
        var procedureRequest = new ProcedureRequest();
        procedureRequest.setStatus(ProcedureRequest.ProcedureRequestStatus.DRAFT);
        procedureRequest.setIntent(ProcedureRequest.ProcedureRequestIntent.PROPOSAL);
        procedureRequest.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasUrl()) {
            procedureRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (request.hasPractitionerId()) {
            procedureRequest.setRequester(new ProcedureRequest.ProcedureRequestRequesterComponent()
                    .setAgent(new Reference(request.getPractitionerId())));
        } else if (request.hasOrganizationId()) {
            procedureRequest.setRequester(new ProcedureRequest.ProcedureRequestRequesterComponent()
                    .setAgent(new Reference(request.getOrganizationId())));
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
