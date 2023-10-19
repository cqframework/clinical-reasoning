package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class ExpressionProcessorServiceTest {
    @Mock
    protected LibraryEngine myLibraryEngine;

    @Spy
    @InjectMocks
    private final ExpressionProcessorService myFixture = new ExpressionProcessorService();

    @Test
    void getInitialExpressionShouldReturnExpressionIfItemHasCqfExpressionExtension() {
        // setup
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final Expression expectedExpression = withExpression();
        final Extension extension = new Extension(Constants.CQF_EXPRESSION, expectedExpression);
        questionnaireItem.addExtension(extension);
        // execute
        final Expression actual = myFixture.getInitialExpression(questionnaireItem);
        // validate
        assertEquals(expectedExpression, actual);
    }

    @Test
    void getInitialExpressionShouldReturnExpressionIfItemHasSdcExpressionExtension() {
        // setup
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final Expression expectedExpression = withExpression();
        final Extension extension = new Extension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION, expectedExpression);
        questionnaireItem.addExtension(extension);
        // execute
        final Expression actual = myFixture.getInitialExpression(questionnaireItem);
        // validate
        assertEquals(expectedExpression, actual);
    }

    @Test
    void getInitialExpressionShouldReturnNullIfNoExpressionExtension() {
        // setup
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final Expression expectedExpression = withExpression();
        final Extension extension = new Extension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, expectedExpression);
        questionnaireItem.addExtension(extension);
        // execute
        final Expression actual = myFixture.getInitialExpression(questionnaireItem);
        // validate
        assertNull(actual);
    }

    private Expression withExpression() {
        return new Expression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
    }

    @Test
    void getExpressionResultShouldReturnEmptyListIfNullParameters() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = withPrePopulateRequest();
        // execute
        final List<IBase> actual = myFixture.getExpressionResult(prePopulateRequest, null, "itemLinkId");
        // validate
        assertTrue(actual.isEmpty());
        verify(myLibraryEngine, never()).resolveExpression(any(), any(), any(), any());
    }

    @Test
    void getExpressionResultShouldReturnListOfIBaseResources() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = withPrePopulateRequest();
        final Expression expression = withExpression();
        final List<IBase> expected = List.of(new Bundle(), new Bundle(), new Bundle());
        final CqfExpression cqfExpression = new CqfExpression();
        doReturn(cqfExpression).when(myFixture).getCqfExpression(prePopulateRequest, expression);
        doReturn(expected)
                .when(myLibraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
        // execute
        final List<IBase> actual = myFixture.getExpressionResult(prePopulateRequest, expression, "itemLinkId");
        // validate
        assertEquals(expected, actual);
        verify(myFixture).getCqfExpression(prePopulateRequest, expression);
        verify(myLibraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
    }

    @Test
    void getExpressionResultShouldThrowResolveExpressionException() {
        // setup
        final PrePopulateRequest prePopulateRequest = withPrePopulateRequest();
        final Expression expression = withExpression();
        final CqfExpression cqfExpression = new CqfExpression();
        doReturn(cqfExpression).when(myFixture).getCqfExpression(prePopulateRequest, expression);
        // TODO: VERIFY THIS IS THE ONLY KIND OF EXCEPTION THROWN -> THEN CAN DELETE ResolveExpressionException
        doThrow(new IllegalArgumentException("message"))
                .when(myLibraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
        // execute
        final ResolveExpressionException actual = assertThrows(
                ResolveExpressionException.class,
                () -> myFixture.getExpressionResult(prePopulateRequest, expression, "itemLinkId"));
        // validate
        assertEquals(
                "Error encountered evaluating expression (%subject.name.given[0]) for item (itemLinkId): message",
                actual.getMessage());
        verify(myFixture).getCqfExpression(prePopulateRequest, expression);
        verify(myLibraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
    }

    private PrePopulateRequest withPrePopulateRequest() {
        final Questionnaire questionnaire = new Questionnaire();
        final CanonicalType type = new CanonicalType("url");
        final Extension extension = new Extension(Constants.CQF_LIBRARY, type);
        questionnaire.addExtension(extension);
        final String patientId = "patientId";
        final IBaseParameters parameters = new Parameters();
        final IBaseBundle bundle = new Bundle();
        return new PrePopulateRequest(questionnaire, patientId, parameters, bundle, myLibraryEngine);
    }
}
