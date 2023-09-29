package org.opencds.cqf.fhir.cr.questionnaire.r4;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;
import org.w3._1999.xhtml.P;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opencds.cqf.fhir.cr.questionnaire.r4.ItemValueTransformer.transformValue;

public class PrePopulateService {
    final ExpressionProcessorService myExpressionProcessorService;
    final Questionnaire myQuestionnaire;
    final String myPatientId;
    OperationOutcome myOperationOutcome;

    public PrePopulateService(
        ExpressionProcessorService theExpressionProcessorService,
        Questionnaire theQuestionnaire,
        String thePatientId
    ) {
        this.myExpressionProcessorService = theExpressionProcessorService;
        this.myQuestionnaire = theQuestionnaire;
        this.myPatientId = thePatientId;
        this.myOperationOutcome = getBaseOperationOutcome();
    }

    public Questionnaire prePopulate() {
        final Questionnaire prepopulatedQuestionnaire = myQuestionnaire.copy();
        prepopulatedQuestionnaire.setId(getQuestionnaireId());
        prepopulatedQuestionnaire.addExtension(ExtensionBuilders.prepopulateSubjectExtension(myPatientId));
        final List<QuestionnaireItemComponent> processedItems = processItems(myQuestionnaire.getItem());
        prepopulatedQuestionnaire.setItem(processedItems);
        if (!myOperationOutcome.getIssue().isEmpty()) {
            prepopulatedQuestionnaire.addContained(myOperationOutcome);
            prepopulatedQuestionnaire.addExtension(ExtensionBuilders.crmiMessagesExtension(myOperationOutcome.getIdPart()));
        }
        return prepopulatedQuestionnaire;
    }

    protected OperationOutcome getBaseOperationOutcome() {
        OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome.setId("populate-outcome-" + getQuestionnaireId());
        return operationOutcome;
    }

    protected String getQuestionnaireId() {
        return this.myQuestionnaire.getIdPart() + "-" + this.myPatientId;
    }

    protected List<QuestionnaireItemComponent> processItems(List<QuestionnaireItemComponent> questionnaireItems) {
        List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
        questionnaireItems.forEach(item -> {
            if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)) {
                final List<QuestionnaireItemComponent> processedSubItems = processItemWithItemPopulationContext(item);
                populatedItems.addAll(processedSubItems);
            } else {
                QuestionnaireItemComponent populatedItem = populateSingleItem(item);
                populatedItems.add(populatedItem);
            }
        });
        return populatedItems;
    }

    protected QuestionnaireItemComponent populateSingleItem(QuestionnaireItemComponent item) {
        QuestionnaireItemComponent populatedItem = item.copy();
        if (item.hasItem()) {
            final List<QuestionnaireItemComponent> processedSubItems = processItems(item.getItem());
            populatedItem.setItem(processedSubItems);
        } else {
            final List<IBase> expressionResults = getExpressionResults(populatedItem);
            expressionResults.forEach(result -> {
                populatedItem.addExtension(ExtensionBuilders.questionnaireResponseAuthorExtension());
                populatedItem.addInitial(new Questionnaire.QuestionnaireItemInitialComponent().setValue(transformValue((Type) result)));
            });
        }
        return populatedItem;
    }

    protected List<QuestionnaireItemComponent> processItemWithItemPopulationContext(QuestionnaireItemComponent groupItem) {
        final Expression contextExpression = (Expression) groupItem
            .getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)
            .getValue();

        final List<IBase> populationContext = myExpressionProcessorService.getExpressionResult(
            contextExpression,
            groupItem.getLinkId()
        );
        return populationContext.isEmpty() ?
            Collections.singletonList(groupItem.copy()) :
            populationContext.stream()
               .map(context -> processPopulationContext(groupItem, context))
               .collect(Collectors.toList());
    }

    protected QuestionnaireItemComponent processPopulationContext(QuestionnaireItemComponent groupItem, IBase context) {
        var contextItem = groupItem.copy();
        for (var item : contextItem.getItem()) {
            var path = item.getDefinition().split("#")[1].split("\\.")[1];
            var initialProperty = ((Base) context).getNamedProperty(path);
            if (initialProperty.hasValues()) {
                if (initialProperty.isList()) {
                    // TODO: handle lists
                } else {
                    item.addExtension(ExtensionBuilders.questionnaireResponseAuthorExtension());
                    item.addInitial(new Questionnaire.QuestionnaireItemInitialComponent().setValue(transformValue((Type) initialProperty.getValues().get(0))));
                }
            }
        }
        return contextItem;
    }

    private List<IBase> getExpressionResults(QuestionnaireItemComponent item) {
        final Expression initialExpression = myExpressionProcessorService.getInitialExpression(item);
        if (initialExpression != null) {
            // evaluate expression and set the result as the initialAnswer on the item
            return myExpressionProcessorService.getExpressionResult(
                initialExpression,
                item.getLinkId()
            ).stream().filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
