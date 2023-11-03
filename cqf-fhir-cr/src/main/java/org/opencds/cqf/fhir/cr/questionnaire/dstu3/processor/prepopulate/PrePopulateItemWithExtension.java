package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor.prepopulate;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.buildDstu3;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.Property;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Type;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;

public class PrePopulateItemWithExtension {
    // TODO: we don't have any resources that are currently using this
    // not writing unit tests till there is a practical implementation
    private final ExpressionProcessor expressionProcessor;
    final PrePopulateHelper prePopulateHelper = new PrePopulateHelper();
    public PrePopulateItemWithExtension() {
        this(new ExpressionProcessor());
    }
    private PrePopulateItemWithExtension(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    List<QuestionnaireItemComponent> processItem(
            PrePopulateRequest prePopulateRequest,
            QuestionnaireItemComponent questionnaireItem,
            Questionnaire questionnaire) throws ResolveExpressionException
    {
        final CqfExpression contextExpression = prePopulateHelper.getExpressionByExtension(questionnaire, questionnaireItem, Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        final List<IBase> populationContext = expressionProcessor.getExpressionResult(
                prePopulateRequest, contextExpression, questionnaireItem.getLinkId());
        return populationContext.stream()
                .map(context -> processPopulationContext(questionnaireItem, context))
                .collect(Collectors.toList());
    }

    QuestionnaireItemComponent processPopulationContext(
            QuestionnaireItemComponent questionnaireGroupItem, IBase context) {
        final QuestionnaireItemComponent contextItem = copyItemWithNoSubItems(questionnaireGroupItem);
        questionnaireGroupItem.getItem().forEach(item -> {
            final QuestionnaireItemComponent processedSubItem = createNewQuestionnaireItemComponent(item, context);
            contextItem.addItem(processedSubItem);
        });
        return contextItem;
    }

    QuestionnaireItemComponent createNewQuestionnaireItemComponent(
            QuestionnaireItemComponent questionnaireItem, IBase context) {
        final QuestionnaireItemComponent item = questionnaireItem.copy();
        final String path = item.getDefinition().split("#")[1].split("\\.")[1];
        final Property initialProperty = ((Base) context).getNamedProperty(path);
        if (initialProperty.hasValues() && !initialProperty.isList()) {
            final Type initialValue =
                    transformValue((Type) initialProperty.getValues().get(0));
            item.addExtension(buildDstu3(QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION));
            item.setInitial(initialValue);
        } else {
            // TODO: handle lists
        }
        return item;
    }

    QuestionnaireItemComponent copyItemWithNoSubItems(QuestionnaireItemComponent questionnaireItem) {
        final QuestionnaireItemComponent questionnaireItemComponent = questionnaireItem.copy();
        questionnaireItemComponent.setItem(new ArrayList<>());
        return questionnaireItemComponent;
    }
}
