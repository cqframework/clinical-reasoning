package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.questionnaireresponse.r4.defintionbased.ProcessDefinitionItem;
import org.opencds.cqf.fhir.cr.questionnaireresponse.r4.observationbased.ProcessItem;
import org.opencds.cqf.fhir.utility.Constants;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ProcessGroupItem {
    ProcessItem processItem;
    ProcessDefinitionItem processDefinitionItem;
    void process(
        QuestionnaireResponseItemComponent item,
        QuestionnaireResponse questionnaireResponse,
        Map<String, List<Coding>> questionnaireCodeMap,
        List<IBaseResource> resources,
        Reference subject
    ) {
        final List<QuestionnaireResponseItemComponent> subjectItems = item.getItem().stream()
            .filter(child -> child.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT))
            .collect(Collectors.toList());
        final Reference groupSubject = !subjectItems.isEmpty() ? subjectItems.get(0).getAnswer().get(0).getValueReference() : subject.copy();
        if (item.hasDefinition()) {
            processDefinitionItem.process(item, questionnaireResponse, resources, groupSubject);
        } else {
            item.getItem().forEach(childItem -> {
                if (!childItem.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT)) {
                    if (childItem.hasItem()) {
                        process(childItem, questionnaireResponse, questionnaireCodeMap, resources, groupSubject);
                    } else {
                        processItem.process(childItem, questionnaireResponse, questionnaireCodeMap, resources, groupSubject);
                    }
                }
            });
        }
    }
}