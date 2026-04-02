package org.opencds.cqf.fhir.cr.measure.helper;

import org.opencds.cqf.cql.engine.runtime.CqlClassInstance;

public class CqlClassInstanceHelper {

    public static String getId(CqlClassInstance cqlClassInstance) {
        if (cqlClassInstance.getType().getNamespaceURI().equals("http://hl7.org/fhir")
                && cqlClassInstance.getElements().containsKey("id")
                && cqlClassInstance.getElements().get("id") != null) {
            var resourceIdInstance =
                    (CqlClassInstance) cqlClassInstance.getElements().get("id");
            var resourceIdValue = (String) resourceIdInstance.getElements().get("value");
            if (resourceIdValue != null) {
                var type = cqlClassInstance.getType().getLocalPart();
                return "%s/%s".formatted(type, resourceIdValue);
            }
        }
        return null;
    }
}
