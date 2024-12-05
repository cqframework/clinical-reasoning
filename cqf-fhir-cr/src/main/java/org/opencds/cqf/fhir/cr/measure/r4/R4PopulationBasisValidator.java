package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.*;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumeration;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cr.measure.common.*;

/**
 * Validates group populations and stratifiers against population basis-es for R4 only.
 */
public class R4PopulationBasisValidator implements PopulationBasisValidator {

    private static final String BOOLEAN_BASIS = "boolean";

    /**
     * For any given stratifier expression, we don't know if it was evaluated in the patient context or it's a valid
     * expression, so we'll apply this heuristic for now
     */
    private static final Set<Class<?>> ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES = new HashSet<>(Arrays.asList(
            CodeableConcept.class,
            Quantity.class,
            Range.class,
            Reference.class,
            Coding.class,
            Enumeration.class,
            Boolean.class,
            // CQL type returned by some stratifier expression that don't map neatly to FHIR types
            Code.class));

    @Override
    public void validateGroupPopulations(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
        groupDef.populations().forEach(population -> {
            validateGroupPopulationBasisType(measureDef.url(), groupDef, population, evaluationResult);
        });
    }

    @Override
    public void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
        groupDef.stratifiers().forEach(stratifier -> {
            validateStratifierPopulationBasisType(measureDef.url(), groupDef, stratifier, evaluationResult);
        });
    }

    private void validateGroupPopulationBasisType(
            String url, GroupDef groupDef, PopulationDef populationDef, EvaluationResult evaluationResult) {

        var scoring = groupDef.measureScoring();
        var populationExpression = populationDef.expression();
        var expressionResult = evaluationResult.forExpression(populationDef.expression());

        if (expressionResult == null || expressionResult.value() == null) {
            return;
        }

        var resultClasses = extractClassesFromSingleOrListResult(expressionResult.value());
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();
        var optResourceClass = extractResourceType(groupPopulationBasisCode);

        if (optResourceClass.isPresent()) {

            var resultMatchingClassCount = resultClasses.stream()
                    .filter(it -> optResourceClass.get().isAssignableFrom(it))
                    .count();

            if (resultMatchingClassCount != resultClasses.size()) {
                throw new InvalidRequestException(String.format(
                        "group expression criteria results for expression: [%s] and scoring: [%s] must fall within accepted types for population basis: [%s] for Measure: %s",
                        populationExpression, scoring, groupPopulationBasisCode, url));
            }
        }
    }

    private void validateStratifierPopulationBasisType(
            String url, GroupDef groupDef, StratifierDef stratifierDef, EvaluationResult evaluationResult) {

        if (!stratifierDef.components().isEmpty()) {
            throw new UnsupportedOperationException("multi-component stratifiers are not yet supported.");
        }

        var stratifierExpression = stratifierDef.expression();

        var expressionResult = evaluationResult.forExpression(stratifierExpression);

        if (expressionResult == null || expressionResult.value() == null) {
            return;
        }

        var resultClasses = extractClassesFromSingleOrListResult(expressionResult.value());
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            var resultMatchingClassCount = resultClasses.stream()
                    .filter(resultClass -> ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES.contains(resultClass)
                            || Boolean.class == resultClass)
                    .count();

            if (resultMatchingClassCount != resultClasses.size()) {
                throw new InvalidRequestException(String.format(
                        "stratifier expression criteria results for expression: [%s] must fall within accepted types for boolean population basis: [%s] for Measure: %s",
                        stratifierExpression, groupPopulationBasisCode, url));
            }

            return;
        }

        var optResourceClass = extractResourceType(groupPopulationBasisCode);

        if (optResourceClass.isPresent()) {

            var resultMatchingClassCount = resultClasses.stream()
                    .filter(it -> optResourceClass.get().isAssignableFrom(it))
                    .count();

            if (resultMatchingClassCount != resultClasses.size()) {
                throw new InvalidRequestException(String.format(
                        "stratifier expression criteria results for expression: [%s] must fall within accepted types for population basis: [%s] for Measure: %s",
                        stratifierExpression, groupPopulationBasisCode, url));
            }
        }
    }

    private Optional<? extends Class<?>> extractResourceType(String groupPopulationBasisCode) {
        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return Optional.of(Boolean.class);
        }
        return Arrays.stream(ResourceType.values())
                .map(ResourceType::name)
                .filter(theName -> {
                    if ("ListResource".equals(groupPopulationBasisCode)) {
                        return true;
                    }

                    return theName.equals(groupPopulationBasisCode);
                })
                .map(typeName -> "org.hl7.fhir.r4.model." + typeName)
                .map(fullyQualified -> {
                    try {
                        return Class.forName(fullyQualified);
                    } catch (Exception exception) {
                        throw new IllegalArgumentException(exception);
                    }
                })
                .findFirst();
    }

    private List<Class<?>> extractClassesFromSingleOrListResult(Object result) {
        if (result == null) {
            return Collections.emptyList();
        }

        if (!(result instanceof List<?>)) {
            return Collections.singletonList(result.getClass());
        }

        var list = (List<?>) result;

        return list.stream().filter(Objects::nonNull).map(Object::getClass).collect(Collectors.toList());
    }
}
