package org.opencds.cqf.fhir.cr.questionnaireresponse.r4.observationbased;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ProcessItem {
    ObservationBuilder observationBuilder;
    public void process(
        QuestionnaireResponseItemComponent item,
        QuestionnaireResponse questionnaireResponse,
        Map<String, List<Coding>> questionnaireCodeMap,
        List<IBaseResource> resources,
        Reference subject
    ) {
        requireNonNull(questionnaireCodeMap);
        if (questionnaireCodeMap.isEmpty()) {
            throw new IllegalArgumentException(
                "Unable to retrieve Questionnaire code map for Observation based extraction");
        }
        if (item.hasAnswer()) {
            item.getAnswer().forEach(answer -> {
                if (answer.hasItem()) {
                    answer.getItem().forEach(answerItem -> process(
                        answerItem,
                        questionnaireResponse,
                        questionnaireCodeMap,
                        resources,
                        subject)
                    );
                } else {
                    if (questionnaireCodeMap.get(item.getLinkId()) != null && !questionnaireCodeMap.get(item.getLinkId()).isEmpty()) {
                        resources.add(observationBuilder.build(
                            answer,
                            item.getLinkId(),
                            questionnaireResponse,
                            subject,
                            questionnaireCodeMap
                        ));
                    }
                }
            });
        }
    }
}
