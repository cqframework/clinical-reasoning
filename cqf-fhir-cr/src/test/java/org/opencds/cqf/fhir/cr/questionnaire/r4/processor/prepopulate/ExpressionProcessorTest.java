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
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
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
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.cr.questionnaire.helpers.PrePopulateRequestHelpers;
import org.opencds.cqf.fhir.cr.questionnaire.r4.helpers.TestingHelper;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class ExpressionProcessorTest {
    @Mock
    protected LibraryEngine libraryEngine;

    @Spy
    @InjectMocks
    private final ExpressionProcessor fixture = new ExpressionProcessor();

    @Test
    void getInitialExpressionShouldReturnExpressionIfItemHasCqfExpressionExtension() {
        // setup
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final Expression expectedExpression = withExpression();
        final Extension extension = new Extension(Constants.CQF_EXPRESSION, expectedExpression);
        questionnaireItem.addExtension(extension);
        // execute
        final Expression actual = fixture.getInitialExpression(questionnaireItem);
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
        final Expression actual = fixture.getInitialExpression(questionnaireItem);
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
        final Expression actual = fixture.getInitialExpression(questionnaireItem);
        // validate
        assertNull(actual);
    }

    private Expression withExpression() {
        return new Expression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
    }

    @Test
    void getExpressionResultShouldReturnEmptyListIfNullParameters() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(libraryEngine);
        final Questionnaire questionnaire = TestingHelper.withQuestionnaire();
        // execute
        final List<IBase> actual = fixture.getExpressionResult(prePopulateRequest, null, "itemLinkId", questionnaire);
        // validate
        assertTrue(actual.isEmpty());
        verify(libraryEngine, never()).resolveExpression(any(), any(), any(), any());
    }

    @Test
    void getExpressionResultShouldReturnListOfIBaseResources() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(libraryEngine);
        final Expression expression = withExpression();
        final List<IBase> expected = List.of(new Bundle(), new Bundle(), new Bundle());
        final CqfExpression cqfExpression = new CqfExpression();
        final Questionnaire questionnaire = TestingHelper.withQuestionnaire();
        doReturn(cqfExpression).when(fixture).getCqfExpression(expression, questionnaire);
        doReturn(expected)
                .when(libraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
        // execute
        final List<IBase> actual =
                fixture.getExpressionResult(prePopulateRequest, expression, "itemLinkId", questionnaire);
        // validate
        assertEquals(expected, actual);
        verify(fixture).getCqfExpression(expression, questionnaire);
        verify(libraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
    }

    @Test
    void getExpressionResultShouldThrowResolveExpressionException() {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(libraryEngine);
        final Expression expression = withExpression();
        final CqfExpression cqfExpression = new CqfExpression();
        final Questionnaire questionnaire = TestingHelper.withQuestionnaire();
        doReturn(cqfExpression).when(fixture).getCqfExpression(expression, questionnaire);
        // TODO: VERIFY THIS IS THE ONLY KIND OF EXCEPTION THROWN -> THEN CAN DELETE ResolveExpressionException
        doThrow(new IllegalArgumentException("message"))
                .when(libraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
        // execute
        final ResolveExpressionException actual = assertThrows(
                ResolveExpressionException.class,
                () -> fixture.getExpressionResult(prePopulateRequest, expression, "itemLinkId", questionnaire));
        // validate
        assertEquals(
                "Error encountered evaluating expression (%subject.name.given[0]) for item (itemLinkId): message",
                actual.getMessage());
        verify(fixture).getCqfExpression(expression, questionnaire);
        verify(libraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
    }
}
