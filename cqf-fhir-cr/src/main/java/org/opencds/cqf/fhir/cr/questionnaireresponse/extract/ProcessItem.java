package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;

public class ProcessItem {
    public void processItem(
            ExtractRequest request,
            ItemPair itemPair,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        if (questionnaireCodeMap == null || questionnaireCodeMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to retrieve Questionnaire code map for Observation based extraction");
        }
        var categoryExt =
                itemPair.getResponseItem().getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_OBSERVATION_EXTRACT_CATEGORY);
        var answers = itemPair.getResponseItem().getAnswer();
        var questionnaireItem = itemPair.getItem();
        if (!answers.isEmpty()) {
            answers.forEach(answer -> {
                var answerItems = answer.getItem();
                if (!answerItems.isEmpty()) {
                    answerItems.forEach(answerChild -> {
                        var childPair = new ItemPair(itemPair.getItem(), answerChild);
                        processItem(request, childPair, questionnaireCodeMap, resources, subject);
                    });
                } else {
                    var linkId = itemPair.getResponseItem().getLinkId();
                    if (questionnaireCodeMap.get(linkId) != null
                            && !questionnaireCodeMap.get(linkId).isEmpty()) {
                        resources.add(createObservationFromItemAnswer(
                                request,
                                answer,
                                questionnaireItem,
                                linkId,
                                subject,
                                questionnaireCodeMap,
                                categoryExt));
                    }
                }
            });
        }
    }

    private IBaseResource createObservationFromItemAnswer(
            ExtractRequest request,
            IQuestionnaireResponseItemAnswerComponentAdapter answer,
            IQuestionnaireItemComponentAdapter questionnaireItem,
            String linkId,
            IBaseReference subject,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            IBaseExtension<?, ?> categoryExt) {
        // Observation-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#observation-based-extraction
        return switch (request.getFhirVersion()) {
            case R4 ->
                new org.opencds.cqf.fhir.cr.questionnaireresponse.extract.r4.ObservationResolver()
                        .resolve(
                                request, answer, questionnaireItem, linkId, subject, questionnaireCodeMap, categoryExt);
            case R5 ->
                new org.opencds.cqf.fhir.cr.questionnaireresponse.extract.r5.ObservationResolver()
                        .resolve(
                                request, answer, questionnaireItem, linkId, subject, questionnaireCodeMap, categoryExt);
            default -> null;
        };
    }
}
