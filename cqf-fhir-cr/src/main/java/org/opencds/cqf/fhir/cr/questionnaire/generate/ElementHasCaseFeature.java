package org.opencds.cqf.fhir.cr.questionnaire.generate;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;

public class ElementHasCaseFeature {
    public Object getPathValue(IOperationRequest request, IBaseResource caseFeature, ICompositeType element) {
        Object pathValue = null;
        var elementPath = request.resolvePathString(element, "path");
        var pathSplit = elementPath.split("\\.");
        if (pathSplit.length > 2) {
            pathValue = caseFeature;
            for (int i = 1; i < pathSplit.length; i++) {
                if (pathValue instanceof List) {
                    pathValue = ((List<?>) pathValue).isEmpty() ? null : ((List<?>) pathValue).get(0);
                }
                pathValue = request.getModelResolver().resolvePath(pathValue, pathSplit[i].replace("[x]", ""));
            }
        } else {
            var path = pathSplit[pathSplit.length - 1].replace("[x]", "");
            pathValue = request.getModelResolver().resolvePath(caseFeature, path);
        }
        return pathValue;
    }
}
