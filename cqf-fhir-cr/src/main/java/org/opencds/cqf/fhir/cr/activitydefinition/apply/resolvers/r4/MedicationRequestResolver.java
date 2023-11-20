package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import java.util.Collections;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class MedicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public MedicationRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public MedicationRequest resolve(
            String subjectId, String encounterId, String practitionerId, String organizationId) {
        // intent, medication, and subject are required
        var medicationRequest = new MedicationRequest();
        medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.DRAFT);
        medicationRequest.setIntent(
                activityDefinition.hasIntent()
                        ? MedicationRequest.MedicationRequestIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            medicationRequest.setInstantiatesCanonical(
                    Collections.singletonList(new CanonicalType(activityDefinition.getUrl())));
        }

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication(activityDefinition.getProduct());
        } else {
            throw new FHIRException(MISSING_CODE_PROPERTY);
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction(activityDefinition.getDosage());
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
