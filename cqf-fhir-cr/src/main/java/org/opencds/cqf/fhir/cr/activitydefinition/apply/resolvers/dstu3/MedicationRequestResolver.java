package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.dstu3;

import java.util.Collections;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class MedicationRequestResolver extends BaseRequestResourceResolver {
    private final ActivityDefinition activityDefinition;

    public MedicationRequestResolver(ActivityDefinition activityDefinition) {
        this.activityDefinition = activityDefinition;
    }

    @Override
    public MedicationRequest resolve(ICpgRequest request) {
        // intent, medication, and subject are required
        var medicationRequest = new MedicationRequest();
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasUrl()) {
            medicationRequest.setDefinition(Collections.singletonList(new Reference(activityDefinition.getUrl())));
        }

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication(activityDefinition.getProduct());
        } else {
            throw new FHIRException(String.format(MISSING_PRODUCT_PROPERTY, "MedicationRequest"));
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction(activityDefinition.getDosage());
        }

        return medicationRequest;
    }
}
