package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;
import java.util.List;

public class PrePopulateRequestHelpers {
    final static String QUESTIONNAIRE_ID = "questionnaireId";
    final static String PATIENT_ID = "patientId";
    final static String CQF_EXTENSION_URL = "url";
    final static List<QuestionnaireItemComponent> QUESTIONNAIRE_SUB_ITEMS = List.of(
        new QuestionnaireItemComponent(),
        new QuestionnaireItemComponent(),
        new QuestionnaireItemComponent()
    );
    protected static PrePopulateRequest withPrePopulateRequest(LibraryEngine theLibraryEngine) {
        final Questionnaire questionnaire = new Questionnaire();
        final CanonicalType type = new CanonicalType(CQF_EXTENSION_URL);
        final Extension extension = new Extension(Constants.CQF_LIBRARY, type);
        questionnaire.addExtension(extension);
        questionnaire.setId(QUESTIONNAIRE_ID);
        questionnaire.setItem(QUESTIONNAIRE_SUB_ITEMS);
        final IBaseParameters parameters = new Parameters();
        final IBaseBundle bundle = new Bundle();
        return new PrePopulateRequest(
            questionnaire,
            PATIENT_ID,
            parameters,
            bundle,
            theLibraryEngine
        );
    }
}
