package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CodeableReference;
import org.hl7.fhir.r5.model.MedicationRequest;
import org.hl7.fhir.r5.model.MedicationRequest.MedicationrequestStatus;
import org.hl7.fhir.r5.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.IApplyRequest;

public class MedicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public MedicationRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public MedicationRequest resolve(IApplyRequest request) {
        // intent, medication, and subject are required
        var medicationRequest = new MedicationRequest();
        medicationRequest.setStatus(MedicationrequestStatus.DRAFT);
        medicationRequest.setIntent(
                activityDefinition.hasIntent()
                        ? MedicationRequest.MedicationRequestIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasProductCodeableConcept()) {
            medicationRequest.setMedication(new CodeableReference(activityDefinition.getProductCodeableConcept()));
        } else if (activityDefinition.hasProductReference()) {
            medicationRequest.setMedication(new CodeableReference(activityDefinition.getProductReference()));
        } else {
            throw new FHIRException(MISSING_CODE_PROPERTY);
        }

        if (activityDefinition.hasDosage()) {
            activityDefinition.getDosage().forEach(medicationRequest::addDosageInstruction);
        }

        if (activityDefinition.hasDoNotPerform()) {
            medicationRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
        }

        if (activityDefinition.hasBodySite()) {
            throw new FHIRException(BODYSITE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasCode()) {
            throw new FHIRException(CODE_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        if (activityDefinition.hasQuantity()) {
            throw new FHIRException(QUANTITY_ERROR_PREAMBLE + activityDefinition.getKind());
        }

        return medicationRequest;
    }
}
