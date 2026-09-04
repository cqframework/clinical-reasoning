package org.opencds.cqf.fhir.cr.measure.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Value;
import org.opencds.cqf.fhir.cql.ClassInstanceHelper;

/**
 * Builds the evaluated-resource map an {@link ExpressionResult} carries.
 * <p>
 * The engine keys these by resource id, so this keys them the same way rather than inventing keys:
 * a fake that agrees with the real thing only on the values would hide anything that later starts
 * reading the keys.
 */
final class TestEvaluatedResources {

    private TestEvaluatedResources() {}

    static Map<String, Value> of(Value... resources) {
        var resourcesById = new LinkedHashMap<String, Value>();
        for (Value resource : resources) {
            resourcesById.put(ClassInstanceHelper.getId((ClassInstance) resource), resource);
        }
        return resourcesById;
    }
}
