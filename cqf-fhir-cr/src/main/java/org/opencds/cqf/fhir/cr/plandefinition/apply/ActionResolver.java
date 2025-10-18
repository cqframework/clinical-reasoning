package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;

import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Constants.CPG_ACTIVITY_TYPE_CODE;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;

public class ActionResolver {

    public void resolveAction(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            IBaseResource result,
            IPlanDefinitionActionAdapter action) {
        if ("Task".equals(result.fhirType())) {
            resolveTask(request, requestOrchestration, result, action);
        }
    }

    protected void resolveTask(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            IBaseResource task,
            IPlanDefinitionActionAdapter action) {
        var actionId = action.getId();
        if (actionId != null) {
            var taskId = Ids.newId(request.getFhirVersion(), task.fhirType(), actionId);
            task.setId(taskId);
        }
        request.getModelResolver()
                .setValue(
                        task,
                        "basedOn",
                        Collections.singletonList(buildReference(
                                request.getFhirVersion(),
                                requestOrchestration.getIdElement().getValue())));
        if (request.getQuestionnaireAdapter() != null
                && request.resolvePath(task, "focus", IBaseReference.class) == null) {
            var codePath = request.resolvePath(task, "code");
            if (codePath != null) {
                var code = request.getAdapterFactory().createCodeableConcept(codePath);
                if (code.hasCoding(CPG_ACTIVITY_TYPE_CODE.COLLECT_INFORMATION.code)) {
                    var questionnaireAdapter = request.getQuestionnaireAdapter();
                    var questionnaireReference = buildReference(
                            request.getFhirVersion(),
                            "%s|%s".formatted(questionnaireAdapter.getUrl(), questionnaireAdapter.getVersion()));
                    request.getModelResolver().setValue(task, "focus", questionnaireReference);
                }
            }
        }
    }
}
