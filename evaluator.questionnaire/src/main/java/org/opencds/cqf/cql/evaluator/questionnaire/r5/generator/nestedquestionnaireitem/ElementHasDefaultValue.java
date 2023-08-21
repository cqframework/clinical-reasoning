package org.opencds.cqf.cql.evaluator.questionnaire.r5.generator.nestedquestionnaireitem;

import static org.opencds.cqf.cql.evaluator.questionnaire.r5.ItemValueTransformer.transformValue;

import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.cql.evaluator.fhir.Constants;

public class ElementHasDefaultValue {
  public QuestionnaireItemComponent addProperties(
      DataType value,
      QuestionnaireItemComponent questionnaireItem) {
    questionnaireItem.addInitial().setValue(transformValue(value));
    questionnaireItem.addExtension(Constants.SDC_QUESTIONNAIRE_HIDDEN, new BooleanType(true));
    questionnaireItem.setReadOnly(true);
    return questionnaireItem;
  }
}
