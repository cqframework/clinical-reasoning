package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.AbstractMap.SimpleEntry;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.opencds.cqf.fhir.utility.Constants;

@SuppressWarnings("rawtypes")
public class ExtensionBuilders {
    private ExtensionBuilders() {}

    public static final SimpleEntry<String, String> QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION =
            new SimpleEntry<>(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, Constants.CQL_ENGINE_DEVICE);

    public static SimpleEntry<String, String> prepopulateSubjectExtension(String patientCode, String patientId) {
        return new SimpleEntry<>(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT, patientCode + "/" + patientId);
    }

    public static SimpleEntry<String, String> crmiMessagesExtension(String operationOutcomeId) {
        return new SimpleEntry<>(Constants.EXT_CRMI_MESSAGES, "#" + operationOutcomeId);
    }

    public static SimpleEntry<String, String> dtrQuestionnaireResponseExtension(String questionnaireId) {
        return new SimpleEntry<>(Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE, "#" + questionnaireId);
    }

    public static SimpleEntry<String, String> pertainToGoalExtension(String goalId) {
        return new SimpleEntry<>(Constants.PERTAINS_TO_GOAL, "#" + goalId);
    }

    public static SimpleEntry<String, Boolean> sdcQuestionnaireHidden(Boolean value) {
        return new SimpleEntry<>(Constants.SDC_QUESTIONNAIRE_HIDDEN, value);
    }

    public static IBaseExtension buildReferenceExt(FhirVersionEnum fhirVersion, SimpleEntry<String, String> entry) {
        var value = buildReference(fhirVersion, entry.getValue());
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Extension(entry.getKey(), value);
            case R4:
                return new org.hl7.fhir.r4.model.Extension(entry.getKey(), value);
            case R5:
                return new org.hl7.fhir.r5.model.Extension(entry.getKey(), value);

            default:
                return null;
        }
    }

    public static IBaseReference buildReference(FhirVersionEnum fhirVersion, String reference) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Reference(reference);
            case R4:
                return new org.hl7.fhir.r4.model.Reference(reference);
            case R5:
                return new org.hl7.fhir.r5.model.Reference(reference);

            default:
                return null;
        }
    }

    public static IBaseReference buildReference(FhirVersionEnum fhirVersion, IAnyResource resource) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Reference(resource);
            case R4:
                return new org.hl7.fhir.r4.model.Reference(resource);
            case R5:
                return new org.hl7.fhir.r5.model.Reference(resource);

            default:
                return null;
        }
    }

    public static IBaseBooleanDatatype buildBooleanType(FhirVersionEnum fhirVersion, Boolean value) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.BooleanType(value);
            case R4:
                return new org.hl7.fhir.r4.model.BooleanType(value);
            case R5:
                return new org.hl7.fhir.r5.model.BooleanType(value);

            default:
                return null;
        }
    }

    public static IBaseExtension buildBooleanExt(FhirVersionEnum fhirVersion, SimpleEntry<String, Boolean> entry) {
        var value = buildBooleanType(fhirVersion, entry.getValue());
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Extension(entry.getKey(), value);
            case R4:
                return new org.hl7.fhir.r4.model.Extension(entry.getKey(), value);
            case R5:
                return new org.hl7.fhir.r5.model.Extension(entry.getKey(), value);

            default:
                return null;
        }
    }
}
