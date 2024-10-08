package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;

import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Ids;

public class ActionResolver {

    public void resolveAction(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            IBaseResource result,
            IBaseBackboneElement action) {
        if ("Task".equals(result.fhirType())) {
            resolveTask(request, requestOrchestration, result, action);
        }
    }

    protected void resolveTask(
            ApplyRequest request, IBaseResource requestGroup, IBaseResource task, IBaseBackboneElement action) {
        var actionId = request.resolvePathString(action, "id");
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
                                requestGroup.getIdElement().getValue())));
    }
}
