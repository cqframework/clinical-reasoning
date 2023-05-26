package org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.cql.evaluator.fhir.Constants;

public class ElementIsFixedOrHasPattern {
  public QuestionnaireItemComponent addProperties(
      ElementDefinition element,
      QuestionnaireItemComponent questionnaireItem
  ) {
    questionnaireItem.addInitial().setValue(element.getFixedOrPattern());
    questionnaireItem.addExtension(Constants.SDC_QUESTIONNAIRE_HIDDEN, new BooleanType(true));
    questionnaireItem.setReadOnly(true);
    return questionnaireItem;
  }
}
