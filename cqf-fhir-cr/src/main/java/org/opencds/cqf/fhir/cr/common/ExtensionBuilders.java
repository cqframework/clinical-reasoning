package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.AbstractMap.SimpleEntry;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.opencds.cqf.fhir.utility.Constants;

public class ExtensionBuilders {
    private ExtensionBuilders() {}

    public static final SimpleEntry<String, String> QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION =
            new SimpleEntry<>(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, Constants.CQL_ENGINE_DEVICE);

    public static SimpleEntry<String, String> prepopulateSubjectExtension(String patientCode, String patientId) {
        return new SimpleEntry<>(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT, patientCode + "/" + patientId);
    }

    public static SimpleEntry<String, String> cqfMessagesExtension(String operationOutcomeId) {
        return new SimpleEntry<>(Constants.CQF_MESSAGES, operationOutcomeId);
    }

    public static SimpleEntry<String, String> dtrQuestionnaireResponseExtension(String questionnaireId) {
        return new SimpleEntry<>(Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE, questionnaireId);
    }

    public static SimpleEntry<String, String> pertainToGoalExtension(String goalId) {
        return new SimpleEntry<>(Constants.PERTAINS_TO_GOAL, goalId);
    }

    public static SimpleEntry<String, Boolean> sdcQuestionnaireHidden(Boolean value) {
        return new SimpleEntry<>(Constants.SDC_QUESTIONNAIRE_HIDDEN, value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseExtension<?, ?>> T buildReferenceExt(
            FhirVersionEnum fhirVersion, SimpleEntry<String, String> entry, Boolean isContained) {
        var value = buildReference(fhirVersion, entry.getValue(), isContained);
        return switch (fhirVersion) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.Extension(entry.getKey(), value);
            case R4 -> (T) new org.hl7.fhir.r4.model.Extension(entry.getKey(), value);
            case R5 -> (T) new org.hl7.fhir.r5.model.Extension(entry.getKey(), value);
            default -> null;
        };
    }

    public static IBaseReference buildReference(FhirVersionEnum fhirVersion, String id) {
        return buildReference(fhirVersion, id, false);
    }

    public static IBaseReference buildReference(FhirVersionEnum fhirVersion, String id, Boolean isContained) {
        var reference = Boolean.TRUE.equals(isContained) ? "#".concat(id) : id;
        return switch (fhirVersion) {
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Reference(reference);
            case R4 -> new org.hl7.fhir.r4.model.Reference(reference);
            case R5 -> new org.hl7.fhir.r5.model.Reference(reference);
            default -> null;
        };
    }

    public static IBaseBooleanDatatype buildBooleanType(FhirVersionEnum fhirVersion, Boolean value) {
        return switch (fhirVersion) {
            case DSTU3 -> new org.hl7.fhir.dstu3.model.BooleanType(value);
            case R4 -> new org.hl7.fhir.r4.model.BooleanType(value);
            case R5 -> new org.hl7.fhir.r5.model.BooleanType(value);
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseExtension<?, ?>> T buildBooleanExt(
            FhirVersionEnum fhirVersion, SimpleEntry<String, Boolean> entry) {
        var value = buildBooleanType(fhirVersion, entry.getValue());
        return switch (fhirVersion) {
            case DSTU3 -> (T) new org.hl7.fhir.dstu3.model.Extension(entry.getKey(), value);
            case R4 -> (T) new org.hl7.fhir.r4.model.Extension(entry.getKey(), value);
            case R5 -> (T) new org.hl7.fhir.r5.model.Extension(entry.getKey(), value);
            default -> null;
        };
    }
}
