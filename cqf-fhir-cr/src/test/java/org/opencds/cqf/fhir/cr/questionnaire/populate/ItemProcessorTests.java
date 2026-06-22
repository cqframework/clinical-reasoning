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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ItemProcessorTests {
    @Mock
    private IRepository repository;

    @Mock
    private ExpressionProcessor expressionProcessor;

    @Mock
    private LibraryEngine libraryEngine;

    private ItemProcessor itemProcessor;

    @BeforeEach
    void setup() {
        itemProcessor = new ItemProcessor(expressionProcessor);
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
        final var actual =
                itemProcessor.processItem(populateRequest, itemAdapter).get(0);
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
        final var actual =
                itemProcessor.processItem(populateRequest, itemAdapter).get(0);
        // validate
        final var answers = actual.getAnswer();
        assertEquals(3, answers.size());
        for (int i = 0; i < answers.size(); i++) {
            assertEquals(expressionResults.get(i), answers.get(i).getValue(), "value");
        }
    }

    private List<IBase> withExpressionResults(FhirVersionEnum fhirVersion) {
        return switch (fhirVersion) {
            case R4 ->
                List.of(
                        new org.hl7.fhir.r4.model.StringType("string type value"),
                        new org.hl7.fhir.r4.model.BooleanType(true),
                        new org.hl7.fhir.r4.model.IntegerType(3));
            case R5 ->
                List.of(
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
                itemProcessor.getInitialValue(prePopulateRequest, itemAdapter, responseItemAdapter, null);
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
                itemProcessor.getInitialValue(prePopulateRequest, itemAdapter, responseItemAdapter, null);
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
                itemProcessor.processItem(populateRequest, itemAdapter).get(0).get();
        assertNotNull(actual);
        assertFalse(actual.hasItem());
        assertTrue(actual.hasAnswer());
        assertFalse(actual.getAnswerFirstRep().hasValue());
        assertTrue(actual.getAnswerFirstRep().hasItem());
    }

    @Test
    void testMissingProfileLogsException() {
        var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var populateRequest = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var questionnaireItem = new QuestionnaireItemComponent()
                .setLinkId("1")
                .setType(QuestionnaireItemType.GROUP)
                .setDefinition("missing");
        var extensions = List.of(new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT));
        questionnaireItem.setExtension(extensions);
        var expression = new CqfExpression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
        List<IBase> expressionResults = List.of(new StringType("test"));
        doReturn(expression)
                .when(expressionProcessor)
                .getCqfExpression(populateRequest, extensions, Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        doReturn(expressionResults)
                .when(expressionProcessor)
                .getExpressionResultForItem(eq(populateRequest), eq(expression), eq("1"), any(), any());
        var adapter = (IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(populateRequest.getFhirVersion(), questionnaireItem);
        itemProcessor.processContextItem(populateRequest, adapter);
        var operationOutcome = (OperationOutcome) populateRequest.getOperationOutcome();
        assertTrue(operationOutcome.hasIssue());
        assertEquals(1, operationOutcome.getIssue().size());
    }

    @Test
    void testNoContextStillReturnsResponseItem() {
        var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var populateRequest = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var questionnaireItem = new QuestionnaireItemComponent()
                .setLinkId("1")
                .setType(QuestionnaireItemType.GROUP)
                .setDefinition("http://hl7.org/fhir/Patient#Patient.name.given");
        var extensions = List.of(new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT));
        questionnaireItem.setExtension(extensions);
        var expression = new CqfExpression().setLanguage("text/cql").setExpression("%subject.name.given[0]");
        List<IBase> expressionResults = new ArrayList<>();
        doReturn(expression)
                .when(expressionProcessor)
                .getCqfExpression(populateRequest, extensions, Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        doReturn(expressionResults)
                .when(expressionProcessor)
                .getExpressionResultForItem(eq(populateRequest), eq(expression), eq("1"), any(), any());
        var adapter = (IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(populateRequest.getFhirVersion(), questionnaireItem);
        var actual = itemProcessor.processContextItem(populateRequest, adapter);
        assertEquals(1, actual.size());
        assertTrue(actual.get(0).getAnswer().isEmpty());
    }

    @Test
    void testTupleContextPopulatesGroupChildrenFromNamedMembers() {
        // A population-context expression that returns a CQL Tuple (a value whose fhirType() is
        // "Tuple") must be wrapped so its named members resolve when the group's child items are
        // populated by definition. Previously the Tuple was wrapped with createBase (an
        // ElementAdapter), whose resolvePath cannot resolve a Tuple's named members and throws; the
        // exception was swallowed and the group was left silently unpopulated.
        final var fhirVersion = FhirVersionEnum.R4;
        final var profileUrl = "http://example.org/StructureDefinition/tuple-context-profile";
        final var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final var populateRequest = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);

        // child item populated from the tuple member "gender" via its definition
        final var childItem = new QuestionnaireItemComponent()
                .setLinkId("1.1")
                .setType(QuestionnaireItemType.STRING)
                .setDefinition(profileUrl + "#Patient.gender");
        final var groupItem = new QuestionnaireItemComponent()
                .setLinkId("1")
                .setType(QuestionnaireItemType.GROUP)
                .setDefinition(profileUrl + "#Patient")
                .addItem(childItem);
        final var extensions = List.of(new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT));
        groupItem.setExtension(extensions);

        final var profile = new StructureDefinition();
        profile.setUrl(profileUrl);
        final var genderElement = profile.getDifferential().addElement();
        genderElement.setPath("Patient.gender");
        genderElement.setId("Patient.gender");
        genderElement.addType().setCode("code");
        doReturn(new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(profile)))
                .when(repository)
                .search(eq(Bundle.class), eq(StructureDefinition.class), any(com.google.common.collect.Multimap.class));

        // the population-context expression returns a CQL Tuple carrying named members
        final var tuple = new Tuple();
        tuple.addProperty("gender", List.<Base>of(new StringType("male")));
        tuple.addProperty("name", List.<Base>of(new StringType("Smith")));
        final var expression = new CqfExpression().setLanguage("text/cql").setExpression("DefineTuple");
        doReturn(expression)
                .when(expressionProcessor)
                .getCqfExpression(populateRequest, extensions, Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        doReturn(List.<IBase>of(tuple))
                .when(expressionProcessor)
                .getExpressionResultForItem(eq(populateRequest), eq(expression), eq("1"), any(), any());

        final var adapter =
                (IQuestionnaireItemComponentAdapter) IAdapterFactory.createAdapterForBase(fhirVersion, groupItem);

        // execute
        final var actual = itemProcessor.processContextItem(populateRequest, adapter);

        // validate: the group's child item is populated from the tuple's "gender" member
        assertEquals(1, actual.size());
        final var responseGroup =
                (QuestionnaireResponseItemComponent) actual.get(0).get();
        assertTrue(responseGroup.hasItem(), "group child items should be populated from the tuple");
        final var responseChild = responseGroup.getItemFirstRep();
        assertTrue(responseChild.hasAnswer(), "child item should have an answer populated from the tuple member");
        assertEquals("male", ((StringType) responseChild.getAnswerFirstRep().getValue()).getValue());
    }

    @Test
    void testGetInitialValueReturnsBooleanType() {
        var questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var populateRequest = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        var nestedItem = new QuestionnaireItemComponent().setLinkId("1.1").setType(QuestionnaireItemType.BOOLEAN);
        nestedItem.addInitial().setValue(new BooleanType(false));
        nestedItem
                .addExtension()
                .setUrl("https://forms.smiledigitalhealth.com/docs/extensions/item-callback")
                .setValue(new StringType("View supporting evidence"));
        var questionnaireItem = new QuestionnaireItemComponent()
                .setLinkId("1")
                .setType(QuestionnaireItemType.GROUP)
                .addItem(nestedItem);
        var adapter = (IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(populateRequest.getFhirVersion(), questionnaireItem);
        doReturn(null).when(expressionProcessor).getItemInitialExpression(eq(populateRequest), any());
        var actual = itemProcessor.processItem(populateRequest, adapter);
        assertNotNull(actual);
        var nestedActual = (IQuestionnaireResponseItemComponentAdapter)
                actual.get(0).getItem().get(0);
        assertNotNull(nestedActual);
        assertTrue(nestedActual.hasAnswer());
        assertFalse(((BooleanType) nestedActual.getAnswer().get(0).getValue()).booleanValue());
    }
}
