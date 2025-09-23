package org.opencds.cqf.fhir.cr.questionnaire.populate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulateProcessor implements IPopulateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(PopulateProcessor.class);
    private final ItemProcessor itemProcessor;
    final ExpressionProcessor expressionProcessor;

    public PopulateProcessor() {
        this(null, null);
    }

    private PopulateProcessor(ItemProcessor itemProcessor, ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor != null ? expressionProcessor : new ExpressionProcessor();
        this.itemProcessor = itemProcessor != null ? itemProcessor : new ItemProcessor(this.expressionProcessor);
    }

    @Override
    public IBaseResource populate(PopulateRequest request) {
        logger.info(
                "Performing $populate operation on Questionnaire/{}",
                request.getQuestionnaire().getIdElement().getIdPart());
        // process root level variables
        request.getQuestionnaireAdapter().getItem().forEach(item -> {
            request.addQuestionnaireResponseItems(populateItem(request, item));
        });
        request.resolveOperationOutcome(
                request.getQuestionnaireResponseAdapter().get());
        logger.info("$populate operation completed");
        return request.getQuestionnaireResponseAdapter().get();
    }

    // This method is not currently used but is intended to support variable extensions for evaluation
    // This work will be done in a separate PR
    protected Map<String, Object> getVariables(PopulateRequest request, IBase element) {
        var variables = new HashMap<String, Object>();
        var expressions = request.getExtensionsByUrl(element, Constants.VARIABLE_EXTENSION).stream()
                .map(e -> CqfExpression.of(e, request.getReferencedLibraries()))
                .toList();
        expressions.forEach(expression -> {
            try {
                var result = expressionProcessor.getExpressionResult(request, expression, null, null);
                if (!result.isEmpty()) {
                    if (result.size() == 1) {
                        variables.put(expression.getName(), result.get(0));
                    } else {
                        variables.put(expression.getName(), result);
                    }
                }
            } catch (Exception e) {
                logger.error("Error encountered evaluating result for variable: {}", expression.getName());
            }
        });
        return variables;
    }

    protected List<IQuestionnaireResponseItemComponentAdapter> populateItem(
            PopulateRequest request, IQuestionnaireItemComponentAdapter item) {
        var linkId = item.getLinkId();
        logger.info("Processing item {}", linkId);

        try {
            return itemProcessor.processItem(request, item);
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return List.of(item.newResponseItem());
        }
    }
}
