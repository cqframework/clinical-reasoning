package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;

public class ProcessItemWithExtension {
    private final ExpressionProcessor expressionProcessor;

    public ProcessItemWithExtension() {
        this(new ExpressionProcessor());
    }

    private ProcessItemWithExtension(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    List<IBaseBackboneElement> processItem(PopulateRequest request, IBaseBackboneElement item)
            throws ResolveExpressionException {
        var itemLinkId = request.getItemLinkId(item);
        final CqfExpression contextExpression = expressionProcessor.getCqfExpression(
                request, item.getExtension(), Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        final List<IBase> populationContext =
                expressionProcessor.getExpressionResultForItem(request, contextExpression, itemLinkId);
        return populationContext.stream()
                .map(context -> processPopulationContext(request, item, context))
                .collect(Collectors.toList());
    }

    IBaseBackboneElement processPopulationContext(
            PopulateRequest request, IBaseBackboneElement groupItem, IBase context) {
        final IBaseBackboneElement contextItem = copyItemWithNoSubItems(request, groupItem);
        request.getItems(groupItem).forEach(item -> {
            final IBaseBackboneElement processedSubItem = createNewQuestionnaireItemComponent(request, item, context);
            request.getModelResolver().setValue(contextItem, "item", Collections.singletonList(processedSubItem));
        });
        return contextItem;
    }

    @SuppressWarnings("unchecked")
    IBaseBackboneElement createNewQuestionnaireItemComponent(
            PopulateRequest request, IBaseBackboneElement item, IBase context) {
        final IBaseBackboneElement populatedItem = SerializationUtils.clone(item);
        // request.getModelResolver().setValue(populatedItem, path, item);
        final String path = ((IPrimitiveType<String>) request.getModelResolver().resolvePath(item, "definition"))
                .getValue()
                .split("#")[1]
                .split("\\.")[1];
        final var initialValue = request.getModelResolver().resolvePath(context, path);
        // final Property initialProperty = ((Base) context).getNamedProperty(path);
        // if (initialProperty.hasValues() && !initialProperty.isList()) {
        if (initialValue instanceof IBase) {
            // final Type initialValue =
            //         transformValue((Type) initialProperty.getValues().get(0));
            // item.addExtension(buildR4(QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION));
            // item.addInitial(new QuestionnaireItemInitialComponent().setValue(initialValue));
            request.getModelResolver()
                    .setValue(
                            populatedItem,
                            "initial",
                            Collections.singletonList(createInitial(request.getFhirVersion(), (IBase) initialValue)));
            // item.addInitial(new QuestionnaireItemInitialComponent().setValue(initialValue));
        } else if (initialValue instanceof List) {
            // TODO: handle lists
        }
        return populatedItem;
    }

    IBaseBackboneElement copyItemWithNoSubItems(PopulateRequest request, IBaseBackboneElement item) {
        var clone = SerializationUtils.clone(item);
        request.getModelResolver().setValue(clone, "item", null);
        return clone;
    }

    IBaseBackboneElement createInitial(FhirVersionEnum fhirVersion, IBase value) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemOptionComponent()
                        .setValue(transformValue((org.hl7.fhir.dstu3.model.Type) value));
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent()
                        .setValue(transformValue((org.hl7.fhir.r4.model.Type) value));
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemInitialComponent()
                        .setValue(transformValue((org.hl7.fhir.r5.model.DataType) value));

            default:
                return null;
        }
    }
}
