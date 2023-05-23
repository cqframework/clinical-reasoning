package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.nestedquestionnaireitem;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.cql.evaluator.fhir.Constants;

public class ElementIsFixedOrHasPattern {
  public QuestionnaireItemComponent addProperties(
      ElementDefinition element,
      QuestionnaireItemComponent questionnaireItem
  ) {
    questionnaireItem.setInitial(element.hasFixed() ? element.getFixed() : element.getPattern());
    questionnaireItem.addExtension(Constants.SDC_QUESTIONNAIRE_HIDDEN, new BooleanType(true));
    questionnaireItem.setReadOnly(true);
    return questionnaireItem;
  }
}
