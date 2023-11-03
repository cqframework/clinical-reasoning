package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.search.Searches;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Processor {
    Repository repository;
    ProcessGroupItem processGroupItem;
    ProcessItem processItem;
    ProcessDefinitionItem processDefinitionItem;
    public List<IBaseResource> processItems(QuestionnaireResponse questionnaireResponse) {
        final ArrayList<IBaseResource> resources = new ArrayList<>();
        final Reference subject = questionnaireResponse.getSubject();
        final Optional<Questionnaire> questionnaire = getQuestionnaire(questionnaireResponse);
        final Map<String, List<Coding>> questionnaireCodeMap = questionnaire.map(this::createCodeMap).orElse(null);
        questionnaireResponse.getItem().forEach(item -> {
            if (processDefinitionBased(questionnaireResponse, item)) {
                processDefinitionItem.process(item, questionnaireResponse, resources, subject);
            } else if (item.hasItem()) {
                processGroupItem.process(item, questionnaireResponse, questionnaireCodeMap, resources, subject);
            } else {
                processItem.process(item, questionnaireResponse, questionnaireCodeMap, resources, subject);
            }
        });
        return resources;
    }

    boolean processDefinitionBased(QuestionnaireResponse questionnaireResponse, QuestionnaireResponseItemComponent item) {
        return questionnaireResponse.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT) ||
            (!item.hasItem() && item.hasDefinition());
    }

    @Nonnull
    private Optional<Questionnaire> getQuestionnaire(QuestionnaireResponse questionnaireResponse) {
        final String questionnaireCanonical = questionnaireResponse.getQuestionnaire();
        if (questionnaireCanonical != null && !questionnaireCanonical.isEmpty()) {
            final Bundle results = this.repository.search(
                Bundle.class,
                Questionnaire.class,
                Searches.byCanonical(questionnaireCanonical)
            );
            return results.hasEntry()
                ? Optional.of((Questionnaire) results.getEntryFirstRep().getResource())
                : Optional.empty();
        }
        return Optional.empty();
    }

    @Nonnull
    private Map<String, List<Coding>> createCodeMap(@Nonnull Questionnaire questionnaire) {
        var questionnaireCodeMap = new HashMap<String, List<Coding>>();
        questionnaire.getItem().forEach(item -> processQuestionnaireItems(item, questionnaireCodeMap));
        return questionnaireCodeMap;
    }

    private void processQuestionnaireItems(QuestionnaireItemComponent item, Map<String, List<Coding>> questionnaireCodeMap) {
        if (item.hasItem()) {
            item.getItem().forEach(qItem -> processQuestionnaireItems(qItem, questionnaireCodeMap));
        } else {
            questionnaireCodeMap.put(item.getLinkId(), item.getCode());
        }
    }

}
