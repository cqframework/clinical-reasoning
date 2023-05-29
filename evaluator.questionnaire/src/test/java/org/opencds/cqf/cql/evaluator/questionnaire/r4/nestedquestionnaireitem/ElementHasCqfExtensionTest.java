package org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.TestingHelper;
import org.testng.Assert;

import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ElementHasCqfExtensionTest {
  final static String LINK_ID = "profileId";
  final static QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.GROUP;
  final static String PROFILE_TITLE = "Profile Title";
  final static String TYPE_CODE = "typeCode";
  final static String PATH_VALUE = "pathValue";
  final static String PATIENT_ID = "patientId";
  final static String SUBJECT_TYPE = "subectType";
  final static String EXPRESSION_EXPRESSION = "expressionExpression";
  final static String EXPRESSION_LANGUAGE = "expressionLanguage";
  final static String EXPRESSION_REFERENCE = "expressionReference";

  @Mock
  protected LibraryEngine libraryEngine;
  @InjectMocks
  @Spy
  private ElementHasCqfExtension myFixture;

  @BeforeEach
  void setUp() {
    myFixture.patientId = PATIENT_ID;
    myFixture.subjectType = SUBJECT_TYPE;
  }

  @Test
  void getExpressionResultsShouldCallLibraryEngine() {
    // setup
    final Expression expression = withExpression();
    final List<IBase> expected = withResultsAsResources();
    final Bundle bundle = withBundle();
    final Parameters parameters = withParameters();
    myFixture.parameters = parameters;
    myFixture.bundle = bundle;
    doReturn(expected).when(libraryEngine).getExpressionResult(
        PATIENT_ID,
        SUBJECT_TYPE,
        EXPRESSION_EXPRESSION,
        EXPRESSION_LANGUAGE,
        EXPRESSION_REFERENCE,
        parameters,
        bundle
    );
    // execute
    final List<IBase> actual = myFixture.getExpressionResults(expression);
    // validate
    verify(libraryEngine).getExpressionResult(PATIENT_ID,
        SUBJECT_TYPE,
        EXPRESSION_EXPRESSION,
        EXPRESSION_LANGUAGE,
        EXPRESSION_REFERENCE,
        parameters,
        bundle
    );
    Assert.assertEquals(actual, expected);
  }

  @Test
  void addPropertiesShouldAddAllPropertiesWhenLibraryEngineReturnsListOfTypes() {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    final ElementDefinition element = TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE);
    final Expression expression = withExpression();
    final List<IBase> results = withResultsAsTypes();
    doReturn(expression).when(myFixture).getExpression(element);
    doReturn(results).when(myFixture).getExpressionResults(expression);
    // execute
    final QuestionnaireItemComponent actual = myFixture.addProperties(questionnaireItem, element);
    // validate
    verify(myFixture).getExpression(element);
    verify(myFixture).getExpressionResults(expression);
    final List<QuestionnaireItemInitialComponent> initial = actual.getInitial();
    Assert.assertEquals(initial.size(), results.size());
    for (int i = 0; i < results.size(); i++) {
      Assert.assertEquals(initial.get(i).getValue(), results.get(i));
    }
  }

  @Test
  void addPropertiesShouldAddAllPropertiesWhenLibraryEngineReturnsListOfResources() {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    final ElementDefinition element = TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE);
    final Expression expression = withExpression();
    final List<IBase> results = withResultsAsResources();
    doReturn(expression).when(myFixture).getExpression(element);
    doReturn(results).when(myFixture).getExpressionResults(expression);
    // execute
    final QuestionnaireItemComponent actual = myFixture.addProperties(questionnaireItem, element);
    // validate
    verify(myFixture).getExpression(element);
    verify(myFixture).getExpressionResults(expression);
    final List<QuestionnaireItemInitialComponent> initial = actual.getInitial();
    Assert.assertEquals(initial.size(), results.size());
    for (int i = 0; i < results.size(); i++) {
      Assert.assertEquals(initial.get(i).getValue(), results.get(i));
    }
  }

  @Nonnull
  QuestionnaireItemComponent withQuestionnaireItemComponent() {
    return new QuestionnaireItemComponent()
        .setType(QUESTIONNAIRE_ITEM_TYPE)
        .setLinkId(LINK_ID)
        .setText(PROFILE_TITLE);
  }

  @Nonnull
  Resource withResourceValue() {
    final Patient patient = new Patient();
    patient.setId("patientId");
    return patient;
  }

  @Nonnull
  List<IBase> withResultsAsResources() {
    return List.of(
        withResourceValue(),
        withResourceValue(),
        withResourceValue()
    );
  }

  @Nonnull
  Type withTypeValue() {
    final CodeType codeType = new CodeType();
    codeType.setValue("someValue");
    return codeType;
  }

  @Nonnull
  List<IBase> withResultsAsTypes() {
    return List.of(
        withTypeValue(),
        withTypeValue(),
        withTypeValue()
    );
  }

  @Nonnull
  Parameters withParameters() {
    return new Parameters();
  }
  @Nonnull
  Expression withExpression() {
    final Expression expression = new Expression();
    expression.setExpression(EXPRESSION_EXPRESSION);
    expression.setLanguage(EXPRESSION_LANGUAGE);
    expression.setReference(EXPRESSION_REFERENCE);
    return expression;
  }

  @Nonnull
  Bundle withBundle() {
    return new Bundle();
  }
}
