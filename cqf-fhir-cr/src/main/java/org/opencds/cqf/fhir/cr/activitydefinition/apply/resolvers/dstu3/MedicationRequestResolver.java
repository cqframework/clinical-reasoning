package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import java.util.Collections;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;

public class MedicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public MedicationRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public MedicationRequest resolve(
            IIdType subjectId, IIdType encounterId, IIdType practitionerId, IIdType organizationId) {
        // intent, medication, and subject are required
        var medicationRequest = new MedicationRequest();
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(subjectId));

        if (activityDefinition.hasUrl()) {
            medicationRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication(activityDefinition.getProduct());
        } else {
            throw new FHIRException(MISSING_CODE_PROPERTY);
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction(activityDefinition.getDosage());
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
