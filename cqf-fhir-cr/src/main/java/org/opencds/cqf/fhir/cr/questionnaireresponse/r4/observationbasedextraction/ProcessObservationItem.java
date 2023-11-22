package org.opencds.cqf.fhir.cr.questionnaireresponse.r4.observationbasedextraction;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.questionnaireresponse.r4.ProcessParameters;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ProcessObservationItem {
    // Observation-based extraction -
    // http://build.fhir.org/ig/HL7/sdc/extraction.html#observation-based-extraction
    ObservationFactory observationFactory;
    public void process(ProcessParameters processParameters) {
        requireNonNull(processParameters.getQuestionnaireCodeMap());
        if (processParameters.getQuestionnaireCodeMap().isEmpty()) {
            throw new IllegalArgumentException(
                "Unable to retrieve Questionnaire code map for Observation based extraction");
        }
        if (processParameters.getItem().hasAnswer()) {
            processParameters.getItem().getAnswer().forEach(answer -> {
                if (answer.hasItem()) {
                    answer.getItem().forEach(answerItem -> {
                        processParameters.setItem(answerItem);
                        process(processParameters);
                    });
                } else {
                    if (processParameters.getQuestionnaireCodeMap().get(processParameters.getItem().getLinkId()) != null && !processParameters.getQuestionnaireCodeMap().get(processParameters.getItem().getLinkId()).isEmpty()) {
                        final Observation observation = observationFactory.makeObservation(
                            answer,
                            processParameters.getItem().getLinkId(),
                            processParameters.getQuestionnaireResponse(),
                            processParameters.getSubject(),
                            processParameters.getQuestionnaireCodeMap()
                        );
                        processParameters.addToResources(observation);
                    }
                }
            });
        }
    }
}
