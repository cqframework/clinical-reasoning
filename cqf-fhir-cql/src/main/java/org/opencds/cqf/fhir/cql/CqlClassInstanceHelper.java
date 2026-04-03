package org.opencds.cqf.fhir.cql;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.CqlClassInstance;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;

public class CqlClassInstanceHelper {
    public static CqlFhirParametersConverter r4Converter =
            Engines.getCqlFhirParametersConverter(FhirContext.forR4Cached());

    private CqlClassInstanceHelper() {
        // intentionally empty
    }

    public static String getId(CqlClassInstance cqlClassInstance) {
        if (cqlClassInstance.getType().getNamespaceURI().equals(FhirModelResolver.fhirModelNamespaceUri)
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

    public static Object convertToFhirR4IfNeeded(Object value) {
        return r4Converter.convertToFhirIfNeeded(value);
    }
}
