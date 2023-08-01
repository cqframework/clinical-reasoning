package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem;

import static org.opencds.cqf.cql.evaluator.questionnaire.r4.ItemValueTransformer.transformValue;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.evaluator.fhir.Constants;

public class ElementHasDefaultValue {
  public QuestionnaireItemComponent addProperties(
      Type value,
      QuestionnaireItemComponent questionnaireItem) {
    questionnaireItem.addInitial().setValue(transformValue(value));
    questionnaireItem.addExtension(Constants.SDC_QUESTIONNAIRE_HIDDEN, new BooleanType(true));
    questionnaireItem.setReadOnly(true);
    return questionnaireItem;
  }
}
