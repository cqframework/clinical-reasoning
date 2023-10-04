package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.populate;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PopulateService {
    public static PopulateService of() {
        return new PopulateService();
    }
    private PopulateService() {}

    public IBaseResource populate(
        Questionnaire theOriginalQuestionnaire,
        Questionnaire thePrePopulatedQuestionnaire,
        String thePatientId
    ) {
        final QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId(thePrePopulatedQuestionnaire.getIdPart() + "-response");
        final Optional<OperationOutcome> operationOutcome = getOperationOutcomeFromPrePopulatedQuestionnaire(thePrePopulatedQuestionnaire);
        if (thePrePopulatedQuestionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES) && operationOutcome.isPresent()) {
            response.addContained(operationOutcome.get());
            response.addExtension(ExtensionBuilders.crmiMessagesExtension(operationOutcome.get().getIdPart()));
        }
        response.addContained(thePrePopulatedQuestionnaire);
        response.addExtension(ExtensionBuilders.dtrQuestionnaireResponseExtension(thePrePopulatedQuestionnaire));
        response.setQuestionnaire(theOriginalQuestionnaire.getUrl());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", thePatientId)));
        response.setItem(processResponseItems(thePrePopulatedQuestionnaire.getItem()));
        return response;
    }

    protected Optional<OperationOutcome> getOperationOutcomeFromPrePopulatedQuestionnaire(Questionnaire thePrePopulatedQuestionnaire) {
        return thePrePopulatedQuestionnaire.getContained().stream()
            .filter(theResource -> theResource.getResourceType() == ResourceType.OperationOutcome)
            .map(OperationOutcome.class::cast)
            .filter(this::filterOperationOutcome)
            .findFirst();
    }

    protected boolean filterOperationOutcome(OperationOutcome theOperationOutcome) {
        if (theOperationOutcome.hasIssue()) {
            final List<OperationOutcomeIssueComponent> filteredIssues = theOperationOutcome.getIssue().stream()
                .filter(theIssue -> theIssue.getCode() == OperationOutcome.IssueType.EXCEPTION && theIssue.getSeverity() == OperationOutcome.IssueSeverity.ERROR)
                .collect(Collectors.toList());
            return !filteredIssues.isEmpty();
        }
        return false;
    }

    protected List<QuestionnaireResponseItemComponent> processResponseItems(List<QuestionnaireItemComponent> theQuestionnaireItems) {
        final List<QuestionnaireResponseItemComponent> responseItems = new ArrayList<>();
        theQuestionnaireItems.forEach(item -> {
            final QuestionnaireResponseItemComponent populatedResponseItem = processResponseItem(item);
            responseItems.add(populatedResponseItem);
        });
        return responseItems;
    }

    protected QuestionnaireResponseItemComponent processResponseItem(QuestionnaireItemComponent theQuestionnaireItem) {
        QuestionnaireResponseItemComponent responseItem = new QuestionnaireResponseItemComponent(theQuestionnaireItem.getLinkIdElement());
        responseItem.setDefinition(theQuestionnaireItem.getDefinition());
        responseItem.setTextElement(theQuestionnaireItem.getTextElement());
        if (theQuestionnaireItem.hasItem()) {
            final List<QuestionnaireResponseItemComponent> nestedResponseItems = processResponseItems(theQuestionnaireItem.getItem());
            responseItem.setItem(nestedResponseItems);
        } else if (theQuestionnaireItem.hasInitial()) {
            responseItem = setAnswersForInitial(theQuestionnaireItem, responseItem);
        }
        return responseItem;
    }

    protected QuestionnaireResponseItemComponent setAnswersForInitial(
        QuestionnaireItemComponent theQuestionnaireItem,
        QuestionnaireResponseItemComponent theQuestionnaireResponseItem
    ) {
        if (theQuestionnaireItem.hasExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR)) {
            theQuestionnaireResponseItem.addExtension(theQuestionnaireItem.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
        }
        theQuestionnaireItem.getInitial().forEach(initial -> {
            final QuestionnaireResponseItemAnswerComponent answer = new QuestionnaireResponseItemAnswerComponent().setValue(initial.getValue());
            theQuestionnaireResponseItem.addAnswer(answer);
        });
        return theQuestionnaireResponseItem;
    }
}
