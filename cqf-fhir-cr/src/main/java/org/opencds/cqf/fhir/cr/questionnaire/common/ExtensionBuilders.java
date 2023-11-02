package org.opencds.cqf.fhir.cr.questionnaire.common;

import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.utility.Constants;
import java.util.AbstractMap.SimpleEntry;

public class ExtensionBuilders {
    ExtensionBuilders() {}

    public static final SimpleEntry<String, IBaseReference> QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION =
        new SimpleEntry<>(
            Constants.QUESTIONNAIRE_RESPONSE_AUTHOR,
            new Reference(Constants.CQL_ENGINE_DEVICE)
        );

    public static SimpleEntry<String, IBaseReference> prepopulateSubjectExtension(String patientId) {
        return new SimpleEntry<>(
            Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT,
            new Reference(FHIRAllTypes.PATIENT.toCode() + "/" + patientId)
        );
    }

    public static SimpleEntry<String, IBaseReference> crmiMessagesExtension(String operationOutcomeId) {
        return new SimpleEntry<>(Constants.EXT_CRMI_MESSAGES, new Reference("#" + operationOutcomeId));
    }

    public static SimpleEntry<String, IBaseReference> dtrQuestionnaireResponseExtension(String questionnaireId) {
        return new SimpleEntry<>(
                Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE,
                new Reference("#" + questionnaireId));
    }

    public static org.hl7.fhir.r4.model.Extension buildR4(SimpleEntry<String, IBaseReference> entry) {
        return new org.hl7.fhir.r4.model.Extension(entry.getKey(), entry.getValue());
    }
    public static org.hl7.fhir.r5.model.Extension buildR5(SimpleEntry<String, IBaseReference> entry) {
        return new org.hl7.fhir.r5.model.Extension(entry.getKey(), entry.getValue());
    }
}
