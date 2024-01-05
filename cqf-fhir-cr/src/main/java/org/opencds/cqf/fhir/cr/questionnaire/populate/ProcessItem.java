package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.build;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;

public class ProcessItem {
    final ExpressionProcessor expressionProcessor;

    public ProcessItem() {
        this(new ExpressionProcessor());
    }

    private ProcessItem(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    IBaseBackboneElement processItem(PopulateRequest request, IBaseBackboneElement item)
            throws ResolveExpressionException {
        final var populatedItem = SerializationUtils.clone(item);
        final List<IBase> expressionResults = expressionProcessor.getExpressionResultForItem(request, item);
        if (!expressionResults.isEmpty()) {
            request.getModelResolver()
                    .setValue(
                            populatedItem,
                            "extension",
                            Collections.singletonList(
                                    build(request.getFhirVersion(), QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION)));
            List<IBaseBackboneElement> initial = new ArrayList<>();
            expressionResults.forEach(result -> {
                initial.add(createInitial(request.getFhirVersion(), result));
            });
            request.getModelResolver().setValue(populatedItem, "initial", initial);
        }
        return populatedItem;
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
