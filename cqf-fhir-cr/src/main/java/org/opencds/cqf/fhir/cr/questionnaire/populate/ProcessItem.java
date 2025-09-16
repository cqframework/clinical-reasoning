package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;
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

    public IQuestionnaireResponseItemComponentAdapter processItem(
            PopulateRequest request, IQuestionnaireItemComponentAdapter item) {
        final var responseItem = item.newResponseItem();
        var childItems = item.getItem().stream()
                .map(IQuestionnaireItemComponentAdapter.class::cast)
                .toList();
        if (item.isGroupItem()) {
            final List<IQuestionnaireResponseItemComponentAdapter> groupChildItems = new ArrayList<>();
            childItems.forEach(childItem -> {
                groupChildItems.add(processItem(request, childItem));
            });
            responseItem.setItem(groupChildItems);
        } else {
            request.setContextVariable(responseItem.get());
            var rawParams = request.getRawParameters();
            rawParams.put("%qitem", item.get());
            populateAnswer(request, responseItem, getInitialValue(request, item, responseItem, rawParams));
            if (!childItems.isEmpty()) {
                //  child items go under each answer
                var childResponseItems =
                        childItems.stream().map(c -> processItem(request, c)).toList();
                var answers = responseItem.getAnswer();
                if (answers.isEmpty()) {
                    answers.add(responseItem.createAnswer(null));
                    responseItem.setAnswer(answers);
                }
                answers.forEach(a -> a.setItem(childResponseItems));
            }
        }
        return responseItem;
    }

    protected void populateAnswer(
            PopulateRequest request, IQuestionnaireResponseItemComponentAdapter responseItem, List<IBase> answerValue) {
        if (answerValue == null || answerValue.isEmpty()) {
            return;
        }
        var answers = new ArrayList<IQuestionnaireResponseItemAnswerComponentAdapter>();
        for (var value : answerValue) {
            answers.add(responseItem.createAnswer(transformValueToItem(request.getFhirVersion(), value)));
        }
        responseItem.setAnswer(answers);
    }

    protected List<IBase> getInitialValue(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            IQuestionnaireResponseItemComponentAdapter responseItem,
            Map<String, Object> rawParameters) {
        List<IBase> results;
        var expression = expressionProcessor.getItemInitialExpression(request, item);
        if (expression != null) {
            var itemLinkId = item.getLinkId();
            try {
                results = expressionProcessor.getExpressionResultForItem(
                        request, expression, itemLinkId, null, rawParameters);
                if (results != null && !results.isEmpty()) {
                    addAuthorExtension(request, responseItem);
                }
            } catch (Exception e) {
                var message = "Encountered error evaluating initial expression for item %s: %s"
                        .formatted(itemLinkId, e.getMessage());
                logger.error(message);
                request.logException(message);
                results = new ArrayList<>();
            }
        } else {
            results = item.getInitial().stream()
                    .map(i -> request.resolvePath(i, "value", IBase.class))
                    .collect(Collectors.toList());
        }
        return results;
    }

    protected void addAuthorExtension(PopulateRequest request, IQuestionnaireResponseItemComponentAdapter item) {
        item.addExtension(buildReferenceExt(request.getFhirVersion(), QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION, false));
    }
}
