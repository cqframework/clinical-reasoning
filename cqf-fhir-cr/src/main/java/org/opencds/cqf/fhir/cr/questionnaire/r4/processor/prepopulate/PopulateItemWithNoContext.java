package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ItemValueTransformer.transformValue;

public class PopulateItemWithNoContext {
    final ExpressionProcessorService myExpressionProcessorService;

    public PopulateItemWithNoContext() {
        // TODO: renaming: PopulateItem (vs. PopulateItemWithExtension)
        myExpressionProcessorService = new ExpressionProcessorService();
    }

    protected QuestionnaireItemComponent processItem(
        PrePopulateRequest thePrePopulateRequest,
        QuestionnaireItemComponent item
    ) throws ResolveExpressionException {
        final QuestionnaireItemComponent populatedItem = item.copy();
        final List<IBase> expressionResults = getExpressionResults(thePrePopulateRequest, populatedItem);
        expressionResults.forEach(result -> {
            populatedItem.addExtension(ExtensionBuilders.questionnaireResponseAuthorExtension());
            populatedItem.addInitial(new Questionnaire.QuestionnaireItemInitialComponent().setValue(transformValue((Type) result)));
        });
        return populatedItem;
    }

    private List<IBase> getExpressionResults(
        PrePopulateRequest thePrePopulateRequest,
        QuestionnaireItemComponent item
    ) throws ResolveExpressionException {
        final Expression initialExpression = myExpressionProcessorService.getInitialExpression(item);
        if (initialExpression != null) {
            // evaluate expression and set the result as the initialAnswer on the item
            final List<IBase> expressionResult = myExpressionProcessorService.getExpressionResult(
                thePrePopulateRequest,
                initialExpression,
                item.getLinkId()
            );
            return expressionResult.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
