package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import java.util.List;

public class ElementHasCqfExtension {
  protected String patientId;
  protected IBaseBundle bundle;
  protected IBaseParameters parameters;
  protected LibraryEngine libraryEngine;
  protected String subjectType = "Patient";

  public ElementHasCqfExtension(
      String thePatientId,
      IBaseParameters theParameters,
      IBaseBundle theBundle,
      LibraryEngine theLibraryEngine
  ) {
    patientId = thePatientId;
    parameters = theParameters;
    bundle = theBundle;
    libraryEngine = theLibraryEngine;
  }

  public QuestionnaireItemComponent addProperties(ElementDefinition element, QuestionnaireItemComponent questionnaireItem) {
    final Expression expression = getExpression(element);
    final List<IBase> results = getExpressionResults(expression);
    // ROSIE TODO: If result is Resource, setValue as resource cast to Reference
    results.forEach(result -> questionnaireItem.addInitial().setValue((Type) result));
    return questionnaireItem;
  }

  protected final List<IBase> getExpressionResults(Expression expression) {
    return this.libraryEngine.getExpressionResult(
        this.patientId,
        subjectType,
        expression.getExpression(),
        expression.getLanguage(),
        expression.getReference(),
        parameters,
        this.bundle
    );
  }

  protected Expression getExpression(ElementDefinition element) {
    final Type type = element.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
    return (Expression) type;
  }

}