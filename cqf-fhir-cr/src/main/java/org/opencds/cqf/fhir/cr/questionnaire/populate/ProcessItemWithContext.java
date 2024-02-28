package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;

public class ProcessItemWithContext {
    private final ExpressionProcessor expressionProcessor;

    public ProcessItemWithContext() {
        this(new ExpressionProcessor());
    }

    private ProcessItemWithContext(ExpressionProcessor expressionProcessor) {
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
        final String path = ((IPrimitiveType<String>) request.getModelResolver().resolvePath(item, "definition"))
                .getValue()
                .split("#")[1]
                .split("\\.")[1]
                .replace("[x]", "");
        final var initialValue = request.getModelResolver().resolvePath(context, path);
        if (initialValue != null) {
            request.getModelResolver()
                    .setValue(
                            populatedItem,
                            "extension",
                            Collections.singletonList(buildReferenceExt(
                                    request.getFhirVersion(), QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION, false)));
        }
        if (initialValue instanceof IBase) {
            request.getModelResolver()
                    .setValue(
                            populatedItem,
                            "initial",
                            Collections.singletonList(createInitial(request.getFhirVersion(), (IBase) initialValue)));
        } else if (initialValue instanceof List) {
            var initials = new ArrayList<>();
            for (var value : (List<IBase>) initialValue) {
                initials.add(createInitial(request.getFhirVersion(), (IBase) value));
            }
            request.getModelResolver().setValue(populatedItem, "initial", initials);
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
                        .setValue(transformValueToItem((org.hl7.fhir.dstu3.model.Type) value));
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent()
                        .setValue(transformValueToItem((org.hl7.fhir.r4.model.Type) value));
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemInitialComponent()
                        .setValue(transformValueToItem((org.hl7.fhir.r5.model.DataType) value));

            default:
                return null;
        }
    }
}
