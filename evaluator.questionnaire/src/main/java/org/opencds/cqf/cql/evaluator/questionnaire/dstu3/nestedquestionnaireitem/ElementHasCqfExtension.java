package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.nestedquestionnaireitem;

import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;

import java.util.List;

import static ca.uhn.fhir.util.ExtensionUtil.getExtensionByUrl;

public class ElementHasCqfExtension {
  protected String patientId;
  protected IBaseBundle bundle;
  protected IBaseParameters parameters;
  protected LibraryEngine libraryEngine;
  protected static final String subjectType = "Patient";
  public QuestionnaireItemComponent addProperties(QuestionnaireItemComponent questionnaireItem) {
    // TODO: is a list result valid here?
    final List<IBase> result = this.libraryEngine.getExpressionResult(
        this.patientId,
        subjectType,
        getExpression(questionnaireItem),
        getLanguage(questionnaireItem),
        getLibrary(questionnaireItem),
        parameters,
        this.bundle
    );
    questionnaireItem.setInitial((Type) result);
    return questionnaireItem;
  }

  protected String getExpression(QuestionnaireItemComponent questionnaireItem) {
    final IBaseExtension<?, ?> expressionExtension = getExtension(questionnaireItem, Constants.CQF_EXPRESSION);
    return expressionExtension.getValue().toString();
  }

  protected String getLanguage(QuestionnaireItemComponent questionnaireItem) {
    final IBaseExtension<?, ?> languageExtension = getExtension(questionnaireItem, Constants.CQF_EXPRESSION_LANGUAGE);
    return languageExtension.getValue().toString();
  }

  protected String getLibrary(QuestionnaireItemComponent questionnaireItem) {
    final IBaseExtension<?, ?> libraryExtension = getExtension(questionnaireItem, Constants.CQF_LIBRARY);
    return libraryExtension.getValue().toString();
  }

  IBaseExtension<?, ?> getExtension(QuestionnaireItemComponent questionnaireItem, String theExtensionUrl) {
    return getExtensionByUrl(questionnaireItem, theExtensionUrl);
  }
}
