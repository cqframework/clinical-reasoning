package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.CodeableReference;
import org.hl7.fhir.r5.model.MedicationRequest;
import org.hl7.fhir.r5.model.MedicationRequest.MedicationrequestStatus;
import org.hl7.fhir.r5.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class MedicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public MedicationRequestResolver(ActivityDefinition activityDefinition) {
        checkNotNull(activityDefinition);
        this.activityDefinition = activityDefinition;
    }

    @Override
    public MedicationRequest resolve(ICpgRequest request) {
        logger.debug(RESOLVE_MESSAGE, activityDefinition.getId(), activityDefinition.getKind());
        // intent, medication, and subject are required
        var medicationRequest = new MedicationRequest();
        medicationRequest.setStatus(MedicationrequestStatus.ACTIVE);
        medicationRequest.setIntent(
                activityDefinition.hasIntent()
                        ? MedicationRequest.MedicationRequestIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(request.getSubjectId()));

        if (request.hasEncounterId()) {
            medicationRequest.setEncounter(new Reference(request.getEncounterId()));
        }

        if (request.hasPractitionerId()) {
            medicationRequest.setRequester(new Reference(request.getPractitionerId()));
        }

        if (activityDefinition.hasProductCodeableConcept()) {
            medicationRequest.setMedication(new CodeableReference(activityDefinition.getProductCodeableConcept()));
        } else if (activityDefinition.hasProductReference()) {
            medicationRequest.setMedication(new CodeableReference(activityDefinition.getProductReference()));
        } else if (!activityDefinition.hasDynamicValue()) {
            throw new FHIRException(MISSING_PRODUCT_PROPERTY.formatted("MedicationRequest"));
        }

        if (activityDefinition.hasDosage()) {
            activityDefinition.getDosage().forEach(medicationRequest::addDosageInstruction);
        }

        if (activityDefinition.hasDoNotPerform()) {
            medicationRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
        }

        return medicationRequest;
    }
}
