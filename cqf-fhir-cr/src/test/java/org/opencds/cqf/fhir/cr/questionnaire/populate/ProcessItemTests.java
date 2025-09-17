package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ProcessItemTests {
    @Mock
    private IRepository repository;

    @Mock
    private ExpressionProcessor expressionProcessor;

    @Mock
    private LibraryEngine libraryEngine;

    private ProcessItem processItem;

    @BeforeEach
    void setup() {
        processItem = new ProcessItem(expressionProcessor);
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(expressionProcessor);
        verifyNoMoreInteractions(libraryEngine);
    }

    @Test
    void processItemShouldReturnQuestionnaireResponseItemComponentR4() {
        // setup
        final var fhirVersion = FhirVersionEnum.R4;
        final var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var populateRequest = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);
        final var originalQuestionnaireItemComponent =
                new QuestionnaireItemComponent().setLinkId("1").setType(QuestionnaireItemType.DECIMAL);
        final var itemAdapter = (IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, originalQuestionnaireItemComponent);
        final CqfExpression expression = withExpression();
        final List<IBase> expressionResults = withExpressionResults(fhirVersion);
        doReturn(expression).when(expressionProcessor).getItemInitialExpression(populateRequest, itemAdapter);
        doReturn(expressionResults)
                .when(expressionProcessor)
                .getExpressionResultForItem(eq(populateRequest), eq(expression), eq("1"), any(), any());
        // execute
        final var actual = processItem.processItem(populateRequest, itemAdapter);
        // validate
        final var answers = actual.getAnswer();
        assertEquals(3, answers.size());
        for (int i = 0; i < answers.size(); i++) {
            assertEquals(expressionResults.get(i), answers.get(i).getValue(), "value");
        }
    }

    @Test
    void processItemShouldReturnQuestionnaireResponseItemComponentR5() {
        // setup
        final var fhirVersion = FhirVersionEnum.R5;
        final var questionnaire = new org.hl7.fhir.r5.model.Questionnaire();
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        final var populateRequest = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);
        final var originalQuestionnaireItemComponent =
                new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent()
                        .setLinkId("1")
                        .setType(org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DECIMAL);
        final var itemAdapter = (IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, originalQuestionnaireItemComponent);
        final CqfExpression expression = withExpression();
        final List<IBase> expressionResults = withExpressionResults(fhirVersion);
        doReturn(expression).when(expressionProcessor).getItemInitialExpression(populateRequest, itemAdapter);
        doReturn(expressionResults)
                .when(expressionProcessor)
                .getExpressionResultForItem(eq(populateRequest), eq(expression), eq("1"), any(), any());
        // execute
        final var actual = processItem.processItem(populateRequest, itemAdapter);
        // validate
        final var answers = actual.getAnswer();
        assertEquals(3, answers.size());
        for (int i = 0; i < answers.size(); i++) {
            assertEquals(expressionResults.get(i), answers.get(i).getValue(), "value");
        }
    }

    private List<IBase> withExpressionResults(FhirVersionEnum fhirVersion) {
        return switch (fhirVersion) {
            case R4 -> List.of(
                    new org.hl7.fhir.r4.model.StringType("string type value"),
                    new org.hl7.fhir.r4.model.BooleanType(true),
                    new org.hl7.fhir.r4.model.IntegerType(3));
            case R5 -> List.of(
                    new org.hl7.fhir.r5.model.StringType("string type value"),
                    new org.hl7.fhir.r5.model.BooleanType(true),
                    new org.hl7.fhir.r5.model.IntegerType(3));
            default -> Collections.emptyList();
        };
    }

    @Test
    void getExpressionResultsShouldReturnEmptyListIfInitialExpressionIsNull() {
        // setup
        final var fhirVersion = FhirVersionEnum.R4;
        final var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var prePopulateRequest = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);
        final var questionnaireItemComponent = new QuestionnaireItemComponent();
        final var questionnaireResponseItemComponent = new QuestionnaireResponseItemComponent();
        final var itemAdapter = (IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, questionnaireItemComponent);
        final var responseItemAdapter = (IQuestionnaireResponseItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, questionnaireResponseItemComponent);
        doReturn(null).when(expressionProcessor).getItemInitialExpression(prePopulateRequest, itemAdapter);
        // execute
        final List<IBase> actual =
                processItem.getInitialValue(prePopulateRequest, itemAdapter, responseItemAdapter, null);
        // validate
        assertTrue(actual.isEmpty());
        verify(expressionProcessor).getItemInitialExpression(prePopulateRequest, itemAdapter);
        verify(expressionProcessor, never()).getExpressionResultForItem(prePopulateRequest, null, "linkId", null, null);
    }

    @Test
    void getExpressionResultsShouldReturnListOfResourcesIfInitialExpressionIsNotNull() {
        // setup
        final var fhirVersion = FhirVersionEnum.R4;
        final var expected = withExpressionResults(FhirVersionEnum.R4);
        final var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var prePopulateRequest = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final var questionnaireItemComponent = new QuestionnaireItemComponent().setLinkId("linkId");
        final var questionnaireResponseItemComponent = new QuestionnaireResponseItemComponent();
        final var itemAdapter = (IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, questionnaireItemComponent);
        final var responseItemAdapter = (IQuestionnaireResponseItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, questionnaireResponseItemComponent);
        final var expression = withExpression();
        doReturn(expression).when(expressionProcessor).getItemInitialExpression(prePopulateRequest, itemAdapter);
        doReturn(expected)
                .when(expressionProcessor)
                .getExpressionResultForItem(prePopulateRequest, expression, "linkId", null, null);
        // execute
        final List<IBase> actual =
                processItem.getInitialValue(prePopulateRequest, itemAdapter, responseItemAdapter, null);
        // validate
        assertEquals(expected, actual);
        verify(expressionProcessor).getItemInitialExpression(prePopulateRequest, itemAdapter);
        verify(expressionProcessor).getExpressionResultForItem(prePopulateRequest, expression, "linkId", null, null);
    }

    private CqfExpression withExpression() {
        return new CqfExpression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
    }

    @Test
    void processItemShouldCorrectlyProcessNonGroupItemsWithChildren() {
        final var fhirVersion = FhirVersionEnum.R4;
        final var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var populateRequest = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var parentItem = questionnaire.addItem().setLinkId("1").setType(QuestionnaireItemType.BOOLEAN);
        parentItem.addItem().setLinkId("1.1").setType(QuestionnaireItemType.BOOLEAN);
        parentItem.addItem().setLinkId("1.2").setType(QuestionnaireItemType.BOOLEAN);
        final var itemAdapter =
                (IQuestionnaireItemComponentAdapter) IAdapterFactory.createAdapterForBase(fhirVersion, parentItem);
        doReturn(null).when(expressionProcessor).getItemInitialExpression(eq(populateRequest), any());
        var actual = (QuestionnaireResponseItemComponent)
                processItem.processItem(populateRequest, itemAdapter).get();
        assertNotNull(actual);
        assertFalse(actual.hasItem());
        assertTrue(actual.hasAnswer());
        assertFalse(actual.getAnswerFirstRep().hasValue());
        assertTrue(actual.getAnswerFirstRep().hasItem());
    }
}
