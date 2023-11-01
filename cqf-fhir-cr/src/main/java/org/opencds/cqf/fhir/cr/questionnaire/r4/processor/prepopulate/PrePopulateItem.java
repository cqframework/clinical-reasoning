package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders;

public class PrePopulateItem {
    final ExpressionProcessor expressionProcessor;

    public PrePopulateItem() {
        this(new ExpressionProcessor());
    }
    private PrePopulateItem(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    QuestionnaireItemComponent processItem(
            PrePopulateRequest prePopulateRequest, QuestionnaireItemComponent questionnaireItem, Questionnaire questionnaire)
            throws ResolveExpressionException {
        final QuestionnaireItemComponent populatedItem = copyQuestionnaireItem(questionnaireItem);
        final List<IBase> expressionResults = getExpressionResults(prePopulateRequest, questionnaire, populatedItem);
        expressionResults.forEach(result -> {
            populatedItem.addExtension(ExtensionBuilders.questionnaireResponseAuthorExtension());
            populatedItem.addInitial(
                    new Questionnaire.QuestionnaireItemInitialComponent().setValue(transformValue((Type) result)));
        });
        return populatedItem;
    }

    QuestionnaireItemComponent copyQuestionnaireItem(QuestionnaireItemComponent questionnaireItem) {
        return questionnaireItem.copy();
    }

    List<IBase> getExpressionResults(
            PrePopulateRequest prePopulateRequest, Questionnaire questionnaire, QuestionnaireItemComponent questionnaireItem)
            throws ResolveExpressionException {
        final Expression initialExpression = expressionProcessor.getInitialExpression(questionnaireItem);
        if (initialExpression != null) {
            // evaluate expression and set the result as the initialAnswer on the item
            final List<IBase> expressionResult = expressionProcessor.getExpressionResult(
                    prePopulateRequest, initialExpression, questionnaireItem.getLinkId(), questionnaire);
            return expressionResult.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
