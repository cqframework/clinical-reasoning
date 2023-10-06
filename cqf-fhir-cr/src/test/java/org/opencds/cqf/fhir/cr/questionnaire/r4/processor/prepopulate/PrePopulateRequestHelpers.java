package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import java.util.List;
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

public class PrePopulateRequestHelpers {
    static final String QUESTIONNAIRE_ID = "questionnaireId";
    static final String PATIENT_ID = "patientId";
    static final String CQF_EXTENSION_URL = "url";
    static final List<QuestionnaireItemComponent> QUESTIONNAIRE_SUB_ITEMS = List.of(
            new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent());

    public static PrePopulateRequest withPrePopulateRequest(LibraryEngine theLibraryEngine) {
        final Questionnaire questionnaire = new Questionnaire();
        final CanonicalType type = new CanonicalType(CQF_EXTENSION_URL);
        final Extension extension = new Extension(Constants.CQF_LIBRARY, type);
        questionnaire.addExtension(extension);
        questionnaire.setId(QUESTIONNAIRE_ID);
        questionnaire.setItem(QUESTIONNAIRE_SUB_ITEMS);
        final IBaseParameters parameters = new Parameters();
        final IBaseBundle bundle = new Bundle();
        return new PrePopulateRequest(questionnaire, PATIENT_ID, parameters, bundle, theLibraryEngine);
    }
}
