package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;

public class CodeMap {
    // this is based on "if a questionnaire.item has items then this item is a
    // header and will not have a specific code to be used with an answer"
    public static Map<String, List<IBaseCoding>> create(ExtractRequest request) {
        if (request.getQuestionnaire() == null) {
            return null;
        }
        var questionnaireCodeMap = new HashMap<String, List<IBaseCoding>>();
        request.getItems(request.getQuestionnaire())
                .forEach(item -> processQuestionnaireItems(request, item, questionnaireCodeMap));

        return questionnaireCodeMap;
    }

    private static void processQuestionnaireItems(
            IOperationRequest request, IBaseBackboneElement item, Map<String, List<IBaseCoding>> questionnaireCodeMap) {
        var childItems = request.getItems(item);
        if (!childItems.isEmpty()) {
            childItems.forEach(child -> processQuestionnaireItems(request, child, questionnaireCodeMap));
        } else {
            var linkId = request.resolvePathString(item, "linkId");
            var codes = request.resolvePathList(item, "code").stream()
                    .map(c -> (IBaseCoding) c)
                    .collect(Collectors.toList());
            questionnaireCodeMap.put(linkId, codes);
        }
    }
}
