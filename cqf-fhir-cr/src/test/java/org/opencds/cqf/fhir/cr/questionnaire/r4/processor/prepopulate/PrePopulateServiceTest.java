package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

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
import org.opencds.cqf.fhir.utility.Constants;
;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PrePopulateServiceTest {
    @Mock
    private PrePopulateItem myPrePopulateItem;

    @Mock
    private PrePopulateItemWithExtension myPrePopulateItemWithExtension;

    @Mock
    private LibraryEngine myLibraryEngine;

    @Spy
    @InjectMocks
    private PrePopulateService myFixture;

    @BeforeEach
    void setup() {
        myFixture.myOperationOutcome = new OperationOutcome();
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
        final String populatedQuestionnaireId = PrePopulateRequestHelpers.QUESTIONNAIRE_ID + "-" + PrePopulateRequestHelpers.PATIENT_ID;
        final List<QuestionnaireItemComponent> expectedSubItems = List.of(
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent()
        );
        final OperationOutcome operationOutcomeWithIssues = new OperationOutcome();
        operationOutcomeWithIssues.setId("operation-outcome-id");
        operationOutcomeWithIssues.addIssue()
            .setCode(OperationOutcome.IssueType.EXCEPTION)
            .setSeverity(OperationOutcome.IssueSeverity.ERROR)
            .setDiagnostics("exception message");
        doReturn(operationOutcomeWithIssues).when(myFixture).getBaseOperationOutcome(populatedQuestionnaireId);
        doReturn(expectedSubItems).when(myFixture).processItems(prePopulateRequest, PrePopulateRequestHelpers.QUESTIONNAIRE_SUB_ITEMS);
        // execute
        final Questionnaire actual = myFixture.prePopulate(prePopulateRequest);
        // validate
        verify(myFixture).getBaseOperationOutcome(populatedQuestionnaireId);
        verify(myFixture).processItems(prePopulateRequest, PrePopulateRequestHelpers.QUESTIONNAIRE_SUB_ITEMS);
        assertEquals(populatedQuestionnaireId, actual.getId());
        assertEquals(expectedSubItems, actual.getItem());
        validateOperationOutcomeResults(actual);
        validateExtensions(actual);
    }

    void validateOperationOutcomeResults(Questionnaire theActual) {
        assertFalse(myFixture.myOperationOutcome.getIssue().isEmpty());
        final OperationOutcome contained = (OperationOutcome) theActual.getContained().get(0);
        assertEquals(myFixture.myOperationOutcome, contained);
    }

    void validateExtensions(Questionnaire theActual) {
        final Reference sdcExtension = (Reference) theActual.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT).getValue();
        assertEquals(FHIRAllTypes.PATIENT.toCode() + "/" + PrePopulateRequestHelpers.PATIENT_ID, sdcExtension.getReference());
        final Reference crmiExtension = (Reference) theActual.getExtensionByUrl(Constants.EXT_CRMI_MESSAGES).getValue();
        assertEquals("#operation-outcome-id", crmiExtension.getReference());
    }

    @Test
    void processItemsShouldProcessItemsIfItemsHaveSdcExtension() {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final List<QuestionnaireItemComponent> expectedItems = List.of(
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent()
        );
        final List<QuestionnaireItemComponent> questionnaireItems = List.of(
            withQuestionnaireItemWithExtension(),
            withQuestionnaireItemWithExtension(),
            withQuestionnaireItemWithExtension()
        );
        doReturn(List.of(expectedItems.get(0))).when(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(0));
        doReturn(List.of(expectedItems.get(1))).when(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(1));
        doReturn(List.of(expectedItems.get(2))).when(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(2));
        // execute
        final List<QuestionnaireItemComponent> actual = myFixture.processItems(prePopulateRequest, questionnaireItems);
        // validate
        assertEquals(expectedItems, actual);
        verify(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(0));
        verify(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(1));
        verify(myFixture).prePopulateItemWithExtension(prePopulateRequest, questionnaireItems.get(2));
    }

    private QuestionnaireItemComponent withQuestionnaireItemWithExtension() {
        final Extension extension = new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT, new StringType("extension value"));
        final QuestionnaireItemComponent item = new QuestionnaireItemComponent();
        item.addExtension(extension);
        return item;
    }

    @Test
    void processItemsShouldRecursivelyProcessItemsIfThereAreSubItems() {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final List<QuestionnaireItemComponent> subItems = List.of(
            new QuestionnaireItemComponent().setLinkId("linkId1"),
            new QuestionnaireItemComponent().setLinkId("linkId2"),
            new QuestionnaireItemComponent().setLinkId("linkId3")
        );
        final List<QuestionnaireItemComponent> expected = List.of(
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent()
        );
        questionnaireItem.setItem(subItems);
        final List<QuestionnaireItemComponent> input = List.of(questionnaireItem);
        doReturn(expected).when(myFixture).processItems(prePopulateRequest, subItems);
        // execute
        List<QuestionnaireItemComponent> actual = myFixture.processItems(prePopulateRequest, input);
        // validate
        verify(myFixture).processItems(prePopulateRequest, subItems);
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
        final QuestionnaireItemComponent expected = new QuestionnaireItemComponent();
        final QuestionnaireItemComponent copied = new QuestionnaireItemComponent();
        final List<QuestionnaireItemComponent> input = List.of(questionnaireItem);
        doReturn(copied).when(myFixture).copyQuestionnaireItem(questionnaireItem);
        doReturn(expected).when(myPrePopulateItem).processItem(prePopulateRequest, copied);
        // execute
        List<QuestionnaireItemComponent> actual = myFixture.processItems(prePopulateRequest, input);
        // validate
        verify(myFixture).copyQuestionnaireItem(questionnaireItem);
        verify(myPrePopulateItem).processItem(prePopulateRequest, copied);
        assertEquals(1, actual.size());
        final QuestionnaireItemComponent subItem = actual.get(0);
        assertEquals(expected, subItem);
    }

    @Test
    void prePopulateItemWithExtensionShouldReturnQuestionnaireItemComponent() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        final List<QuestionnaireItemComponent> expected = List.of(
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent(),
            new QuestionnaireItemComponent()
        );
        doReturn(expected).when(myPrePopulateItemWithExtension).processItem(prePopulateRequest, input);
        // execute
        final List<QuestionnaireItemComponent> actual = myFixture.prePopulateItemWithExtension(prePopulateRequest, input);
        // validate
        verify(myPrePopulateItemWithExtension).processItem(prePopulateRequest, input);
        assertEquals(expected, actual);
    }

    @Test
    void prePopulateItemWithExceptionShouldReturnEmptyListWhenExceptionCaught() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        doThrow(new ResolveExpressionException("message")).when(myPrePopulateItemWithExtension).processItem(prePopulateRequest, input);
        // execute
        final List<QuestionnaireItemComponent> actual = myFixture.prePopulateItemWithExtension(prePopulateRequest, input);
        // validate
        verify(myPrePopulateItemWithExtension).processItem(prePopulateRequest, input);
        verify(myFixture).addExceptionToOperationOutcome("message");
        assertTrue(actual.isEmpty());
    }


    @Test
    void prePopulateItemShouldReturnQuestionnaireItemComponent() throws ResolveExpressionException {
        // setup
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        final QuestionnaireItemComponent expected = new QuestionnaireItemComponent();
        doReturn(expected).when(myPrePopulateItem).processItem(prePopulateRequest, input);
        // execute
        final QuestionnaireItemComponent actual = myFixture.prePopulateItem(prePopulateRequest, input);
        // validate
        verify(myPrePopulateItem).processItem(prePopulateRequest, input);
        assertEquals(expected, actual);
    }

    @Test
    void prePopulateItemShouldReturnQuestionnaireItemComponentWhenExceptionCaught() throws ResolveExpressionException {
        // setup
        final String id = "questionnaire-item-id";
        final PrePopulateRequest prePopulateRequest = PrePopulateRequestHelpers.withPrePopulateRequest(myLibraryEngine);
        final QuestionnaireItemComponent input = new QuestionnaireItemComponent();
        input.setId(id);
        doThrow(new ResolveExpressionException("message")).when(myPrePopulateItem).processItem(prePopulateRequest, input);
        // execute
        final QuestionnaireItemComponent actual = myFixture.prePopulateItem(prePopulateRequest, input);
        // validate
        verify(myPrePopulateItem).processItem(prePopulateRequest, input);
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
        assertTrue(myFixture.myOperationOutcome.hasIssue());
        final Optional<OperationOutcomeIssueComponent> issue = myFixture.myOperationOutcome.getIssue().stream().findFirst();
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
