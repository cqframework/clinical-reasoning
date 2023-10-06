package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils;

import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.utility.Constants;

public class ExtensionBuilders {
    ExtensionBuilders() {}

    public static Extension prepopulateSubjectExtension(String patientId) {
        return new Extension(
                Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT,
                new Reference(FHIRAllTypes.PATIENT.toCode() + "/" + patientId));
    }

    public static Extension questionnaireResponseAuthorExtension() {
        return new Extension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, new Reference(Constants.CQL_ENGINE_DEVICE));
    }

    public static Extension crmiMessagesExtension(String operationOutcomeId) {
        return new Extension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + operationOutcomeId));
    }

    public static Extension dtrQuestionnaireResponseExtension(Questionnaire thePrePopulatedQuestionnaire) {
        return new Extension(
                Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE,
                new Reference("#" + thePrePopulatedQuestionnaire.getIdPart()));
    }
}
