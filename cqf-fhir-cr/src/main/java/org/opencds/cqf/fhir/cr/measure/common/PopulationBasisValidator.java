package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates group populations and stratifiers against population basis-es.
 * <p/>
 * Default implementations provide the full validation logic used by R4 and later FHIR versions.
 * FHIR-version-specific behavior is supplied by overriding {@link #allowedStratifierValueTypes()}
 * and {@link #extractResourceType(String)}. Versions that do not support population basis
 * (e.g. DSTU3) can override the public methods to no-op.
 */
public interface PopulationBasisValidator {

    Logger log = LoggerFactory.getLogger(PopulationBasisValidator.class);

    String BOOLEAN_BASIS = "boolean";
    String STRING_BASIS = "string";

    /**
     * Returns the set of Java classes that are allowed as value stratifier result types.
     * Typically includes FHIR types like CodeableConcept, Coding, Quantity, etc., plus
     * Java/CQL primitives like Boolean, Integer, String, and Code.
     * <p/>
     * Default returns an empty set. Implementations that enable validation must override this.
     */
    default Set<String> allowedStratifierValueTypes() {
        return Set.of();
    }

    /**
     * Maps a population basis code (e.g. "Encounter", "boolean") to the corresponding Java class
     * for group population validation. Handles primitive FHIR type codes ("boolean") that are
     * common across all FHIR versions, then delegates to {@link #extractFhirResourceType(String)}
     * for version-specific resource type resolution. Returns {@link Optional#empty()} if the code
     * does not map to a known type (e.g. for primitive types like "string").
     */
    default Optional<String> extractResourceType(String groupPopulationBasisCode) {
        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return Optional.of(org.opencds.cqf.cql.engine.runtime.Boolean.class.getName());
        }
        return extractFhirResourceType(groupPopulationBasisCode);
    }

    /**
     * Maps a population basis code to a FHIR-version-specific resource class
     * (e.g. "Encounter" to {@code org.hl7.fhir.r4.model.Encounter}).
     * <p/>
     * Default returns empty. Implementations must override this to enable resource type validation.
     */
    default Optional<String> extractFhirResourceType(String groupPopulationBasisCode) {
        return Optional.empty();
    }

    /**
     * Resolves a population basis code to a Java class by matching against the provided resource
     * type names and loading the class from the given FHIR model package.
     * <p/>
     * This is the shared implementation that version-specific validators call from their
     * {@link #extractFhirResourceType(String)} override, supplying the appropriate
     * resource type names and model package for their FHIR version.
     *
     * @param groupPopulationBasisCode the population basis code from the Measure (e.g. "Encounter")
     * @param resourceTypeNames the valid resource type names for the FHIR version (e.g. from ResourceType enum)
     * @param fhirModelPackage the FHIR model package prefix (e.g. "org.hl7.fhir.r4.model.")
     */
    default Optional<String> resolveResourceType(
            String groupPopulationBasisCode, Collection<String> resourceTypeNames, String fhirModelPackage) {

        return resourceTypeNames.stream()
                .filter(theName -> theName.equals(groupPopulationBasisCode))
                .map(typeName -> fhirModelPackage + typeName)
                .findFirst();
    }

    default void validateGroupPopulations(
            MeasureDef measureDef, GroupDef groupDef, CqlEvaluationResult evaluationResult) {
        groupDef.populations()
                .forEach(population ->
                        validateGroupPopulationBasisType(measureDef.url(), groupDef, population, evaluationResult));
    }

    default void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, CqlEvaluationResult evaluationResult) {
        groupDef.stratifiers()
                .forEach(stratifier -> validateStratifierPopulationBasisType(
                        measureDef.url(), groupDef, stratifier, evaluationResult));
    }

    private void validateGroupPopulationBasisType(
            String url, GroupDef groupDef, PopulationDef populationDef, CqlEvaluationResult evaluationResult) {

        var scoring = groupDef.measureScoring();
        var populationExpression = populationDef.expression();
        if (populationExpression == null || populationExpression.isBlank()) {
            return;
        }

        var wrapper = evaluationResult.get(populationExpression);

        if (wrapper == null || wrapper.isNull()) {
            return;
        }

        var resultClasses = StratifierUtils.extractClassesFromSingleOrListResult(wrapper);
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();
        var optResourceClass = extractResourceType(groupPopulationBasisCode);

        if (optResourceClass.isPresent()) {

            var resultMatchingClasses = resultClasses.stream()
                    .filter(it -> doClassesMatch(it, optResourceClass.orElse(null)))
                    .toList();

            if (resultMatchingClasses.size() != resultClasses.size()) {
                throw new InvalidRequestException(
                        "group expression criteria results for expression: [%s] and scoring: [%s] must fall within accepted types for population basis: [%s] for Measure: [%s] due to mismatch between total result classes: %s and matching result classes: %s"
                                .formatted(
                                        populationExpression,
                                        scoring,
                                        groupPopulationBasisCode,
                                        url,
                                        simpleNamesFromTypes(resultClasses),
                                        simpleNamesFromTypes(resultMatchingClasses)));
            }
        }
    }

    private List<String> simpleNamesFromTypes(List<String> classNames) {
        return classNames.stream()
                .map(PopulationBasisValidator::simpleNameFromType)
                .distinct()
                .toList();
    }

    private static boolean doClassesMatch(String it, @Nullable String resourceClassName) {
        return Objects.equals(resourceClassName, it);
    }

    private void validateStratifierPopulationBasisType(
            String url, GroupDef groupDef, StratifierDef stratifierDef, CqlEvaluationResult evaluationResult) {

        if (stratifierDef.isCriteriaStratifier()) {
            validateExpressionResultType(groupDef, stratifierDef, stratifierDef.expression(), evaluationResult, url);
        } else {
            for (var component : stratifierDef.components()) {
                validateExpressionResultType(groupDef, stratifierDef, component.expression(), evaluationResult, url);
            }
        }
    }

    private void validateExpressionResultType(
            GroupDef groupDef,
            StratifierDef stratifierDef,
            String expression,
            CqlEvaluationResult evaluationResult,
            String url) {
        if (StringUtils.isBlank(expression)) {
            return;
        }

        var wrapper = evaluationResult.get(expression);

        if (wrapper == null || wrapper.isNull()) {
            return;
        }

        var resultClasses = StratifierUtils.extractClassesFromSingleOrListResult(wrapper);
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        if (stratifierDef.isCriteriaStratifier()) {
            if (resultClasses.isEmpty()) {
                log.warn("Criteria-based stratifier results are empty for measure: {}", url);
                return;
            }

            if (resultClasses.stream()
                    .noneMatch(resultClass -> doesBasisMatchResource(resultClass, groupPopulationBasisCode))) {

                throw new InvalidRequestException(
                        "criteria-based stratifier is invalid for expression: [%s] due to mismatch between population basis: [%s] and result types: %s for measure URL: %s"
                                .formatted(
                                        expression,
                                        groupPopulationBasisCode,
                                        simpleNamesFromTypes(resultClasses),
                                        url));
            }

            // skip validation below since for criteria-based stratifier, the boolean basis test is irrelevant
            return;
        }

        var resultMatchingClasses =
                resultClasses.stream().filter(this::isResultClassAllowed).toList();

        if (resultMatchingClasses.size() != resultClasses.size()) {
            var invalidTypes = resultClasses.stream()
                    .filter(typeName -> !resultMatchingClasses.contains(typeName))
                    .map(PopulationBasisValidator::simpleNameFromType)
                    .distinct()
                    .toList();
            throw new InvalidStratifierExpressionTypeException(buildValueStratifierErrorMessage(
                    stratifierDef, expression, groupPopulationBasisCode, url, invalidTypes));
        }
    }

    /**
     * Ensure the type returned in the error is a simple name (ex: Encounter, not org.fhir...Encounter)
     */
    private static String simpleNameFromType(String invalidType) {
        final String[] split = invalidType.split("\\.");

        if (split.length == 1) {
            return invalidType;
        }

        return split[split.length - 1];
    }

    private String buildValueStratifierErrorMessage(
            StratifierDef stratifierDef,
            String expression,
            String groupPopulationBasisCode,
            String url,
            List<String> invalidTypes) {

        // var distinctInvalidTypes = prettyDistinctClassNames(invalidTypes);

        if (stratifierDef.getStratifierType() == MeasureStratifierType.NON_SUBJECT_VALUE) {
            return "non-subject value stratifier is invalid for expression: [%s] with result types: %s for population basis: [%s] for measure URL: %s. Expected a scalar or scalar-returning function"
                    .formatted(expression, invalidTypes, groupPopulationBasisCode, url);
        }

        return "value stratifier is invalid for expression: [%s] with result types: %s for measure URL: %s. Expected a scalar type"
                .formatted(expression, invalidTypes, url);
    }

    private boolean isResultClassAllowed(String resultClass) {
        return allowedStratifierValueTypes().contains(resultClass)
                || org.opencds.cqf.cql.engine.runtime.Boolean.class.getName().equals(resultClass);
    }

    private boolean doesBasisMatchResource(String resultClass, String groupPopulationBasisCode) {
        // FHIR primitive type codes are lowercase ("boolean", "string") but Java class simple names
        // are uppercase ("Boolean", "String"). Handle these explicitly to match only the valid
        // lowercase FHIR codes and reject invalid uppercase variants like "String" or "Boolean".
        if (Boolean.class.getName().equals(resultClass)
                || org.opencds.cqf.cql.engine.runtime.Boolean.class.getName().equals(resultClass)) {
            return BOOLEAN_BASIS.equals(groupPopulationBasisCode);
        }
        if (String.class.getName().equals(resultClass)
                || org.opencds.cqf.cql.engine.runtime.String.class.getName().equals(resultClass)) {
            return STRING_BASIS.equals(groupPopulationBasisCode);
        }
        var classSplit = resultClass.split("\\.");
        var simpleName = classSplit[classSplit.length - 1];
        return groupPopulationBasisCode.equals(simpleName);
    }

    private List<String> prettyClassNames(List<Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName).toList();
    }

    private List<String> prettyDistinctClassNames(List<Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName).distinct().toList();
    }
}
