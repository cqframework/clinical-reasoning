package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ItemValueTransformer.transformValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;

public class PrePopulateItem {
    final ExpressionProcessorService expressionProcessorService;

    public PrePopulateItem() {
        this(new ExpressionProcessorService());
    }
    private PrePopulateItem(ExpressionProcessorService expressionProcessorService) {
        this.expressionProcessorService = expressionProcessorService;
    }

    protected QuestionnaireItemComponent processItem(
            PrePopulateRequest prePopulateRequest, QuestionnaireItemComponent questionnaireItem)
            throws ResolveExpressionException {
        final QuestionnaireItemComponent populatedItem = copyQuestionnaireItem(questionnaireItem);
        final List<IBase> expressionResults = getExpressionResults(prePopulateRequest, populatedItem);
        expressionResults.forEach(result -> {
            populatedItem.addExtension(ExtensionBuilders.questionnaireResponseAuthorExtension());
            populatedItem.addInitial(
                    new Questionnaire.QuestionnaireItemInitialComponent().setValue(transformValue((Type) result)));
        });
        return populatedItem;
    }

    protected QuestionnaireItemComponent copyQuestionnaireItem(QuestionnaireItemComponent questionnaireItem) {
        return questionnaireItem.copy();
    }

    protected List<IBase> getExpressionResults(
            PrePopulateRequest prePopulateRequest, QuestionnaireItemComponent questionnaireItem)
            throws ResolveExpressionException {
        final Expression initialExpression = expressionProcessorService.getInitialExpression(questionnaireItem);
        if (initialExpression != null) {
            // evaluate expression and set the result as the initialAnswer on the item
            final List<IBase> expressionResult = expressionProcessorService.getExpressionResult(
                    prePopulateRequest, initialExpression, questionnaireItem.getLinkId());
            return expressionResult.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
