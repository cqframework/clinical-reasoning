package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.ExpressionProcessorService;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class NewExpressionProcessorServiceTest {

    private final ExpressionProcessorService expressionProcessorService = ExpressionProcessorService.of();

    @Test
    public void testGetInitialExpressionWithCQFExpression() {
        // Arrange
        QuestionnaireItemComponent item = new QuestionnaireItemComponent();
        Expression expression = new Expression();
        item.addExtension()
            .setUrl(Constants.CQF_EXPRESSION)
            .setValue(expression);

        // Act
        Expression result = expressionProcessorService.getInitialExpression(item);

        // Assert
        assertEquals(expression, result);
    }

    @Test
    public void testGetInitialExpressionWithSDCQuestionnaireInitialExpression() {
        // Arrange
        QuestionnaireItemComponent item = new QuestionnaireItemComponent();
        Expression expression = new Expression();
        item.addExtension()
            .setUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)
            .setValue(expression);

        // Act
        Expression result = expressionProcessorService.getInitialExpression(item);

        // Assert
        assertEquals(expression, result);
    }

    @Test
    public void testGetInitialExpressionWithNoExpression() {
        // Arrange
        QuestionnaireItemComponent item = new QuestionnaireItemComponent();

        // Act
        Expression result = expressionProcessorService.getInitialExpression(item);

        // Assert
        assertNull(result);
    }

    @Test
    public void testGetExpressionResultWithNullExpression() throws ResolveExpressionException {
        // Arrange
        PrePopulateRequest prePopulateRequest = mock(PrePopulateRequest.class);

        // Act
        List<IBase> result = expressionProcessorService.getExpressionResult(prePopulateRequest, null, "itemLinkId");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetExpressionResultWithValidExpression() throws ResolveExpressionException {
        // Arrange
        PrePopulateRequest prePopulateRequest = mock(PrePopulateRequest.class);
        Expression expression = new Expression();
        when(prePopulateRequest.getLibraryEngine()).thenReturn(mock(LibraryEngine.class));
        when(prePopulateRequest.getPatientId()).thenReturn("patientId");
        when(prePopulateRequest.getParameters()).thenReturn(new Parameters());
        when(prePopulateRequest.getBundle()).thenReturn(mock(Bundle.class));

        // Act
        List<IBase> result = expressionProcessorService.getExpressionResult(prePopulateRequest, expression, "itemLinkId");

        // Assert
        assertNotNull(result);
        // Add more assertions based on your actual implementation and expected behavior.
    }

    // Add more test cases for other methods as needed.

}
