package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.populate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class PopulateServiceTest {
    @Spy
    private final PopulateService myFixture = PopulateService.of();

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFields() {
        // setup
        final String patientId = "patientId";
        final String questionnaireUrl = "original-questionnaire-url";
        final String operationOutcomeId = "operation-outcome-id";
        final String prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final Questionnaire originalQuestionnaire = new Questionnaire();
        originalQuestionnaire.setUrl(questionnaireUrl);
        final OperationOutcome operationOutcome = withOperationOutcome(operationOutcomeId);
        final Questionnaire prePopulatedQuestionnaire =
                withPrepopulatedQuestionnaire(operationOutcome, prePopulatedQuestionnaireId);
        final List<QuestionnaireResponseItemComponent> expectedResponses = List.of(
                new QuestionnaireResponseItemComponent(),
                new QuestionnaireResponseItemComponent(),
                new QuestionnaireResponseItemComponent());
        doReturn(expectedResponses).when(myFixture).processResponseItems(prePopulatedQuestionnaire.getItem());
        // execute
        final QuestionnaireResponse actual =
                myFixture.populate(originalQuestionnaire, prePopulatedQuestionnaire, patientId);
        // validate
        assertEquals(prePopulatedQuestionnaireId + "-response", actual.getId());
        assertContainedResources(actual, operationOutcome, prePopulatedQuestionnaire);
        assertExtensions(actual, operationOutcomeId, prePopulatedQuestionnaireId);
        assertEquals(questionnaireUrl, actual.getQuestionnaire());
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, actual.getStatus());
        assertEquals("Patient/" + patientId, actual.getSubject().getReference());
        assertEquals(expectedResponses, actual.getItem());
        verify(myFixture).processResponseItems(prePopulatedQuestionnaire.getItem());
    }

    private OperationOutcome withOperationOutcome(String theOperationOutcomeId) {
        final OperationOutcome operationOutcome = withOperationOutcomeWithIssue();
        operationOutcome.setId(theOperationOutcomeId);
        return operationOutcome;
    }

    private Questionnaire withPrepopulatedQuestionnaire(OperationOutcome theOperationOutcome, String theId) {
        final Questionnaire prePopulatedQuestionnaire = new Questionnaire();
        prePopulatedQuestionnaire.addContained(theOperationOutcome);
        prePopulatedQuestionnaire.setId(theId);
        prePopulatedQuestionnaire.addExtension(
                new Extension(Constants.EXT_CRMI_MESSAGES, new StringType("message value")));
        prePopulatedQuestionnaire.setItem(List.of(
                new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent()));
        return prePopulatedQuestionnaire;
    }

    private void assertExtensions(
            QuestionnaireResponse theActual, String theOperationOutcomeId, String thePrePopulatedQuestionnaireId) {
        final Extension expectedCrmiExtension = theActual.getExtensionByUrl(Constants.EXT_CRMI_MESSAGES);
        assertEquals("#" + theOperationOutcomeId, ((Reference) expectedCrmiExtension.getValue()).getReference());
        final Extension expectedDtrQuestionnaireExtension =
                theActual.getExtensionByUrl(Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE);
        assertEquals(
                "#" + thePrePopulatedQuestionnaireId,
                ((Reference) expectedDtrQuestionnaireExtension.getValue()).getReference());
    }

    private void assertContainedResources(
            QuestionnaireResponse theActual,
            OperationOutcome theExpectedOperationOutcome,
            Questionnaire theExpectedQuestionnaire) {
        final OperationOutcome operationOutcome =
                (OperationOutcome) getContainedByResourceType(theActual, ResourceType.OperationOutcome);
        assertEquals(theExpectedOperationOutcome, operationOutcome);
        final Questionnaire questionnaire =
                (Questionnaire) getContainedByResourceType(theActual, ResourceType.Questionnaire);
        assertEquals(theExpectedQuestionnaire, questionnaire);
    }

    private Resource getContainedByResourceType(QuestionnaireResponse theActual, ResourceType theResourceType) {
        final Optional<Resource> resource = theActual.getContained().stream()
                .filter(contained -> contained.getResourceType() == theResourceType)
                .findFirst();
        assertTrue(resource.isPresent());
        return resource.get();
    }

    @Test
    void getOperationOutcomeFromPrePopulatedQuestionnaireShouldReturnOptionalOperationIfPresent() {
        // setup
        final OperationOutcome operationOutcome = withOperationOutcomeWithIssue();
        final Questionnaire prePopulatedQuestionnaire = new Questionnaire();
        prePopulatedQuestionnaire.addContained(operationOutcome);
        // execute
        final Optional<OperationOutcome> actual =
                myFixture.getOperationOutcomeFromPrePopulatedQuestionnaire(prePopulatedQuestionnaire);
        // validate
        assertTrue(actual.isPresent());
        assertEquals(operationOutcome, actual.get());
    }

    @Test
    void filterOperationOutcomeShouldReturnTrueIfOperationOutcomeHasIssues() {
        // setup
        final OperationOutcome operationOutcome = withOperationOutcomeWithIssue();
        // execute
        final boolean actual = myFixture.filterOperationOutcome(operationOutcome);
        // validate
        assertTrue(actual);
    }

    @Test
    void filterOperationOutcomeShouldReturnFalseIfOperationOutcomeHasNoIssues() {
        // setup
        final OperationOutcome operationOutcome = new OperationOutcome();
        // execute
        final boolean actual = myFixture.filterOperationOutcome(operationOutcome);
        // validate
        assertFalse(actual);
    }

    @Test
    void processResponseItemShouldSetBasePropertiesOnQuestionnaireResponseItemComponent() {
        // setup
        final String linkId = "linkId";
        final String definition = "definition";
        final StringType textElement = new StringType("textElement");
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent()
                .setLinkId(linkId)
                .setDefinition(definition)
                .setTextElement(textElement);
        // execute
        final QuestionnaireResponseItemComponent actual = myFixture.processResponseItem(questionnaireItem);
        // validate
        assertEquals(linkId, actual.getLinkId());
        assertEquals(definition, actual.getDefinition());
        assertEquals(textElement, actual.getTextElement());
    }

    @Test
    void processResponseItemShouldProcessResponseItemsRecursivelyIfQuestionnaireItemHasItems() {
        // setup
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final List<QuestionnaireItemComponent> nestedQuestionnaireItems = List.of(
                new QuestionnaireItemComponent().setLinkId("linkId1"),
                new QuestionnaireItemComponent().setLinkId("linkId2"),
                new QuestionnaireItemComponent().setLinkId("linkId3"));
        questionnaireItem.setItem(nestedQuestionnaireItems);
        List<QuestionnaireResponseItemComponent> expectedResponseItems = List.of(
                new QuestionnaireResponseItemComponent(),
                new QuestionnaireResponseItemComponent(),
                new QuestionnaireResponseItemComponent());
        doReturn(expectedResponseItems).when(myFixture).processResponseItems(nestedQuestionnaireItems);
        // execute
        final QuestionnaireResponseItemComponent actual = myFixture.processResponseItem(questionnaireItem);
        // validate
        verify(myFixture).processResponseItems(nestedQuestionnaireItems);
        assertEquals(3, actual.getItem().size());
        for (int i = 0; i < actual.getItem().size(); i++) {
            assertEquals(expectedResponseItems.get(i), actual.getItem().get(i));
        }
    }

    @Test
    void processResponseItemShouldSetAnswersIfTheQuestionnaireItemHasInitialValues() {
        // setup
        final List<Type> expectedValues = withTypeValues();
        final QuestionnaireItemComponent questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValues(expectedValues);
        // execute
        final QuestionnaireResponseItemComponent actual = myFixture.processResponseItem(questionnaireItemComponent);
        // validate
        validateQuestionnaireResponseItemAnswers(expectedValues, actual);
    }

    @Test
    void setAnswersForInitialShouldPopulateQuestionnaireResponseItemWithAnswers() {
        // setup
        final List<Type> expectedValues = withTypeValues();
        final QuestionnaireItemComponent questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValues(expectedValues);
        // execute
        final QuestionnaireResponseItemComponent actual =
                myFixture.setAnswersForInitial(questionnaireItemComponent, new QuestionnaireResponseItemComponent());
        // validate
        validateQuestionnaireResponseItemAnswers(expectedValues, actual);
    }

    private void validateQuestionnaireResponseItemAnswers(
            List<Type> theExpectedValues, QuestionnaireResponseItemComponent theQuestionnaireResponse) {
        assertEquals(3, theQuestionnaireResponse.getAnswer().size());
        for (int i = 0; i < theQuestionnaireResponse.getAnswer().size(); i++) {
            final List<QuestionnaireResponseItemAnswerComponent> answers = theQuestionnaireResponse.getAnswer();
            assertEquals(theExpectedValues.get(i), answers.get(i).getValue());
        }
    }

    @Test
    void setAnswersForInitialShouldAddExtensionIfResponseExtensionPresent() {
        // setup
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        final Extension extension = new Extension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, new StringType("theAuthor"));
        questionnaireItemComponent.addExtension(extension);
        // execute
        final QuestionnaireResponseItemComponent actual =
                myFixture.setAnswersForInitial(questionnaireItemComponent, new QuestionnaireResponseItemComponent());
        // validate
        assertEquals(extension, actual.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
    }

    private QuestionnaireItemComponent withQuestionnaireItemComponentWithInitialValues(List<Type> theInitialValues) {
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        final List<QuestionnaireItemInitialComponent> initialComponents =
                theInitialValues.stream().map(this::withInitialWithValue).collect(Collectors.toList());
        initialComponents.forEach(questionnaireItemComponent::addInitial);
        return questionnaireItemComponent;
    }

    private QuestionnaireItemInitialComponent withInitialWithValue(Type theValue) {
        final QuestionnaireItemInitialComponent initialComponent = new QuestionnaireItemInitialComponent();
        initialComponent.setValue(theValue);
        return initialComponent;
    }

    private OperationOutcome withOperationOutcomeWithIssue() {
        final OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome
                .addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION);
        return operationOutcome;
    }

    private List<Type> withTypeValues() {
        return List.of(new BooleanType(true), new StringType("sample string"), new IntegerType(3));
    }
}
