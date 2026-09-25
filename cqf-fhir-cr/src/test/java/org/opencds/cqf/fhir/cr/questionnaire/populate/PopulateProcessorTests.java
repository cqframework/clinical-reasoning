package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class PopulateProcessorTests {
    @Mock
    private IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Mock
    private ExpressionProcessor expressionProcessor;

    @Mock
    private ItemProcessor itemProcessor;

    @Spy
    private PopulateProcessor fixture;

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
        fixture = spy(new PopulateProcessor(itemProcessor, expressionProcessor));
    }

    private void assertContainedOperationOutcome(
            PopulateRequest request, IBaseResource actual, IBaseOperationOutcome expectedOperationOutcome) {
        final var operationOutcome = getContainedByResourceType(
                request, IAdapterFactory.createAdapterForResource(actual), "OperationOutcome");
        assertEquals(expectedOperationOutcome, operationOutcome);
    }

    private IBaseResource getContainedByResourceType(
            PopulateRequest request, IResourceAdapter actual, String resourceType) {
        return actual.getContained().stream()
                .filter(c -> c.fhirType().equals(resourceType))
                .findFirst()
                .orElse(null);
    }

    @Test
    void resolveOperationOutcomeShouldAddOperationOutcomeIfHasIssues() {
        // setup
        final var operationOutcome = withOperationOutcomeWithIssue();
        final var questionnaire = new Questionnaire();
        var adapter = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createQuestionnaire(questionnaire);
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        request.resolveOperationOutcome(adapter);
        // validate
        assertContainedOperationOutcome(request, questionnaire, operationOutcome);
    }

    @Test
    void resolveOperationOutcomeShouldNotAddOperationOutcomeIfHasNoIssues() {
        // setup
        final var operationOutcome = new OperationOutcome();
        final var questionnaire = new Questionnaire();
        var adapter = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createQuestionnaire(questionnaire);
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        request.resolveOperationOutcome(adapter);
        // validate
        assertContainedOperationOutcome(request, questionnaire, null);
    }

    private OperationOutcome withOperationOutcomeWithIssue() {
        final var operationOutcome = new OperationOutcome();
        operationOutcome
                .addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION);
        return operationOutcome;
    }

    @Test
    void testGetVariablesReturnsObject() {
        var questionnaire = new Questionnaire();
        var expression = new Expression()
                .setLanguage("text/cql-expression")
                .setExpression("test")
                .setName("testName");
        questionnaire.addExtension(Constants.VARIABLE_EXTENSION, expression);
        var expectedResponse = new StringType("test");
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        doReturn(List.of(expectedResponse))
                .when(expressionProcessor)
                .getExpressionResult(any(), any(), eq(null), eq(null));
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var actual = fixture.getVariables(request, questionnaire);
        assertNotNull(actual);
        assertEquals(expectedResponse, actual.get("testName"));
    }

    @Test
    void testGetVariablesReturnsList() {
        var questionnaire = new Questionnaire();
        var expression = new Expression()
                .setLanguage("text/cql-expression")
                .setExpression("test")
                .setName("testName");
        questionnaire.addExtension(Constants.VARIABLE_EXTENSION, expression);
        var expectedResponse = List.of(new StringType("test1"), new StringType("test2"));
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        doReturn(expectedResponse).when(expressionProcessor).getExpressionResult(any(), any(), eq(null), eq(null));
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var actual = fixture.getVariables(request, questionnaire);
        assertNotNull(actual);
        assertEquals(expectedResponse, actual.get("testName"));
    }

    @Test
    void testGetVariablesHandlesEmptyList() {
        var questionnaire = new Questionnaire();
        var expression = new Expression()
                .setLanguage("text/cql-expression")
                .setExpression("test")
                .setName("testName");
        questionnaire.addExtension(Constants.VARIABLE_EXTENSION, expression);
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        doReturn(emptyList()).when(expressionProcessor).getExpressionResult(any(), any(), eq(null), eq(null));
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var actual = fixture.getVariables(request, questionnaire);
        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    @Test
    void testGetVariablesHandlesError() {
        var questionnaire = new Questionnaire();
        var expression = new Expression()
                .setLanguage("text/cql-expression")
                .setExpression("test")
                .setName("testName");
        questionnaire.addExtension(Constants.VARIABLE_EXTENSION, expression);
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var errorMessage = "An error has occurred.";
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        doThrow(new RuntimeException(errorMessage))
                .when(expressionProcessor)
                .getExpressionResult(any(), any(), eq(null), eq(null));
        var actual = fixture.getVariables(request, questionnaire);
        assertNotNull(actual);
        assertEquals(0, actual.size());
        var opOutcome = request.getOperationOutcome();
        assertNotNull(opOutcome);
        assertFalse(((OperationOutcome) opOutcome).getIssue().isEmpty());
    }

    @Test
    void testPopulateItemHandlesError() {
        var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var errorMessage = "An error has occurred.";
        final var request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var item = request.getAdapterFactory()
                .createQuestionnaireItem(new QuestionnaireItemComponent().setType(QuestionnaireItemType.BOOLEAN));
        doThrow(new RuntimeException(errorMessage)).when(itemProcessor).processItem(any(), any(), anyList());
        fixture.populateItem(request, item);
        var opOutcome = request.getOperationOutcome();
        assertNotNull(opOutcome);
        assertFalse(((OperationOutcome) opOutcome).getIssue().isEmpty());
    }
}
