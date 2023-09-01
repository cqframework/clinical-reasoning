package org.opencds.cqf.fhir.cr.questionnaire.r5.generator.nestedquestionnaireitem;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.cr.questionnaire.r5.helpers.TestingHelper.withQuestionnaireItemComponent;

import java.util.List;
import javax.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.r5.helpers.TestingHelper;

@ExtendWith(MockitoExtension.class)
class ElementHasCqfExpressionTest {
    static final String TYPE_CODE = "typeCode";
    static final String PATH_VALUE = "pathValue";
    static final String PATIENT_ID = "patientId";
    static final String EXPRESSION_EXPRESSION = "expressionExpression";
    static final String EXPRESSION_LANGUAGE = "expressionLanguage";
    static final String EXPRESSION_REFERENCE = "expressionReference";

    @Mock
    protected LibraryEngine libraryEngine;

    @InjectMocks
    @Spy
    private ElementHasCqfExpression myFixture;

    @BeforeEach
    void setUp() {
        myFixture.patientId = PATIENT_ID;
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
        doReturn(expected)
                .when(libraryEngine)
                .getExpressionResult(
                        PATIENT_ID,
                        EXPRESSION_EXPRESSION,
                        EXPRESSION_LANGUAGE,
                        EXPRESSION_REFERENCE,
                        parameters,
                        bundle);
        // execute
        final List<IBase> actual = myFixture.getExpressionResults(expression);
        // validate
        verify(libraryEngine)
                .getExpressionResult(
                        PATIENT_ID,
                        EXPRESSION_EXPRESSION,
                        EXPRESSION_LANGUAGE,
                        EXPRESSION_REFERENCE,
                        parameters,
                        bundle);
        Assertions.assertEquals(actual, expected);
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
        final QuestionnaireItemComponent actual = myFixture.addProperties(element, questionnaireItem);
        // validate
        verify(myFixture).getExpression(element);
        verify(myFixture).getExpressionResults(expression);
        final List<QuestionnaireItemInitialComponent> initial = actual.getInitial();
        Assertions.assertEquals(initial.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            Assertions.assertEquals(initial.get(i).getValue(), results.get(i));
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
        final QuestionnaireItemComponent actual = myFixture.addProperties(element, questionnaireItem);
        // validate
        verify(myFixture).getExpression(element);
        verify(myFixture).getExpressionResults(expression);
        final List<QuestionnaireItemInitialComponent> initial = actual.getInitial();
        Assertions.assertEquals(initial.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            final IBase expected = results.get(i);
            final DataType actualType = initial.get(i).getValue();
            final Reference actualRef = (Reference) actualType;
            Assertions.assertEquals(actualRef.getResource(), expected);
        }
    }

    @Test
    void addTypeValueShouldAddValueToQuestionnaireItem() {
        // setup
        final IBase result = withTypeValue();
        final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
        // execute
        myFixture.addTypeValue(result, questionnaireItem);
        // validate
        Assertions.assertTrue(DataType.class.isAssignableFrom(result.getClass()));
        Assertions.assertFalse(questionnaireItem.getInitial().isEmpty());
        final QuestionnaireItemInitialComponent actual =
                questionnaireItem.getInitial().get(0);
        Assertions.assertNotNull(actual.getValue());
        final DataType type = actual.getValue();
        Assertions.assertEquals(type, result);
    }

    @Test
    void addResourceValueShouldAddValueToQuestionnaireItem() {
        // setup
        final IBase result = withResourceValue();
        final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
        // execute
        myFixture.addResourceValue(result, questionnaireItem);
        // validate
        Assertions.assertTrue(Resource.class.isAssignableFrom(result.getClass()));
        Assertions.assertFalse(questionnaireItem.getInitial().isEmpty());
        final QuestionnaireItemInitialComponent actual =
                questionnaireItem.getInitial().get(0);
        Assertions.assertNotNull(actual.getValue());
        final DataType type = actual.getValue();
        Assertions.assertTrue(Reference.class.isAssignableFrom(type.getClass()));
        final Reference typeAsRef = (Reference) type;
        final IBaseResource resource = typeAsRef.getResource();
        Assertions.assertEquals(resource, result);
    }

    @Nonnull
    Resource withResourceValue() {
        final Patient patient = new Patient();
        patient.setId("patientId");
        return patient;
    }

    @Nonnull
    List<IBase> withResultsAsResources() {
        return List.of(withResourceValue(), withResourceValue(), withResourceValue());
    }

    @Nonnull
    DataType withTypeValue() {
        final CodeType codeType = new CodeType();
        codeType.setValue("someValue");
        return codeType;
    }

    @Nonnull
    List<IBase> withResultsAsTypes() {
        return List.of(withTypeValue(), withTypeValue(), withTypeValue());
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
