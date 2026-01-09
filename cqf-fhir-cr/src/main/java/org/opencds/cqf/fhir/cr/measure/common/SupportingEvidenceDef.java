package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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

    /**
     * Supporting Evidence Def used to store fhir agnostic supporting evidence definition and results
     * @param expression CQL expression name found in the library where the expression resides.
     * @param systemUrl the url that will be used on MeasureReport to signify results as 'Supporting Evidence'
     * @param expressionDescription the text describing any context as to how the result is supporting evidence
     * @param name the unique identifier for the supporting evidence component
     * @param expressionLanguage language tag for expression ex: 'text/cql-identifier'
     * @param code CodeableConcept for expression definition
     */
    public SupportingEvidenceDef(
            String expression,
            String systemUrl,
            @Nullable String expressionDescription,
            String name,
            @Nullable String expressionLanguage,
            @Nullable ConceptDef code) {
        // must be populated
        this.expression = requireNonBlank(expression, "expression");
        this.systemUrl = requireNonBlank(systemUrl, "systemUrl");
        this.name = requireNonBlank(name, "name");
        // optionally populated
        this.expressionDescription = expressionDescription;
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
    public String getExpressionDescription() {
        return this.expressionDescription;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public String getExpressionLanguage() {
        return this.expressionLanguage;
    }

    public Set<Object> getResourcesForSubject(String subjectId) {
        return subjectResources.getOrDefault(subjectId, new HashSetForFhirResourcesAndCqlTypes<>());
    }

    @Nullable
    public ConceptDef getCode() {
        return this.code;
    }

    // Add an element to Set<Object> under a key (Creates a new set if key is missing)
    public void addResource(String key, Object value) {
        subjectResources
                .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
                .add(value);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null) {
            throw new InvalidRequestException(fieldName + " must not be null");
        }
        if (value.isBlank()) {
            throw new InvalidRequestException(fieldName + " must not be blank");
        }
        return value;
    }
}
