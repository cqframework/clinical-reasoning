package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SupportingEvidenceDef {
    private final String expression;
    private final String systemUrl;
    protected Map<String, Set<Object>> subjectResources = new HashMap<>();

    public SupportingEvidenceDef(String expression, String systemUrl) {
        this.expression = expression;
        this.systemUrl = systemUrl;
    }

    // Getter method
    public String getSystemUrl() {
        return this.systemUrl;
    }

    public String getExpression() {
        return this.expression;
    }

    public Map<String, Set<Object>> getSubjectResources() {
        return subjectResources;
    }

    public Set<Object> getResourcesForSubject(String subjectId) {
        return subjectResources.getOrDefault(subjectId, new HashSetForFhirResourcesAndCqlTypes<>());
    }

    // Add an element to Set<Object> under a key (Creates a new set if key is missing)
    public void addResource(String key, Object value) {
        subjectResources
                .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
                .add(value);
    }
}
