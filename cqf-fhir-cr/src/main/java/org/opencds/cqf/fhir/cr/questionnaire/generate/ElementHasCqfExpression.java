package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createInitial;

import ca.uhn.fhir.model.api.IElement;
import java.util.List;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class ElementHasCqfExpression {
    protected final ExpressionProcessor expressionProcessor;

    public ElementHasCqfExpression() {
        expressionProcessor = new ExpressionProcessor();
    }

    public IBaseBackboneElement addProperties(
            IOperationRequest request, IElement element, IBaseBackboneElement questionnaireItem) {
        final var expression =
                expressionProcessor.getCqfExpression(request, request.getExtensions(element), Constants.CQF_EXPRESSION);
        final List<IBase> results = expressionProcessor.getExpressionResult(request, expression);
        results.forEach(result -> {
            if (IAnyResource.class.isAssignableFrom(result.getClass())) {
                addResourceValue(request, (IAnyResource) result, questionnaireItem);
            } else {
                addTypeValue(request, result, questionnaireItem);
            }
        });
        return questionnaireItem;
    }

    protected void addResourceValue(
            IOperationRequest request, IAnyResource resource, IBaseBackboneElement questionnaireItem) {
        final var reference = buildReference(request.getFhirVersion(), resource);
        var initial = createInitial(request, reference);
        request.getModelResolver().setValue(questionnaireItem, "initial", initial);
    }

    protected void addTypeValue(IOperationRequest request, IBase result, IBaseBackboneElement questionnaireItem) {
        var initial = createInitial(request, (IBaseDatatype) result);
        request.getModelResolver().setValue(questionnaireItem, "initial", initial);
    }
}
