package org.opencds.cqf.fhir.cr.questionnaire.populate;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.opencds.cqf.fhir.cr.questionnaire.common.PopulateRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class ProcessResponseItem {
    public ProcessResponseItem() {}

    public List<IBaseBackboneElement> processItems(PopulateRequest request, List<IBaseBackboneElement> items) {
        return items.stream().map(i -> processItem(request, i)).collect(Collectors.toList());
    }

    protected IBaseBackboneElement processItem(PopulateRequest request, IBaseBackboneElement item) {
        var responseItem = createResponseItem(request.getFhirVersion(), item);
        var items = request.getItems(item);
        if (!items.isEmpty()) {
            final List<IBaseBackboneElement> nestedResponseItems = processItems(request, items);
            request.getModelResolver().setValue(responseItem, "item", nestedResponseItems);
        } else {
            var authorExt = item.getExtension().stream()
                    .filter(e -> e.getUrl().equals(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR))
                    .findFirst()
                    .orElse(null);
            if (authorExt != null) {
                request.getModelResolver().setValue(responseItem, "extension", Collections.singletonList(authorExt));
            }
            var initial = request.resolvePathList(item, "initial").stream()
                    .map(i -> (IBaseBackboneElement) i)
                    .collect(Collectors.toList());
            if (!initial.isEmpty()) {
                responseItem = setAnswersForInitial(request, initial, responseItem);
            }
        }
        return responseItem;
    }

    protected IBaseBackboneElement setAnswersForInitial(
            PopulateRequest request, List<IBaseBackboneElement> initial, IBaseBackboneElement responseItem) {
        initial.forEach(i -> {
            final var answer = createAnswer(request.getFhirVersion(), request.resolvePath(i, "value"));
            request.getModelResolver().setValue(responseItem, "answer", Collections.singletonList(answer));
        });
        return responseItem;
    }

    protected IBaseBackboneElement createAnswer(FhirVersionEnum fhirVersion, IBase value) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                        .setValue((org.hl7.fhir.dstu3.model.Type) value);
            case R4:
                return new org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                        .setValue((org.hl7.fhir.r4.model.Type) value);
            case R5:
                return new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                        .setValue((org.hl7.fhir.r5.model.DataType) value);

            default:
                return null;
        }
    }

    protected IBaseBackboneElement createResponseItem(FhirVersionEnum fhirVersion, IBaseBackboneElement item) {
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Item = (org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent) item;
                return new org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(
                                dstu3Item.getLinkIdElement())
                        .setDefinitionElement(dstu3Item.getDefinitionElement())
                        .setTextElement(dstu3Item.getTextElement());
            case R4:
                var r4Item = (org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent) item;
                return new org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(
                                r4Item.getLinkIdElement())
                        .setDefinitionElement(r4Item.getDefinitionElement())
                        .setTextElement(r4Item.getTextElement());
            case R5:
                var r5Item = (org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent) item;
                return new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(
                                r5Item.getLinkId())
                        .setDefinitionElement(r5Item.getDefinitionElement())
                        .setTextElement(r5Item.getTextElement());

            default:
                return null;
        }
    }
}
