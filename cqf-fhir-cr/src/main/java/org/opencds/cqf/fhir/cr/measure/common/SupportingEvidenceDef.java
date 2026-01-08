package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * FHIR agnostic Object to store Measure Population supporting evidence definition and expression results
 */
public class SupportingEvidenceDef {
    private final String expression;
    private final String systemUrl;
    private final String name;
    @Nullable
    private final String expressionDescription;
    @Nullable
    private final String expressionLanguage;
    @Nullable
    private final ConceptDef code;
    protected Map<String, Set<Object>> subjectResources = new HashMap<>();

    public SupportingEvidenceDef(
        String expression,
        String systemUrl,
        @Nullable String expressionDescription,
        String name,
        @Nullable String expressionLanguage,
        @Nullable ConceptDef code) {
        this.expression = expression;
        this.systemUrl = systemUrl;
        this.expressionDescription = expressionDescription;
        this.name = name;
        this.expressionLanguage = expressionLanguage;
        this.code = code;
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
    @Nullable
    public String getExpressionDescription() {return this.expressionDescription;}
    public String getName() {return this.name;}
    @Nullable
    public String getExpressionLanguage() {return this.expressionLanguage;}

    public Set<Object> getResourcesForSubject(String subjectId) {
        return subjectResources.getOrDefault(subjectId, new HashSetForFhirResourcesAndCqlTypes<>());
    }
    @Nullable
    public ConceptDef getCode() {return this.code;}

    // Add an element to Set<Object> under a key (Creates a new set if key is missing)
    public void addResource(String key, Object value) {
        subjectResources
                .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
                .add(value);
    }
}
