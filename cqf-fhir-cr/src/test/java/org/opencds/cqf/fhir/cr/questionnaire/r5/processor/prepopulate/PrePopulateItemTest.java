package org.opencds.cqf.fhir.cr.questionnaire.r5.processor.prepopulate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.cr.questionnaire.helpers.PrePopulateRequestHelpers;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class PrePopulateItemTest {
    @Mock
    private ExpressionProcessor expressionProcessorService;

    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    @InjectMocks
    private PrePopulateItem fixture;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(expressionProcessorService);
        verifyNoMoreInteractions(libraryEngine);
    }

    @Test
    void processItemShouldReturnQuestionnaireItemComponent() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(libraryEngine);
        final QuestionnaireItemComponent originalQuestionnaireItemComponent = new QuestionnaireItemComponent();
        final QuestionnaireItemComponent populatedQuestionnaireItemComponent = new QuestionnaireItemComponent();
        final Questionnaire questionnaire = new Questionnaire();
        final List<IBase> expressionResults = withExpressionResults();
        doReturn(populatedQuestionnaireItemComponent)
                .when(fixture)
                .copyQuestionnaireItem(originalQuestionnaireItemComponent);
        doReturn(expressionResults)
                .when(fixture)
                .getExpressionResults(prePopulateRequest, questionnaire, populatedQuestionnaireItemComponent);
        // execute
        final QuestionnaireItemComponent actual =
                fixture.processItem(prePopulateRequest, originalQuestionnaireItemComponent, questionnaire);
        // validate
        verify(fixture).getExpressionResults(prePopulateRequest, questionnaire, populatedQuestionnaireItemComponent);
        verify(fixture).copyQuestionnaireItem(originalQuestionnaireItemComponent);
        final List<Extension> extensions = actual.getExtensionsByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR);
        assertEquals(3, extensions.size());
        final List<QuestionnaireItemInitialComponent> initials = actual.getInitial();
        assertEquals(3, initials.size());
        for (int i = 0; i < initials.size(); i++) {
            assertEquals(expressionResults.get(i), initials.get(i).getValue());
        }
    }

    private List<IBase> withExpressionResults() {
        return List.of(new StringType("string type value"), new BooleanType(true), new IntegerType(3));
    }

    @Test
    void getExpressionResultsShouldReturnEmptyListIfInitialExpressionIsNull() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(libraryEngine);
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(null).when(expressionProcessorService).getInitialExpression(questionnaireItemComponent);
        // execute
        final List<IBase> actual =
                fixture.getExpressionResults(prePopulateRequest, questionnaire, questionnaireItemComponent);
        // validate
        assertTrue(actual.isEmpty());
        verify(expressionProcessorService).getInitialExpression(questionnaireItemComponent);
        verify(expressionProcessorService, never()).getExpressionResult(any(), any(), any(), any());
    }

    @Test
    void getExpressionResultsShouldReturnListOfResourcesIfInitialExpressionIsNotNull()
            throws ResolveExpressionException {
        // setup
        final List<IBase> expected = withExpressionResults();
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(libraryEngine);
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        questionnaireItemComponent.setLinkId("linkId");
        final Expression expression = withExpression();
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(expression).when(expressionProcessorService).getInitialExpression(questionnaireItemComponent);
        doReturn(expected)
                .when(expressionProcessorService)
                .getExpressionResult(prePopulateRequest, expression, "linkId", questionnaire);
        // execute
        final List<IBase> actual =
                fixture.getExpressionResults(prePopulateRequest, questionnaire, questionnaireItemComponent);
        // validate
        assertEquals(expected, actual);
        verify(expressionProcessorService).getInitialExpression(questionnaireItemComponent);
        verify(expressionProcessorService).getInitialExpression(questionnaireItemComponent);
    }

    private Expression withExpression() {
        return new Expression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
    }
}
