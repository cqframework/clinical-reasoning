package org.opencds.cqf.fhir.cr.questionnaire.helpers;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.utility.Constants;
import java.util.List;

public class QuestionnaireHelpers {
    public static final String QUESTIONNAIRE_ID = "questionnaireId";
    public static final String CQF_EXTENSION_URL = "url";
    public static Questionnaire withQuestionnaire() {
        final Questionnaire questionnaire = new Questionnaire();
        final CanonicalType type = new CanonicalType(CQF_EXTENSION_URL);
        final Extension extension = new Extension(Constants.CQF_LIBRARY, type);
        questionnaire.addExtension(extension);
        questionnaire.setId(QUESTIONNAIRE_ID);
        questionnaire.setItem(QUESTIONNAIRE_SUB_ITEMS);
        return questionnaire;
    }
    public static final List<QuestionnaireItemComponent> QUESTIONNAIRE_SUB_ITEMS = List.of(
        new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent());
}
