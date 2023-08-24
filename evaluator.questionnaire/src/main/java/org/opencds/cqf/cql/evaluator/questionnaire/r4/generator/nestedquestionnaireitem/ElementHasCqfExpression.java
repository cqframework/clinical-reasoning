package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem;

import static org.opencds.cqf.cql.evaluator.questionnaire.r4.ItemValueTransformer.transformValue;

import java.util.List;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;

public class ElementHasCqfExpression {
  protected String patientId;
  protected IBaseBundle bundle;
  protected IBaseParameters parameters;
  protected LibraryEngine libraryEngine;

  public ElementHasCqfExpression(
      String thePatientId,
      IBaseParameters theParameters,
      IBaseBundle theBundle,
      LibraryEngine theLibraryEngine) {
    patientId = thePatientId;
    parameters = theParameters;
    bundle = theBundle;
    libraryEngine = theLibraryEngine;
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
    final Type type = transformValue((Type) result);
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
    final Type type = element.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
    return (Expression) type;
  }

}
