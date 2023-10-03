package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.populate;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Constants;
import java.util.ArrayList;
import java.util.List;

public class PopulateService {

    public IBaseResource populate(
        Questionnaire theOriginalQuestionnaire,
        Questionnaire thePrePopulatedQuestionnaire,
        OperationOutcome theOperationOutcome,
        String patientId
    ) {
        final QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId(thePrePopulatedQuestionnaire.getIdPart() + "-response");

        // TODO: we are already adding the OperationOutcome to the prepopulated questionniare - lets just read it from there

//        if (!myOperationOutcome.getIssue().isEmpty()) {
//            prepopulatedQuestionnaire.addContained(myOperationOutcome);
//            prepopulatedQuestionnaire.addExtension(ExtensionBuilders.crmiMessagesExtension(myOperationOutcome.getIdPart()));
//        }

        thePrePopulatedQuestionnaire.getContained();

        if (thePrePopulatedQuestionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES) && !theOperationOutcome.getIssue().isEmpty()) {
            response.addContained(theOperationOutcome);
            response.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + theOperationOutcome.getIdPart()));
        }

        response.addContained(thePrePopulatedQuestionnaire);
        response.addExtension(ExtensionBuilders.dtrQuestionnaireResponseExtension(thePrePopulatedQuestionnaire));
        response.setQuestionnaire(theOriginalQuestionnaire.getUrl());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", patientId)));
        response.setItem(processResponseItems(thePrePopulatedQuestionnaire.getItem()));
        return response;
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
