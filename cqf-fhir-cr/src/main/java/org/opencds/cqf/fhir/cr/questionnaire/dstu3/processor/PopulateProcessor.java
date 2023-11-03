package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.buildDstu3;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.buildR4;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.crmiMessagesExtension;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.dtrQuestionnaireResponseExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.opencds.cqf.fhir.utility.Constants;

public class PopulateProcessor {
    public QuestionnaireResponse populate(
            Questionnaire originalQuestionnaire, Questionnaire prePopulatedQuestionnaire, String patientId) {
        final QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId(prePopulatedQuestionnaire.getIdPart() + "-response");
        final Optional<OperationOutcome> operationOutcome =
                getOperationOutcomeFromPrePopulatedQuestionnaire(prePopulatedQuestionnaire);
        if (prePopulatedQuestionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES) && operationOutcome.isPresent()) {
            response.addContained(operationOutcome.get());
            response.addExtension(buildDstu3(crmiMessagesExtension(
                    operationOutcome.get().getIdPart())));
        }
        response.addContained(prePopulatedQuestionnaire);
        response.addExtension(buildDstu3(dtrQuestionnaireResponseExtension(prePopulatedQuestionnaire.getIdPart())));
        response.setQuestionnaire(new Reference(originalQuestionnaire.getUrl()));
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", patientId)));
        response.setItem(processResponseItems(prePopulatedQuestionnaire.getItem()));
        return response;
    }

    Optional<OperationOutcome> getOperationOutcomeFromPrePopulatedQuestionnaire(
            Questionnaire prePopulatedQuestionnaire) {
        return prePopulatedQuestionnaire.getContained().stream()
                .filter(resource -> resource.getResourceType() == ResourceType.OperationOutcome)
                .map(OperationOutcome.class::cast)
                .filter(this::filterOperationOutcome)
                .findFirst();
    }

    boolean filterOperationOutcome(OperationOutcome operationOutcome) {
        if (operationOutcome.hasIssue()) {
            final List<OperationOutcomeIssueComponent> filteredIssues = operationOutcome.getIssue().stream()
                    .filter(issue -> issue.getCode() == OperationOutcome.IssueType.EXCEPTION
                            && issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR)
                    .collect(Collectors.toList());
            return !filteredIssues.isEmpty();
        }
        return false;
    }

    List<QuestionnaireResponseItemComponent> processResponseItems(
            List<QuestionnaireItemComponent> questionnaireItems) {
        return questionnaireItems.stream().map(this::processResponseItem).collect(Collectors.toList());
    }

    QuestionnaireResponseItemComponent processResponseItem(QuestionnaireItemComponent questionnaireItem) {
        QuestionnaireResponseItemComponent responseItem =
                new QuestionnaireResponseItemComponent(questionnaireItem.getLinkIdElement());
        responseItem.setDefinition(questionnaireItem.getDefinition());
        responseItem.setTextElement(questionnaireItem.getTextElement());
        if (questionnaireItem.hasItem()) {
            final List<QuestionnaireResponseItemComponent> nestedResponseItems =
                    processResponseItems(questionnaireItem.getItem());
            responseItem.setItem(nestedResponseItems);
        } else if (questionnaireItem.hasInitial()) {
            responseItem = setAnswersForInitial(questionnaireItem, responseItem);
        }
        return responseItem;
    }

    QuestionnaireResponseItemComponent setAnswersForInitial(
            QuestionnaireItemComponent questionnaireItem,
            QuestionnaireResponseItemComponent questionnaireResponseItem) {
        if (questionnaireItem.hasExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR)) {
            questionnaireResponseItem.addExtension(
                    questionnaireItem.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
        }
        final QuestionnaireResponseItemAnswerComponent answer = new QuestionnaireResponseItemAnswerComponent().setValue(questionnaireItem.getInitial());
        questionnaireResponseItem.addAnswer(answer);
        return questionnaireResponseItem;
    }
}
