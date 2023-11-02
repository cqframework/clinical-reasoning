package org.opencds.cqf.fhir.cr.questionnaire.dstu3.generator.nestedquestionnaireitem;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.fhir.utility.Constants;

public class ElementHasDefaultValue {
    public QuestionnaireItemComponent addProperties(Type value, QuestionnaireItemComponent questionnaireItem) {
        questionnaireItem.setInitial(transformValue(value));
        questionnaireItem.addExtension(Constants.SDC_QUESTIONNAIRE_HIDDEN, new BooleanType(true));
        questionnaireItem.setReadOnly(true);
        return questionnaireItem;
    }
}
