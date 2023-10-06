package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ItemValueTransformer.transformValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Constants;

public class PrePopulateItemWithExtension {
    // TODO: we don't have any resources that are currently using this
    // not writing unit tests till there is a practical implementation
    private final ExpressionProcessorService myExpressionProcessorService;

    public static PrePopulateItemWithExtension of() {
        final ExpressionProcessorService expressionProcessorService = ExpressionProcessorService.of();
        return new PrePopulateItemWithExtension(expressionProcessorService);
    }

    private PrePopulateItemWithExtension(ExpressionProcessorService theExpressionProcessorService) {
        myExpressionProcessorService = theExpressionProcessorService;
    }

    protected List<QuestionnaireItemComponent> processItem(
            PrePopulateRequest thePrePopulateRequest, QuestionnaireItemComponent theQuestionnaireGroupItem)
            throws ResolveExpressionException {
        final Expression contextExpression = (Expression) theQuestionnaireGroupItem
                .getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)
                .getValue();
        final List<IBase> populationContext = myExpressionProcessorService.getExpressionResult(
                thePrePopulateRequest, contextExpression, theQuestionnaireGroupItem.getLinkId());
        return populationContext.stream()
                .map(context -> processPopulationContext(theQuestionnaireGroupItem, context))
                .collect(Collectors.toList());
    }

    protected QuestionnaireItemComponent processPopulationContext(
            QuestionnaireItemComponent theQuestionnaireGroupItem, IBase theContext) {
        final QuestionnaireItemComponent contextItem = copyItemWithNoSubItems(theQuestionnaireGroupItem);
        theQuestionnaireGroupItem.getItem().forEach(item -> {
            final QuestionnaireItemComponent processedSubItem = createNewQuestionnaireItemComponent(item, theContext);
            contextItem.addItem(processedSubItem);
        });
        return contextItem;
    }

    protected QuestionnaireItemComponent createNewQuestionnaireItemComponent(
            QuestionnaireItemComponent theQuestionnaireItemComponent, IBase theContext) {
        final QuestionnaireItemComponent item = theQuestionnaireItemComponent.copy();
        final String path = item.getDefinition().split("#")[1].split("\\.")[1];
        final Property initialProperty = ((Base) theContext).getNamedProperty(path);
        if (initialProperty.hasValues() && !initialProperty.isList()) {
            final Type initialValue =
                    transformValue((Type) initialProperty.getValues().get(0));
            item.addExtension(ExtensionBuilders.questionnaireResponseAuthorExtension());
            item.addInitial(new QuestionnaireItemInitialComponent().setValue(initialValue));
        } else {
            // TODO: handle lists
        }
        return item;
    }

    protected QuestionnaireItemComponent copyItemWithNoSubItems(QuestionnaireItemComponent theQuestionnaireItem) {
        final QuestionnaireItemComponent questionnaireItemComponent = theQuestionnaireItem.copy();
        questionnaireItemComponent.setItem(new ArrayList<>());
        return questionnaireItemComponent;
    }
}
