package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.opencds.cqf.fhir.cr.questionnaire.helpers.QuestionnaireHelpers;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class PrePopulateProcessorTest {
    @Mock
    private PrePopulateItem myPrePopulateItem;

    @Mock
    private PrePopulateItemWithExtension myPrePopulateItemWithExtension;

    @Mock
    private LibraryEngine myLibraryEngine;

    @Spy
    @InjectMocks
    private PrePopulateProcessor myFixture;

    @BeforeEach
    void setup() {
        myFixture.operationOutcome = new OperationOutcome();
    }

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(myPrePopulateItem);
        verifyNoMoreInteractions(myPrePopulateItemWithExtension);
    }

    @Test
    void prePopulateShouldReturnQuestionnaire() {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final Questionnaire questionnaire = QuestionnaireHelpers.withQuestionnaire();
        questionnaire.setId(QuestionnaireHelpers.QUESTIONNAIRE_ID);
        final String populatedQuestionnaireId =
            QuestionnaireHelpers.QUESTIONNAIRE_ID + "-" + PrePopulateRequestHelpers.PATIENT_ID;
        final List<QuestionnaireItemComponent> expectedSubItems = List.of(
                new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent());
        final OperationOutcome operationOutcomeWithIssues = new OperationOutcome();
        operationOutcomeWithIssues.setId("operation-outcome-id");
        operationOutcomeWithIssues
                .addIssue()
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDiagnostics("exception message");
        doReturn(operationOutcomeWithIssues).when(myFixture).getBaseOperationOutcome(populatedQuestionnaireId);
        doReturn(expectedSubItems)
                .when(myFixture)
                .processItems(prePopulateRequest, QuestionnaireHelpers.QUESTIONNAIRE_SUB_ITEMS, questionnaire);
        // execute
        final Questionnaire actual = myFixture.prePopulate(questionnaire, prePopulateRequest);
        // validate
        verify(myFixture).getBaseOperationOutcome(populatedQuestionnaireId);
        verify(myFixture).processItems(prePopulateRequest, QuestionnaireHelpers.QUESTIONNAIRE_SUB_ITEMS, questionnaire);
        assertEquals(populatedQuestionnaireId, actual.getId());
        assertEquals(expectedSubItems, actual.getItem());
        validateOperationOutcomeResults(actual);
        validateExtensions(actual);
    }

    void validateOperationOutcomeResults(Questionnaire theActual) {
        assertFalse(myFixture.operationOutcome.getIssue().isEmpty());
        final OperationOutcome contained =
                (OperationOutcome) theActual.getContained().get(0);
        assertEquals(myFixture.operationOutcome, contained);
    }

    void validateExtensions(Questionnaire theActual) {
        final Reference sdcExtension = (Reference) theActual
                .getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT)
                .getValue();
        assertEquals(
                FHIRAllTypes.PATIENT.toCode() + "/" + PrePopulateRequestHelpers.PATIENT_ID,
                sdcExtension.getReference());
        final Reference crmiExtension = (Reference)
                theActual.getExtensionByUrl(Constants.EXT_CRMI_MESSAGES).getValue();
        assertEquals("#operation-outcome-id", crmiExtension.getReference());
    }

    @Test
    void processItemsShouldProcessItemsIfItemsHaveSdcExtension() {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final Questionnaire questionnaire = new Questionnaire();
        final List<QuestionnaireItemComponent> expectedItems = List.of(
                new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent());
        final List<QuestionnaireItemComponent> questionnaireItems = List.of(
                withQuestionnaireItemWithExtension(),
                withQuestionnaireItemWithExtension(),
                withQuestionnaireItemWithExtension());
        doReturn(List.of(expectedItems.get(0)))
                .when(myFixture)
                .prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(0), questionnaire);
        doReturn(List.of(expectedItems.get(1)))
                .when(myFixture)
                .prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(1), questionnaire);
        doReturn(List.of(expectedItems.get(2)))
                .when(myFixture)
                .prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(2), questionnaire);
        // execute
        final List<QuestionnaireItemComponent> actual = myFixture.processItems(prePopulateRequest, questionnaireItems, questionnaire);
        // validate
        assertEquals(expectedItems, actual);
        verify(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(0), questionnaire);
        verify(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(1), questionnaire);
        verify(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(2), questionnaire);
    }

    private QuestionnaireItemComponent withQuestionnaireItemWithExtension() {
        final Extension extension =
                new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT, new StringType("extension value"));
        final QuestionnaireItemComponent item = new QuestionnaireItemComponent();
        item.addExtension(extension);
        return item;
    }

    @Test
    void processItemsShouldRecursivelyProcessItemsIfThereAreSubItems() {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final Questionnaire questionnaire = new Questionnaire();
        final List<QuestionnaireItemComponent> subItems = List.of(
                new QuestionnaireItemComponent().setLinkId("linkId1"),
                new QuestionnaireItemComponent().setLinkId("linkId2"),
                new QuestionnaireItemComponent().setLinkId("linkId3"));
        final List<QuestionnaireItemComponent> expected = List.of(
                new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent());
        questionnaireItem.setItem(subItems);
        final List<QuestionnaireItemComponent> input = List.of(questionnaireItem);
        doReturn(expected).when(myFixture).processItems(prePopulateRequest, subItems, questionnaire);
        // execute
        List<QuestionnaireItemComponent> actual = myFixture.processItems(prePopulateRequest, input, questionnaire);
        // validate
        verify(myFixture).processItems(prePopulateRequest, subItems, questionnaire);
        assertEquals(1, actual.size());
        final List<QuestionnaireItemComponent> actualSubItems = actual.get(0).getItem();
        assertEquals(3, actualSubItems.size());
        assertEquals(expected, actualSubItems);
    }

    @Test
    void processItemsShouldProcessItemsIfThereAreNoSubItems() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final Questionnaire questionnaire = new Questionnaire();
        final QuestionnaireItemComponent expected = new QuestionnaireItemComponent();
        final QuestionnaireItemComponent copied = new QuestionnaireItemComponent();
        final List<QuestionnaireItemComponent> input = List.of(questionnaireItem);
        doReturn(copied).when(myFixture).copyQuestionnaireItem(questionnaireItem);
        doReturn(expected).when(myPrePopulateItem).processItem(prePopulateRequest, copied, questionnaire);
        // execute
        List<QuestionnaireItemComponent> actual = myFixture.processItems(prePopulateRequest, input, questionnaire);
        // validate
        verify(myFixture).copyQuestionnaireItem(questionnaireItem);
        verify(myPrePopulateItem).processItem(prePopulateRequest, copied, questionnaire);
        assertEquals(1, actual.size());
        final QuestionnaireItemComponent subItem = actual.get(0);
        assertEquals(expected, subItem);
    }

    @Test
    void prePopulateItemWithExtensionShouldReturnQuestionnaireItemComponent() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        final Questionnaire questionnaire = new Questionnaire();
        final List<QuestionnaireItemComponent> expected = List.of(
                new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent());
        doReturn(expected).when(myPrePopulateItemWithExtension).processItem(prePopulateRequest, input, questionnaire);
        // execute
        final List<QuestionnaireItemComponent> actual =
                myFixture.prePopulateItemWithExtension(prePopulateRequest, input, questionnaire);
        // validate
        verify(myPrePopulateItemWithExtension).processItem(prePopulateRequest, input, questionnaire);
        assertEquals(expected, actual);
    }

    @Test
    void prePopulateItemWithExceptionShouldReturnEmptyListWhenExceptionCaught() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        final Questionnaire questionnaire = new Questionnaire();
        doThrow(new ResolveExpressionException("message"))
                .when(myPrePopulateItemWithExtension)
                .processItem(prePopulateRequest, input, questionnaire);
        // execute
        final List<QuestionnaireItemComponent> actual =
                myFixture.prePopulateItemWithExtension(prePopulateRequest, input, questionnaire);
        // validate
        verify(myPrePopulateItemWithExtension).processItem(prePopulateRequest, input, questionnaire);
        verify(myFixture).addExceptionToOperationOutcome("message");
        assertTrue(actual.isEmpty());
    }

    @Test
    void prePopulateItemShouldReturnQuestionnaireItemComponent() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        final QuestionnaireItemComponent expected = new QuestionnaireItemComponent();
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(expected).when(myPrePopulateItem).processItem(prePopulateRequest, input, questionnaire);
        // execute
        final QuestionnaireItemComponent actual = myFixture.prePopulateItem(prePopulateRequest, input, questionnaire);
        // validate
        verify(myPrePopulateItem).processItem(prePopulateRequest, input, questionnaire);
        assertEquals(expected, actual);
    }

    @Test
    void prePopulateItemShouldReturnQuestionnaireItemComponentWhenExceptionCaught() throws ResolveExpressionException {
        // setup
        final String id = "questionnaire-item-id";
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        input.setId(id);
        final Questionnaire questionnaire = new Questionnaire();
        doThrow(new ResolveExpressionException("message"))
                .when(myPrePopulateItem)
                .processItem(prePopulateRequest, input, questionnaire);
        // execute
        final QuestionnaireItemComponent actual = myFixture.prePopulateItem(prePopulateRequest, input, questionnaire);
        // validate
        verify(myPrePopulateItem).processItem(prePopulateRequest, input, questionnaire);
        verify(myFixture).addExceptionToOperationOutcome("message");
        assertEquals(id, actual.getId());
    }

    @Test
    void addExceptionToOperationOutcomeShouldSetPropertiesOnOperationOutcome() {
        // setup
        final String errorMessage = "the error message";
        // execute
        myFixture.addExceptionToOperationOutcome(errorMessage);
        // validate
        assertTrue(myFixture.operationOutcome.hasIssue());
        final Optional<OperationOutcomeIssueComponent> issue =
                myFixture.operationOutcome.getIssue().stream().findFirst();
        assertTrue(issue.isPresent());
        assertEquals(OperationOutcome.IssueType.EXCEPTION, issue.get().getCode());
        assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.get().getSeverity());
        assertEquals(errorMessage, issue.get().getDiagnostics());
    }

    @Test
    void getBaseOperationOutcomeShouldReturnOperationOutcome() {
        // execute
        final OperationOutcome actual = myFixture.getBaseOperationOutcome("questionnaireId");
        // validate
        assertEquals("populate-outcome-questionnaireId", actual.getId());
    }
}
