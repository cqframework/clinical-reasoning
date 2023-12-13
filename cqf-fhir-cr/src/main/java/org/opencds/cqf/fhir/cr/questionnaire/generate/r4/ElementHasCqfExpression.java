package org.opencds.cqf.fhir.cr.questionnaire.generate.r4;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ItemValueTransformer.transformValue;

import java.util.List;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class ElementHasCqfExpression {
    protected final ExpressionProcessor expressionProcessor;

    public ElementHasCqfExpression() {
        expressionProcessor = new ExpressionProcessor();
    }

    public QuestionnaireItemComponent addProperties(
            IOperationRequest request, ElementDefinition element, QuestionnaireItemComponent questionnaireItem) {
        final var expression =
                expressionProcessor.getCqfExpression(request, element.getExtension(), Constants.CQF_EXPRESSION);
        final List<IBase> results = expressionProcessor.getExpressionResult(request, expression);
        results.forEach(result -> {
            if (Resource.class.isAssignableFrom(result.getClass())) {
                addResourceValue(result, questionnaireItem);
            } else {
                addTypeValue(result, questionnaireItem);
            }
        });
        return questionnaireItem;
    }

    protected void addResourceValue(IBase result, QuestionnaireItemComponent questionnaireItem) {
        final IAnyResource resource = (IAnyResource) result;
        final Reference reference = new Reference(resource);
        questionnaireItem.addInitial().setValue(reference);
    }

    protected void addTypeValue(IBase result, QuestionnaireItemComponent questionnaireItem) {
        final Type type = transformValue((Type) result);
        questionnaireItem.addInitial().setValue(type);
    }
}
