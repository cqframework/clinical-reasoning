package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ItemValueTransformer.transformValue;

public class PrePopulateItemWithExtension {
    private final ExpressionProcessorService myExpressionProcessorService;

    public PrePopulateItemWithExtension() {
        // TODO: we don't have any resources that are currently using this
        // not writing unit tests till there is a practical implementation
        myExpressionProcessorService = new ExpressionProcessorService();
    }

    protected List<QuestionnaireItemComponent> processItem(
        PrePopulateRequest thePrePopulateRequest,
        QuestionnaireItemComponent groupItem
    ) throws ResolveExpressionException {
        final Expression contextExpression = (Expression) groupItem
            .getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)
            .getValue();
        final List<IBase> populationContext = myExpressionProcessorService.getExpressionResult(
            thePrePopulateRequest,
            contextExpression,
            groupItem.getLinkId()
        );
        return populationContext.stream()
            .map(context -> processPopulationContext(groupItem, context))
            .collect(Collectors.toList());
    }

    protected QuestionnaireItemComponent processPopulationContext(QuestionnaireItemComponent groupItem, IBase context) {
        final QuestionnaireItemComponent contextItem = copyItemWithNoSubItems(groupItem);
        groupItem.getItem().forEach(item -> {
            final QuestionnaireItemComponent processedSubItem = createNewQuestionnaireItemComponent(item, context);
            contextItem.addItem(processedSubItem);
        });
        return contextItem;
    }

    protected QuestionnaireItemComponent createNewQuestionnaireItemComponent(QuestionnaireItemComponent theQuestionnaireItemComponent, IBase context) {
        final QuestionnaireItemComponent item = theQuestionnaireItemComponent.copy();
        final String path = item.getDefinition().split("#")[1].split("\\.")[1];
        final Property initialProperty = ((Base) context).getNamedProperty(path);
        if (initialProperty.hasValues() && !initialProperty.isList()) {
            final Type initialValue = transformValue((Type) initialProperty.getValues().get(0));
            item.addExtension(ExtensionBuilders.questionnaireResponseAuthorExtension());
            item.addInitial(new QuestionnaireItemInitialComponent().setValue(initialValue));
        } else {
            // TODO: handle lists
        }
        return item;
    }

    protected QuestionnaireItemComponent copyItemWithNoSubItems(QuestionnaireItemComponent theItem) {
        final QuestionnaireItemComponent questionnaireItemComponent = theItem.copy();
        questionnaireItemComponent.setItem(new ArrayList<>());
        return questionnaireItemComponent;
    }
}
