package org.opencds.cqf.fhir.cr.questionnaire.r5.generator.nestedquestionnaireitem;

import static org.opencds.cqf.fhir.cr.questionnaire.r5.ItemValueTransformer.transformValue;

import java.util.List;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Resource;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;

public class ElementHasCqfExpression {
  protected String patientId;
  protected IBaseBundle bundle;
  protected IBaseParameters parameters;
  protected LibraryEngine libraryEngine;

  public ElementHasCqfExpression(
      String patientId,
      IBaseParameters parameters,
      IBaseBundle bundle,
      LibraryEngine libraryEngine) {
    this.patientId = patientId;
    this.parameters = parameters;
    this.bundle = bundle;
    this.libraryEngine = libraryEngine;
  }

  public QuestionnaireItemComponent addProperties(ElementDefinition element,
      QuestionnaireItemComponent questionnaireItem) {
    final Expression expression = getExpression(element);
    final List<IBase> results = getExpressionResults(expression);
    results.forEach(result -> {
      if (Resource.class.isAssignableFrom(result.getClass())) {
        addResourceValue(result, questionnaireItem);
      } else {
        addTypeValue(result, questionnaireItem);
      }
    });
    return questionnaireItem;
  }

  void addResourceValue(IBase result, QuestionnaireItemComponent questionnaireItem) {
    final IAnyResource resource = (IAnyResource) result;
    final Reference reference = new Reference(resource);
    questionnaireItem.addInitial().setValue(reference);
  }

  void addTypeValue(IBase result, QuestionnaireItemComponent questionnaireItem) {
    final DataType type = transformValue((DataType) result);
    questionnaireItem.addInitial().setValue(type);
  }

  protected final List<IBase> getExpressionResults(Expression expression) {
    return libraryEngine.getExpressionResult(
        patientId,
        expression.getExpression(),
        expression.getLanguage(),
        expression.getReference(),
        parameters,
        bundle);
  }

  protected Expression getExpression(ElementDefinition element) {
    final DataType type = element.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
    return (Expression) type;
  }

}
