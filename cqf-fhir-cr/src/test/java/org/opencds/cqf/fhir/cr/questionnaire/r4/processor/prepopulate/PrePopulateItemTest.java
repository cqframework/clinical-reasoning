package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class PrePopulateItemTest {
    @Mock
    private ExpressionProcessorService myExpressionProcessorService;

    @Mock
    private LibraryEngine myLibraryEngine;

    @Spy
    @InjectMocks
    private PrePopulateItem myFixture;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(myExpressionProcessorService);
        verifyNoMoreInteractions(myLibraryEngine);
    }

    @Test
    void processItemShouldReturnQuestionnaireItemComponent() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent originalQuestionnaireItemComponent = new QuestionnaireItemComponent();
        final QuestionnaireItemComponent populatedQuestionnaireItemComponent = new QuestionnaireItemComponent();
        final List<IBase> expressionResults = withExpressionResults();
        doReturn(populatedQuestionnaireItemComponent)
                .when(myFixture)
                .copyQuestionnaireItem(originalQuestionnaireItemComponent);
        doReturn(expressionResults)
                .when(myFixture)
                .getExpressionResults(prePopulateRequest, populatedQuestionnaireItemComponent);
        // execute
        final QuestionnaireItemComponent actual =
                myFixture.processItem(prePopulateRequest, originalQuestionnaireItemComponent);
        // validate
        verify(myFixture).getExpressionResults(prePopulateRequest, populatedQuestionnaireItemComponent);
        verify(myFixture).copyQuestionnaireItem(originalQuestionnaireItemComponent);
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
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        doReturn(null).when(myExpressionProcessorService).getInitialExpression(questionnaireItemComponent);
        // execute
        final List<IBase> actual = myFixture.getExpressionResults(prePopulateRequest, questionnaireItemComponent);
        // validate
        assertTrue(actual.isEmpty());
        verify(myExpressionProcessorService).getInitialExpression(questionnaireItemComponent);
        verify(myExpressionProcessorService, never()).getExpressionResult(any(), any(), any());
    }

    @Test
    void getExpressionResultsShouldReturnListOfResourcesIfInitialExpressionIsNotNull()
            throws ResolveExpressionException {
        // setup
        final List<IBase> expected = withExpressionResults();
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        questionnaireItemComponent.setLinkId("linkId");
        final Expression expression = withExpression();
        doReturn(expression).when(myExpressionProcessorService).getInitialExpression(questionnaireItemComponent);
        doReturn(expected)
                .when(myExpressionProcessorService)
                .getExpressionResult(prePopulateRequest, expression, "linkId");
        // execute
        final List<IBase> actual = myFixture.getExpressionResults(prePopulateRequest, questionnaireItemComponent);
        // validate
        assertEquals(expected, actual);
        verify(myExpressionProcessorService).getInitialExpression(questionnaireItemComponent);
        verify(myExpressionProcessorService).getInitialExpression(questionnaireItemComponent);
    }

    private Expression withExpression() {
        return new Expression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
    }
}
