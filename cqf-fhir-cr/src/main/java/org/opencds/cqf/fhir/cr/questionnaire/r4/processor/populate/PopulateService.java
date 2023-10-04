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

    public IBaseResource populate(
        Questionnaire theOriginalQuestionnaire,
        Questionnaire thePrePopulatedQuestionnaire,
        String patientId
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
        response.setSubject(new Reference(new IdType("Patient", patientId)));
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

    protected List<QuestionnaireResponseItemComponent> processResponseItems(List<QuestionnaireItemComponent> items) {
        final List<QuestionnaireResponseItemComponent> responseItems = new ArrayList<>();
        items.forEach(item -> {
            final QuestionnaireResponseItemComponent populatedResponseItem = processResponseItem(item);
            responseItems.add(populatedResponseItem);
        });
        return responseItems;
    }

    protected QuestionnaireResponseItemComponent processResponseItem(QuestionnaireItemComponent item) {
        QuestionnaireResponseItemComponent responseItem = new QuestionnaireResponseItemComponent(item.getLinkIdElement());
        responseItem.setDefinition(item.getDefinition());
        responseItem.setTextElement(item.getTextElement());
        if (item.hasItem()) {
            final List<QuestionnaireResponseItemComponent> nestedResponseItems = processResponseItems(item.getItem());
            responseItem.setItem(nestedResponseItems);
        } else if (item.hasInitial()) {
            responseItem = setAnswersForInitial(item, responseItem);
        }
        return responseItem;
    }

    protected QuestionnaireResponseItemComponent setAnswersForInitial(QuestionnaireItemComponent item, QuestionnaireResponseItemComponent responseItem) {
        if (item.hasExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR)) {
            responseItem.addExtension(item.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
        }
        item.getInitial().forEach(initial -> {
            final QuestionnaireResponseItemAnswerComponent answer = new QuestionnaireResponseItemAnswerComponent().setValue(initial.getValue());
            responseItem.addAnswer(answer);
        });
        return responseItem;
    }
}
