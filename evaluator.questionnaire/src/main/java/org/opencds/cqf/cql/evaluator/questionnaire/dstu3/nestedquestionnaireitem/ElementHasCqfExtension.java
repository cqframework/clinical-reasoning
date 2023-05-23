package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.nestedquestionnaireitem;

import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;

import static ca.uhn.fhir.util.ExtensionUtil.getExtensionByUrl;

public class ElementHasCqfExtension {
  protected String patientId;
  protected IBaseBundle bundle;
  protected IBaseParameters parameters;
  protected LibraryEngine libraryEngine;
  protected static final String subjectType = "Patient";
  public QuestionnaireItemComponent addProperties(QuestionnaireItemComponent questionnaireItem) {
    var expressionExtension = getExtensionByUrl(questionnaireItem, Constants.CQF_EXPRESSION);
    var expression = expressionExtension.getValue().toString();
    var languageExtension = getExtensionByUrl(questionnaireItem, Constants.CQF_EXPRESSION_LANGUAGE);
    var language = languageExtension.getValue().toString();
    var libraryExtension = getExtensionByUrl(questionnaireItem, Constants.CQF_LIBRARY);
    var library = libraryExtension.getValue().toString();
    // TODO: is a list result valid here?
    var result = this.libraryEngine.getExpressionResult(this.patientId, subjectType, expression, language, library, parameters, this.bundle);
    questionnaireItem.setInitial((Type) result);
    return questionnaireItem;
  }
}
