package org.opencds.cqf.fhir.cr.questionnaire.populate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulateProcessor implements IPopulateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(PopulateProcessor.class);
    private final ProcessItem processItem;
    private final ProcessItemWithContext processItemWithContext;
    final ExpressionProcessor expressionProcessor;

    public PopulateProcessor() {
        this(new ProcessItem(), new ProcessItemWithContext(), new ExpressionProcessor());
    }

    private PopulateProcessor(
            ProcessItem processItem,
            ProcessItemWithContext processItemWithExtension,
            ExpressionProcessor expressionProcessor) {
        this.processItem = processItem;
        this.processItemWithContext = processItemWithExtension;
        this.expressionProcessor = expressionProcessor;
    }

    @Override
    public IBaseResource populate(PopulateRequest request) {
        logger.info(
                "Performing $populate operation on Questionnaire/{}",
                request.getQuestionnaire().getIdElement().getIdPart());
        // process root level variables
        request.getItems(request.getQuestionnaire()).forEach(item -> {
            request.addQuestionnaireResponseItems(populateItem(request, item));
        });
        request.resolveOperationOutcome(request.getQuestionnaireResponse());
        logger.info("$populate operation completed");
        return request.getQuestionnaireResponse();
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

    protected List<IBaseBackboneElement> populateItem(PopulateRequest request, IBaseBackboneElement item) {
        var linkId = request.getItemLinkId(item);
        logger.info("Processing item {}", linkId);
        var populationContextExt = item.getExtension().stream()
                .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT))
                .findFirst()
                .orElse(null);
        return populationContextExt != null
                ? processItemWithContext(request, item)
                : List.of(processItem(request, item));
    }

    protected List<IBaseBackboneElement> processItemWithContext(PopulateRequest request, IBaseBackboneElement item) {
        try {
            return processItemWithContext.processContextItem(request, item);
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return List.of();
        }
    }

    protected IBaseBackboneElement processItem(PopulateRequest request, IBaseBackboneElement item) {
        try {
            return processItem.processItem(request, item);
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return processItem.createResponseItem(request.getFhirVersion(), item);
        }
    }
}
