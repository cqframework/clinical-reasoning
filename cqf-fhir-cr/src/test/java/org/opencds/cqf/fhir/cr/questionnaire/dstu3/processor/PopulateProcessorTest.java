package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class PopulateProcessorTest {
    @Spy
    private final PopulateProcessor fixture = new PopulateProcessor();

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
        doReturn(expectedResponses).when(fixture).processResponseItems(prePopulatedQuestionnaire.getItem());
        // execute
        final QuestionnaireResponse actual =
                fixture.populate(originalQuestionnaire, prePopulatedQuestionnaire, patientId);
        // validate
        assertEquals(prePopulatedQuestionnaireId + "-response", actual.getId());
        assertContainedResources(actual, operationOutcome, prePopulatedQuestionnaire);
        assertExtensions(actual, operationOutcomeId, prePopulatedQuestionnaireId);
        assertEquals(questionnaireUrl, actual.getQuestionnaire().getReference());
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, actual.getStatus());
        assertEquals("Patient/" + patientId, actual.getSubject().getReference());
        assertEquals(expectedResponses, actual.getItem());
        verify(fixture).processResponseItems(prePopulatedQuestionnaire.getItem());
    }

    private OperationOutcome withOperationOutcome(String operationOutcomeId) {
        final OperationOutcome operationOutcome = withOperationOutcomeWithIssue();
        operationOutcome.setId(operationOutcomeId);
        return operationOutcome;
    }

    private Questionnaire withPrepopulatedQuestionnaire(OperationOutcome operationOutcome, String id) {
        final Questionnaire prePopulatedQuestionnaire = new Questionnaire();
        prePopulatedQuestionnaire.addContained(operationOutcome);
        prePopulatedQuestionnaire.setId(id);
        prePopulatedQuestionnaire.addExtension(
                new Extension(Constants.EXT_CRMI_MESSAGES, new StringType("message value")));
        prePopulatedQuestionnaire.setItem(List.of(
                new QuestionnaireItemComponent(), new QuestionnaireItemComponent(), new QuestionnaireItemComponent()));
        return prePopulatedQuestionnaire;
    }

    private void assertExtensions(
            QuestionnaireResponse theActual, String operationOutcomeId, String prePopulatedQuestionnaireId) {
        final Extension expectedCrmiExtension = theActual.getExtensionByUrl(Constants.EXT_CRMI_MESSAGES);
        assertEquals("#" + operationOutcomeId, ((Reference) expectedCrmiExtension.getValue()).getReference());
        final Extension expectedDtrQuestionnaireExtension =
                theActual.getExtensionByUrl(Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE);
        assertEquals(
                "#" + prePopulatedQuestionnaireId,
                ((Reference) expectedDtrQuestionnaireExtension.getValue()).getReference());
    }

    private void assertContainedResources(
            QuestionnaireResponse actual,
            OperationOutcome expectedOperationOutcome,
            Questionnaire expectedQuestionnaire) {
        final OperationOutcome operationOutcome =
                (OperationOutcome) getContainedByResourceType(actual, ResourceType.OperationOutcome);
        assertEquals(expectedOperationOutcome, operationOutcome);
        final Questionnaire questionnaire =
                (Questionnaire) getContainedByResourceType(actual, ResourceType.Questionnaire);
        assertEquals(expectedQuestionnaire, questionnaire);
    }

    private Resource getContainedByResourceType(QuestionnaireResponse actual, ResourceType resourceType) {
        final Optional<Resource> resource = actual.getContained().stream()
                .filter(contained -> contained.getResourceType() == resourceType)
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
                fixture.getOperationOutcomeFromPrePopulatedQuestionnaire(prePopulatedQuestionnaire);
        // validate
        assertTrue(actual.isPresent());
        assertEquals(operationOutcome, actual.get());
    }

    @Test
    void filterOperationOutcomeShouldReturnTrueIfOperationOutcomeHasIssues() {
        // setup
        final OperationOutcome operationOutcome = withOperationOutcomeWithIssue();
        // execute
        final boolean actual = fixture.filterOperationOutcome(operationOutcome);
        // validate
        assertTrue(actual);
    }

    @Test
    void filterOperationOutcomeShouldReturnFalseIfOperationOutcomeHasNoIssues() {
        // setup
        final OperationOutcome operationOutcome = new OperationOutcome();
        // execute
        final boolean actual = fixture.filterOperationOutcome(operationOutcome);
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
        final QuestionnaireResponseItemComponent actual = fixture.processResponseItem(questionnaireItem);
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
        doReturn(expectedResponseItems).when(fixture).processResponseItems(nestedQuestionnaireItems);
        // execute
        final QuestionnaireResponseItemComponent actual = fixture.processResponseItem(questionnaireItem);
        // validate
        verify(fixture).processResponseItems(nestedQuestionnaireItems);
        assertEquals(3, actual.getItem().size());
        for (int i = 0; i < actual.getItem().size(); i++) {
            assertEquals(expectedResponseItems.get(i), actual.getItem().get(i));
        }
    }

    @Test
    void processResponseItemShouldSetAnswersIfTheQuestionnaireItemHasInitialValues() {
        // setup
        final Type expectedValue = withTypeValue();
        final QuestionnaireItemComponent questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValue(expectedValue);
        // execute
        final QuestionnaireResponseItemComponent actual = fixture.processResponseItem(questionnaireItemComponent);
        // validate
        assertEquals(1, actual.getAnswer().size());
        assertEquals(expectedValue, actual.getAnswer().get(0).getValue());
    }

    @Test
    void setAnswersForInitialShouldPopulateQuestionnaireResponseItemWithAnswers() {
        // setup
        final Type expectedValue = withTypeValue();
        final QuestionnaireItemComponent questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValue(expectedValue);
        // execute
        final QuestionnaireResponseItemComponent actual =
                fixture.setAnswersForInitial(questionnaireItemComponent, new QuestionnaireResponseItemComponent());
        // validate
        assertEquals(1, actual.getAnswer().size());
        assertEquals(expectedValue, actual.getAnswer().get(0).getValue());
    }

    @Test
    void setAnswersForInitialShouldAddExtensionIfResponseExtensionPresent() {
        // setup
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        final Extension extension = new Extension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, new StringType("theAuthor"));
        questionnaireItemComponent.addExtension(extension);
        // execute
        final QuestionnaireResponseItemComponent actual =
                fixture.setAnswersForInitial(questionnaireItemComponent, new QuestionnaireResponseItemComponent());
        // validate
        assertEquals(extension, actual.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
    }

    private QuestionnaireItemComponent withQuestionnaireItemComponentWithInitialValue(Type initialValue) {
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        questionnaireItemComponent.setInitial(initialValue);
        return questionnaireItemComponent;
    }

    private OperationOutcome withOperationOutcomeWithIssue() {
        final OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome
                .addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION);
        return operationOutcome;
    }

    private Type withTypeValue() {
        return new StringType("sample string");
    }

    private List<Type> withTypeValues() {
        return List.of(new BooleanType(true), new StringType("sample string"), new IntegerType(3));
    }
}
