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
import org.opencds.cqf.fhir.cr.questionnaireresponse.r4.defintionbasedextraction.ProcessDefinitionItem;
import org.opencds.cqf.fhir.cr.questionnaireresponse.r4.observationbasedextraction.ProcessObservationItem;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.search.Searches;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProcessorService {
    Repository repository;
    ProcessObservationItem processObservationItem;
    ProcessDefinitionItem processDefinitionItem;
    public List<IBaseResource> processItems(QuestionnaireResponse questionnaireResponse) {
        final ArrayList<IBaseResource> resources = new ArrayList<>();
        final Reference subject = questionnaireResponse.getSubject();
        final Optional<Questionnaire> questionnaire = getQuestionnaire(questionnaireResponse);
        final Map<String, List<Coding>> questionnaireCodeMap = questionnaire.map(this::createCodeMap).orElse(null);
        questionnaireResponse.getItem().forEach(item -> {
            final ProcessParameters processParameters = new ProcessParameters(
                item,
                questionnaireResponse,
                resources,
                subject,
                questionnaireCodeMap
            );
            if (processDefinitionBased(questionnaireResponse, item)) {
                processDefinitionItem.process(processParameters);
            } else if (item.hasItem()) {
                processGroupItem(processParameters);
            } else {
                processObservationItem.process(processParameters);
            }
        });
        return resources;
    }

    void processGroupItem(ProcessParameters processParameters) {
        final Reference groupSubject = getGroupSubject(processParameters.getItem(), processParameters.getSubject());
        processParameters.setSubject(groupSubject);
        if (processParameters.getItem().hasDefinition()) {
            processDefinitionItem.process(processParameters);
        } else {
            processParameters.getItem().getItem().forEach(childItem -> {
                if (!childItem.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT)) {
                    if (childItem.hasItem()) {
                        processGroupItem(processParameters);
                    } else {
                        processObservationItem.process(processParameters);
                    }
                }
            });
        }
    }

    @Nonnull
    private Reference getGroupSubject(QuestionnaireResponseItemComponent item, Reference subject) {
        final List<QuestionnaireResponseItemComponent> subjectItems = getSubjectItems(item);
        if (!subjectItems.isEmpty()) {
            return subjectItems.get(0).getAnswer().get(0).getValueReference();
        }
        return subject.copy();
    }

    @Nonnull
    private List<QuestionnaireResponseItemComponent> getSubjectItems(QuestionnaireResponseItemComponent item) {
        return item.getItem().stream()
            .filter(child -> child.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT))
            .collect(Collectors.toList());
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
        questionnaire.getItem().forEach(item -> buildQuestionnaireCodeMap(item, questionnaireCodeMap));
        return questionnaireCodeMap;
    }

    private void buildQuestionnaireCodeMap(QuestionnaireItemComponent item, Map<String, List<Coding>> questionnaireCodeMap) {
        if (item.hasItem()) {
            item.getItem().forEach(qItem -> buildQuestionnaireCodeMap(qItem, questionnaireCodeMap));
        } else {
            questionnaireCodeMap.put(item.getLinkId(), item.getCode());
        }
    }

}
