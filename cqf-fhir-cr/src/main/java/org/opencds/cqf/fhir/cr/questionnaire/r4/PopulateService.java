package org.opencds.cqf.fhir.cr.questionnaire.r4;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
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
        var response = new QuestionnaireResponse();
        response.setId(thePrePopulatedQuestionnaire.getIdPart() + "-response");

        if (thePrePopulatedQuestionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES) && !theOperationOutcome.getIssue().isEmpty()) {
            response.addContained(theOperationOutcome);
            response.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + theOperationOutcome.getIdPart()));
        }

        response.addContained(thePrePopulatedQuestionnaire);
        response.addExtension(
            Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE,
            new Reference("#" + thePrePopulatedQuestionnaire.getIdPart())
        );
        response.setQuestionnaire(theOriginalQuestionnaire.getUrl());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", patientId)));
        response.setItem(processResponseItems(thePrePopulatedQuestionnaire.getItem()));

        return response;
    }

    protected List<QuestionnaireResponseItemComponent> processResponseItems(List<QuestionnaireItemComponent> items) {
        var responseItems = new ArrayList<QuestionnaireResponseItemComponent>();
        items.forEach(item -> {
            var responseItem = new QuestionnaireResponse.QuestionnaireResponseItemComponent(item.getLinkIdElement());
            responseItem.setDefinition(item.getDefinition());
            responseItem.setTextElement(item.getTextElement());
            if (item.hasItem()) {
                var nestedResponseItems = processResponseItems(item.getItem());
                responseItem.setItem(nestedResponseItems);
            } else if (item.hasInitial()) {
                responseItem = setAnswersForInitial(item, responseItem);
            }
            responseItems.add(responseItem);
        });
        return responseItems;
    }

    protected QuestionnaireResponseItemComponent setAnswersForInitial(QuestionnaireItemComponent item, QuestionnaireResponseItemComponent responseItem) {
        if (item.hasExtension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR)) {
            responseItem.addExtension(item.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
        }
        item.getInitial().forEach(initial -> {
            var answer = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(initial.getValue());
            responseItem.addAnswer(answer);
        });
        return responseItem;
    }

}
