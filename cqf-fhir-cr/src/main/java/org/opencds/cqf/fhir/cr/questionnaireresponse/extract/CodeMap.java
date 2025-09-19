package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;

public class CodeMap {
    private CodeMap() {}

    // this is based on "if a questionnaire.item has items then this item is a
    // header and will not have a specific code to be used with an answer"
    public static Map<String, List<IBaseCoding>> create(ExtractRequest request) {
        if (request.getQuestionnaire() == null) {
            return Collections.emptyMap();
        }
        var questionnaireCodeMap = new HashMap<String, List<IBaseCoding>>();
        if (request.hasQuestionnaire()) {
            request.getQuestionnaireAdapter()
                    .getItem()
                    .forEach(item -> processQuestionnaireItems(request, item, questionnaireCodeMap));
        }
        return questionnaireCodeMap;
    }

    private static void processQuestionnaireItems(
            IQuestionnaireRequest request,
            IQuestionnaireItemComponentAdapter item,
            Map<String, List<IBaseCoding>> questionnaireCodeMap) {
        if (item.hasItem()) {
            item.getItem().stream()
                    .map(IQuestionnaireItemComponentAdapter.class::cast)
                    .forEach(child -> processQuestionnaireItems(request, child, questionnaireCodeMap));
        } else {
            questionnaireCodeMap.put(item.getLinkId(), item.getCode());
        }
    }
}
