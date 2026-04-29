package org.opencds.cqf.fhir.cql;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;

/**
 * This class provides utilities for handling ClassInstance objects from the CQL engine.
 */
public class ClassInstanceHelper {
    public static CqlFhirParametersConverter r4Converter =
            Engines.getCqlFhirParametersConverter(FhirContext.forR4Cached());

    private ClassInstanceHelper() {
        // intentionally empty
    }

    public static String getId(ClassInstance classInstance) {
        if (classInstance.getType().getNamespaceURI().equals(FhirModelResolver.fhirModelNamespaceUri)
                && classInstance.has("id")) {
            var resourceIdInstance = classInstance.get("id");
            var resourceIdValue = resourceIdInstance == null ? null : resourceIdInstance.toString();
            if (resourceIdValue != null) {
                var type = classInstance.getType().getLocalPart();
                return "%s/%s".formatted(type, resourceIdValue);
            }
        }
        return null;
    }

    public static Object convertToFhirR4IfNeeded(Object value) {
        return r4Converter.convertToFhirIfNeeded(value);
    }
}
