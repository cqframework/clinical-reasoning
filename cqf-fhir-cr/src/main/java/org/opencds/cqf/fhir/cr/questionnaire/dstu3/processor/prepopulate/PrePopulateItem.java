package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor.prepopulate;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.buildDstu3;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;

public class PrePopulateItem {
    final ExpressionProcessor expressionProcessor;

    public PrePopulateItem() {
        this(new ExpressionProcessor());
    }

    private PrePopulateItem(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    QuestionnaireItemComponent processItem(
            PrePopulateRequest prePopulateRequest,
            QuestionnaireItemComponent questionnaireItem,
            Questionnaire questionnaire)
            throws ResolveExpressionException {
        final QuestionnaireItemComponent populatedItem = copyQuestionnaireItem(questionnaireItem);
        final List<IBase> expressionResults = getExpressionResults(prePopulateRequest, questionnaire, populatedItem);
        final Optional<IBase> firstExpressionResult = expressionResults.stream().findFirst();
        if (firstExpressionResult.isPresent()) {
            populatedItem.addExtension(buildDstu3(QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION));
            populatedItem.setInitial(transformValue((Type) firstExpressionResult.get()));
        }
        return populatedItem;
    }

    QuestionnaireItemComponent copyQuestionnaireItem(QuestionnaireItemComponent questionnaireItem) {
        return questionnaireItem.copy();
    }

    List<IBase> getExpressionResults(
            PrePopulateRequest prePopulateRequest,
            Questionnaire questionnaire,
            QuestionnaireItemComponent questionnaireItem)
            throws ResolveExpressionException {
        final CqfExpression initialExpression =
                expressionProcessor.getInitialExpression(questionnaire, questionnaireItem);
        if (initialExpression != null) {
            // evaluate expression and set the result as the initialAnswer on the item
            final List<IBase> expressionResult = expressionProcessor.getExpressionResult(
                    prePopulateRequest, initialExpression, questionnaireItem.getLinkId());
            return expressionResult.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
