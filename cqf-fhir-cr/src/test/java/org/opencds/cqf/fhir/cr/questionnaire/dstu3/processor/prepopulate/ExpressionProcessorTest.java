package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor.prepopulate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBase;
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
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class ExpressionProcessorTest {
    @Mock
    protected LibraryEngine libraryEngine;

    @Mock
    protected PrePopulateHelper prePopulateHelper;

    @Spy
    @InjectMocks
    private final ExpressionProcessor fixture = new ExpressionProcessor();

    @Test
    void getInitialExpressionShouldReturnExpressionIfItemHasCqfExpressionExtension() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        questionnaireItem.addExtension(withExtension(Constants.CQF_EXPRESSION));
        final CqfExpression expectedExpression = withExpression();
        doReturn(expectedExpression)
                .when(prePopulateHelper)
                .getExpressionByExtension(questionnaire, questionnaireItem, Constants.CQF_EXPRESSION);
        // execute
        final CqfExpression actual = fixture.getInitialExpression(questionnaire, questionnaireItem);
        // validate
        assertEquals(expectedExpression, actual);
        verify(prePopulateHelper).getExpressionByExtension(questionnaire, questionnaireItem, Constants.CQF_EXPRESSION);
    }

    private Extension withExtension(String url) {
        return new Extension(url, new Reference("value"));
    }

    @Test
    void getInitialExpressionShouldReturnExpressionIfItemHasSdcExpressionExtension() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        questionnaireItem.addExtension(withExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION));
        final CqfExpression expectedExpression = withExpression();
        doReturn(expectedExpression)
                .when(prePopulateHelper)
                .getExpressionByExtension(
                        questionnaire, questionnaireItem, Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
        // execute
        final CqfExpression actual = fixture.getInitialExpression(questionnaire, questionnaireItem);
        // validate
        assertEquals(expectedExpression, actual);
        verify(prePopulateHelper)
                .getExpressionByExtension(
                        questionnaire, questionnaireItem, Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
    }

    @Test
    void getInitialExpressionShouldReturnNullIfNoExpressionExtension() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        questionnaireItem.addExtension(withExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
        // execute
        final CqfExpression actual = fixture.getInitialExpression(questionnaire, questionnaireItem);
        // validate
        assertNull(actual);
        verify(prePopulateHelper, never())
                .getExpressionByExtension(
                        any(Questionnaire.class), any(QuestionnaireItemComponent.class), any(String.class));
    }

    private CqfExpression withExpression() {
        return new CqfExpression("language", "expression", "libraryUrl");
    }

    @Test
    void getExpressionResultShouldReturnListOfIBaseResources() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(libraryEngine);
        final List<IBase> expected = List.of(new Bundle(), new Bundle(), new Bundle());
        final CqfExpression cqfExpression = new CqfExpression();
        doReturn(expected)
                .when(libraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
        // execute
        final List<IBase> actual = fixture.getExpressionResult(prePopulateRequest, cqfExpression, "itemLinkId");
        // validate
        assertEquals(expected, actual);
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
        final CqfExpression cqfExpression = withExpression();
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
                () -> fixture.getExpressionResult(prePopulateRequest, cqfExpression, "itemLinkId"));
        // validate
        assertEquals(
                "Error encountered evaluating expression (expression) for item (itemLinkId): message",
                actual.getMessage());
        verify(libraryEngine)
                .resolveExpression(
                        prePopulateRequest.getPatientId(),
                        cqfExpression,
                        prePopulateRequest.getParameters(),
                        prePopulateRequest.getBundle());
    }
}
