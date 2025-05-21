package org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Reference;
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
        medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        medicationRequest.setIntent(
                activityDefinition.hasIntent()
                        ? MedicationRequest.MedicationRequestIntent.fromCode(
                                activityDefinition.getIntent().toCode())
                        : MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(request.getSubjectId()));

        if (activityDefinition.hasUrl()) {
            medicationRequest.setInstantiatesCanonical(
                    Collections.singletonList(new CanonicalType(activityDefinition.getUrl()
                            + (activityDefinition.hasVersion()
                                    ? String.format("|%s", activityDefinition.getVersion())
                                    : ""))));
        }

        if (request.hasEncounterId()) {
            medicationRequest.setEncounter(new Reference(request.getEncounterId()));
        }

        if (request.hasPractitionerId()) {
            medicationRequest.setRequester(new Reference(request.getPractitionerId()));
        }

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication(activityDefinition.getProduct());
        } else if (!activityDefinition.hasDynamicValue()) {
            throw new FHIRException(String.format(MISSING_PRODUCT_PROPERTY, "MedicationRequest"));
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction(activityDefinition.getDosage());
        }

        if (activityDefinition.hasDoNotPerform()) {
            medicationRequest.setDoNotPerform(activityDefinition.getDoNotPerform());
        }

        return medicationRequest;
    }
}
