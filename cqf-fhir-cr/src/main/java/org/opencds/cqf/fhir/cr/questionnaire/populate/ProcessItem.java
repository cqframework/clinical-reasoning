package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessItem {
    private static final Logger logger = LoggerFactory.getLogger(ProcessItem.class);
    final ExpressionProcessor expressionProcessor;

    public ProcessItem() {
        this(new ExpressionProcessor());
    }

    public ProcessItem(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    public IBaseBackboneElement processItem(PopulateRequest request, IBaseBackboneElement item) {
        final var responseItem = createResponseItem(request.getFhirVersion(), item);
        request.setContextVariable(responseItem);
        var rawParams = request.getRawParameters();
        rawParams.put("%qitem", item);
        populateAnswer(request, responseItem, getInitialValue(request, item, responseItem, rawParams));
        return responseItem;
    }

    protected void populateAnswer(PopulateRequest request, IBaseBackboneElement responseItem, List<IBase> answerValue) {
        if (answerValue == null || answerValue.isEmpty()) {
            return;
        }
        var answers = new ArrayList<>();
        for (var value : answerValue) {
            answers.add(createAnswer(request.getFhirVersion(), value));
        }
        request.getModelResolver().setValue(responseItem, "answer", answers);
    }

    protected List<IBase> getInitialValue(
            PopulateRequest request,
            IBaseBackboneElement item,
            IBaseBackboneElement responseItem,
            Map<String, Object> rawParameters) {
        List<IBase> results;
        var expression = expressionProcessor.getItemInitialExpression(request, item);
        if (expression != null) {
            var itemLinkId = request.getItemLinkId(item);
            try {
                results = expressionProcessor.getExpressionResultForItem(
                        request, expression, itemLinkId, null, rawParameters);
                if (results != null && !results.isEmpty()) {
                    addAuthorExtension(request, responseItem);
                }
            } catch (Exception e) {
                var message = String.format(
                        "Encountered error evaluating initial expression for item %s: %s", itemLinkId, e.getMessage());
                logger.error(message);
                request.logException(message);
                results = new ArrayList<>();
            }
        } else {
            results = request.resolvePathList(item, "initial", IBaseBackboneElement.class).stream()
                    .map(i -> request.resolvePath(i, "value", IBase.class))
                    .collect(Collectors.toList());
        }
        return results;
    }

    protected void addAuthorExtension(PopulateRequest request, IBaseBackboneElement item) {
        request.getModelResolver()
                .setValue(
                        item,
                        "extension",
                        Collections.singletonList(buildReferenceExt(
                                request.getFhirVersion(), QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION, false)));
    }

    protected IBaseBackboneElement createAnswer(FhirVersionEnum fhirVersion, IBase value) {
        return switch (fhirVersion) {
            case R4 -> new org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                    .setValue(transformValueToItem((org.hl7.fhir.r4.model.Type) value));
            case R5 -> new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                    .setValue(transformValueToItem((org.hl7.fhir.r5.model.DataType) value));
            default -> null;
        };
    }

    protected IBaseBackboneElement createResponseItem(FhirVersionEnum fhirVersion, IBaseBackboneElement item) {
        return switch (fhirVersion) {
            case R4 -> {
                var r4Item = (org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent) item;
                yield new org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(
                                r4Item.getLinkIdElement())
                        .setDefinitionElement(r4Item.getDefinitionElement())
                        .setTextElement(r4Item.getTextElement());
            }
            case R5 -> {
                var r5Item = (org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent) item;
                yield new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(
                                r5Item.getLinkId())
                        .setDefinitionElement(r5Item.getDefinitionElement())
                        .setTextElement(r5Item.getTextElement());
            }
            default -> null;
        };
    }
}
