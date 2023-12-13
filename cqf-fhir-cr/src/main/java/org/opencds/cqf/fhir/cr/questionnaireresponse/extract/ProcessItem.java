package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class ProcessItem {
    public void processItem(
            ExtractRequest request,
            IBaseBackboneElement item,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        if (questionnaireCodeMap == null || questionnaireCodeMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to retrieve Questionnaire code map for Observation based extraction");
        }
        var answers = request.resolvePathList(item, "answer", IBaseBackboneElement.class);
        if (!answers.isEmpty()) {
            answers.forEach(answer -> {
                var answerItems = request.getItems(answer);
                if (!answerItems.isEmpty()) {
                    answerItems.forEach(
                            answerItem -> processItem(request, answerItem, questionnaireCodeMap, resources, subject));
                } else {
                    var linkId = request.resolvePathString(item, "linkId");
                    if (questionnaireCodeMap.get(linkId) != null
                            && !questionnaireCodeMap.get(linkId).isEmpty()) {
                        resources.add(createObservationFromItemAnswer(
                                request, answer, linkId, subject, questionnaireCodeMap));
                    }
                }
            });
        }
    }

    private IBaseResource createObservationFromItemAnswer(
            ExtractRequest request,
            IBaseBackboneElement answer,
            String linkId,
            IBaseReference subject,
            Map<String, List<IBaseCoding>> questionnaireCodeMap) {
        // Observation-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#observation-based-extraction
        switch (request.getFhirVersion()) {
            case DSTU3:
                return new org.opencds.cqf.fhir.cr.questionnaireresponse.extract.dstu3.ObservationResolver()
                        .resolve(request, answer, linkId, subject, questionnaireCodeMap);
            case R4:
                return new org.opencds.cqf.fhir.cr.questionnaireresponse.extract.r4.ObservationResolver()
                        .resolve(request, answer, linkId, subject, questionnaireCodeMap);
            case R5:
                return new org.opencds.cqf.fhir.cr.questionnaireresponse.extract.r5.ObservationResolver()
                        .resolve(request, answer, linkId, subject, questionnaireCodeMap);

            default:
                return null;
        }
    }
}
