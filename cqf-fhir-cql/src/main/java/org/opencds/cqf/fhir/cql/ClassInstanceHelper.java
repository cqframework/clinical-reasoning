package org.opencds.cqf.fhir.cql;

import static org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver.fhirModelNamespaceUri;

import ca.uhn.fhir.context.FhirContext;
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
        if (isFhirResource(classInstance)) {
            var resourceIdInstance = (ClassInstance) classInstance.get("id");
            var resourceIdValue = resourceIdInstance == null ? null : resourceIdInstance.get("value");
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

    public static String getClassName(ClassInstance classInstance) {
        // TODO: Need to fix version determination
        var version = "r4";
        var qName = classInstance.getType();
        var system = qName.getNamespaceURI().equals(fhirModelNamespaceUri) ? "org.hl7.fhir" : qName.getNamespaceURI();
        return "%s.%s.model.%s".formatted(system, version, qName.getLocalPart());
    }

    public static boolean isFhirResource(ClassInstance classInstance) {
        return classInstance.getType().getNamespaceURI().equals(fhirModelNamespaceUri) && classInstance.has("id");
    }
}
