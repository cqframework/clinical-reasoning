package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cql.CqlClassInstanceHelper.convertToFhirR4IfNeeded;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;

/**
 * Validates group populations and stratifiers against population basis-es for R4.
 * Provides R4-specific FHIR type mappings; all validation logic lives in
 * {@link PopulationBasisValidator} default methods.
 */
public class R4PopulationBasisValidator implements PopulationBasisValidator {

    private static final Set<Class<?>> ALLOWED_STRATIFIER_VALUE_TYPES = new HashSet<>(Arrays.asList(
            CodeableConcept.class,
            Quantity.class,
            Range.class,
            Reference.class,
            Coding.class,
            Enumeration.class,
            Boolean.class,
            // added Integer and String for examples like age or gender
            Integer.class,
            String.class,
            // CQL type returned by some stratifier expression that don't map neatly to FHIR types
            Code.class));

    private static final String FHIR_MODEL_PACKAGE = "org.hl7.fhir.r4.model.";

    private static final List<String> RESOURCE_TYPE_NAMES =
            Arrays.stream(ResourceType.values()).map(ResourceType::name).toList();

    @Override
    public Set<Class<?>> allowedStratifierValueTypes() {
        return ALLOWED_STRATIFIER_VALUE_TYPES;
    }
    
    public void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
        groupDef.stratifiers()
                .forEach(stratifier -> validateStratifierPopulationBasisType(
                        measureDef.url(), groupDef, stratifier, evaluationResult));
    }

    private void validateGroupPopulationBasisType(
            String url, GroupDef groupDef, PopulationDef populationDef, EvaluationResult evaluationResult) {

        // PROPORTION
        var scoring = groupDef.measureScoring();
        // Numerator
        var populationExpression = populationDef.expression();
        if (populationExpression == null || populationExpression.isBlank()) {
            return;
        }

        var cqlExpressionResult = evaluationResult.get(populationExpression);
        if (cqlExpressionResult == null || cqlExpressionResult.getValue() == null) {
            return;
        }
        var expressionResult = convertExpressionResult(cqlExpressionResult);

        var resultClasses = StratifierUtils.extractClassesFromSingleOrListResult(expressionResult);
        // Encounter
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();
        var optResourceClass = extractResourceType(groupPopulationBasisCode);

        if (optResourceClass.isPresent()) {

            var resultMatchingClasses = resultClasses.stream()
                    .filter(it -> optResourceClass.get().isAssignableFrom(it))
                    .toList();

            if (resultMatchingClasses.size() != resultClasses.size()) {
                throw new InvalidRequestException(
                        "group expression criteria results for expression: [%s] and scoring: [%s] must fall within accepted types for population basis: [%s] for Measure: [%s] due to mismatch between total result classes: %s and matching result classes: %s"
                                .formatted(
                                        populationExpression,
                                        scoring,
                                        groupPopulationBasisCode,
                                        url,
                                        prettyClassNames(resultClasses),
                                        prettyClassNames(resultMatchingClasses)));
            }
        }
    }

    @Override
    public Optional<Class<?>> extractFhirResourceType(String groupPopulationBasisCode) {
        return resolveResourceType(groupPopulationBasisCode, RESOURCE_TYPE_NAMES, FHIR_MODEL_PACKAGE);
    }
    
    private void validateStratifierPopulationBasisType(
            String url, GroupDef groupDef, StratifierDef stratifierDef, EvaluationResult evaluationResult) {

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
            EvaluationResult evaluationResult,
            String url) {

        var cqlExpressionResult = evaluationResult.get(expression);
        if (cqlExpressionResult == null || cqlExpressionResult.getValue() == null) {
            return;
        }
        var expressionResult = convertExpressionResult(cqlExpressionResult);

        var resultClasses = StratifierUtils.extractClassesFromSingleOrListResult(expressionResult);
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
                                .formatted(expression, groupPopulationBasisCode, prettyClassNames(resultClasses), url));
            }

            // skip validation below since for criteria-based stratifier, the boolean basis test is irrelevant
            return;
        }

        var resultMatchingClasses = resultClasses.stream()
                .filter(resultClass ->
                        ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES.contains(resultClass) || Boolean.class == resultClass)
                .toList();

        if (resultMatchingClasses.size() != resultClasses.size()) {
            throw new InvalidRequestException(
                    "stratifier expression criteria results for expression: [%s] must fall within accepted types for population-basis: [%s] for Measure: [%s] due to mismatch between total eval result classes: %s and matching result classes: %s"
                            .formatted(
                                    expression,
                                    groupPopulationBasisCode,
                                    url,
                                    prettyClassNames(resultClasses),
                                    prettyClassNames(resultMatchingClasses)));
        }
    }

    @SuppressWarnings("unchecked")
    private static Object convertExpressionResult(ExpressionResult cqlExpressionResult) {
        Object expressionResult;
        if (cqlExpressionResult.getValue() instanceof Iterable<?> iterable) {
            expressionResult = new ArrayList<>();
            for (var result : iterable) {
                ((List<Object>) expressionResult).add(convertToFhirR4IfNeeded(result));
            }
        } else {
            expressionResult = convertToFhirR4IfNeeded(cqlExpressionResult.getValue());
        }
        return expressionResult;
    }

    private boolean doesBasisMatchResource(Class<?> resultClass, String groupPopulationBasisCode) {
        // If we don't do this we'll fail with "boolean" vs. "Boolean"
        if (resultClass == Boolean.class && BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return true;
        }

        return resultClass.getSimpleName().equals(groupPopulationBasisCode);
    }

    private Optional<Class<?>> extractResourceType(String groupPopulationBasisCode) {
        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return Optional.of(Boolean.class);
        }

        final Optional<String> optResourceClassName = Arrays.stream(ResourceType.values())
                .map(ResourceType::name)
                .filter(theName -> {
                    if ("ListResource".equals(groupPopulationBasisCode)) {
                        return true;
                    }

                    return theName.equals(groupPopulationBasisCode);
                })
                .map(typeName -> "org.hl7.fhir.r4.model." + typeName)
                .findFirst();

        if (optResourceClassName.isPresent()) {
            try {
                return Optional.of(Class.forName(optResourceClassName.get()));
            } catch (ClassNotFoundException exception) {
                throw new InternalErrorException(exception);
            }
        }
        return Optional.empty();
    }

    private List<String> prettyClassNames(List<Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName).toList();
    }
}
