package org.opencds.cqf.fhir.cql;

import static org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver.fhirModelNamespaceUri;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Value;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;

/**
 * This class provides utilities for handling ClassInstance objects from the CQL engine.
 */
public class ClassInstanceHelper {
    public static CqlFhirParametersConverter r4Converter =
            Engines.getCqlFhirParametersConverter(FhirContext.forR4Cached());
    public static final List<String> DSTU3_RESOURCE_TYPE_NAMES =
            Arrays.stream(ResourceType.values()).map(ResourceType::name).toList();
    public static final List<String> R4_RESOURCE_TYPE_NAMES = Arrays.stream(org.hl7.fhir.r4.model.ResourceType.values())
            .map(org.hl7.fhir.r4.model.ResourceType::name)
            .toList();

    private ClassInstanceHelper() {
        // intentionally empty
    }

    public static String getId(ClassInstance classInstance) {
        if (classInstance.getType().getNamespaceURI().equals(fhirModelNamespaceUri) && classInstance.has("id")) {
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

    public static IBase convertToFhirR4(Value value) {
        return r4Converter.toFhirValue(value);
    }

    public static String getClassName(ClassInstance classInstance) {
        // TODO: Need to fix version determination
        var version = "r4";
        var qName = classInstance.getType();
        var system = qName.getNamespaceURI().equals(fhirModelNamespaceUri) ? "org.hl7.fhir" : qName.getNamespaceURI();
        return "%s.%s.model.%s".formatted(system, version, qName.getLocalPart());
    }

    public static boolean isFhirResource(FhirVersionEnum fhirVersion, ClassInstance classInstance) {
        if (classInstance.getType().getNamespaceURI().equals(fhirModelNamespaceUri)) {
            var resourceTypes =
                    switch (fhirVersion) {
                        case DSTU3 -> DSTU3_RESOURCE_TYPE_NAMES;
                        case R4 -> R4_RESOURCE_TYPE_NAMES;
                        default -> List.of();
                    };
            return resourceTypes.contains(classInstance.getType().getLocalPart());
        }
        return false;
    }
}
