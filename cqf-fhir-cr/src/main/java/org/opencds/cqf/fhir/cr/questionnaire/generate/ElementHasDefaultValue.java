package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildBooleanExt;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildBooleanType;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.sdcQuestionnaireHidden;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createInitial;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;

public class ElementHasDefaultValue {
    public IBaseBackboneElement addProperties(
            IOperationRequest request, IBaseDatatype value, IBaseBackboneElement questionnaireItem) {
        var initial = createInitial(request, value);
        request.getModelResolver().setValue(questionnaireItem, "initial", initial);
        request.getModelResolver()
                .setValue(
                        questionnaireItem,
                        "extension",
                        buildBooleanExt(request.getFhirVersion(), sdcQuestionnaireHidden(Boolean.TRUE)));
        // Collections.singletonList(buildBooleanExt(request.getFhirVersion(), sdcQuestionnaireHidden(Boolean.TRUE))));
        request.getModelResolver()
                .setValue(questionnaireItem, "readOnly", buildBooleanType(request.getFhirVersion(), Boolean.TRUE));
        return questionnaireItem;
    }
}
