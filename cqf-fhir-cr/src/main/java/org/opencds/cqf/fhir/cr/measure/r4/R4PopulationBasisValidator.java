package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.*;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cr.measure.common.*;

// LUKETODO: javadoc
public class R4PopulationBasisValidator implements PopulationBasisValidator {

    private static final String BOOLEAN_BASIS = "boolean";

    // LUKETODO:  we're failing because "Code" doesn't match "CodeableConcept" so investigate
    // LUKETODO:  we also get a FHIR Enumeration here (ex: female)
    private static final Set<Class<?>> ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES = Set.of(
            CodeableConcept.class,
            Quantity.class,
            Range.class,
            Reference.class,
            Boolean.class,
            // CQL type returned by some stratifier expression that don't map neatly to FHIR types
            Code.class);

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

    // LUKETODO:  pass actual measure later?
    private void validateGroupPopulationBasisType(
            String url, GroupDef groupDef, PopulationDef populationDef, EvaluationResult evaluationResult) {
        printDetails(url, groupDef, populationDef, evaluationResult);

        var scoring = groupDef.measureScoring();

        // Depending on the scoring type, we'll only evaluate some of the expressions, so we don't evaluate all of them?
        var expressionsToEvaluate =
                switch (scoring) {
                    case PROPORTION, RATIO -> Set.of(
                            INITIALPOPULATION,
                            MEASUREPOPULATION,
                            MEASUREOBSERVATION,
                            MEASUREPOPULATIONEXCLUSION,
                            NUMERATOR,
                            DENOMINATOR);
                    case CONTINUOUSVARIABLE -> Set.of(INITIALPOPULATION);
                    case COHORT -> Set.of(
                            INITIALPOPULATION, MEASUREPOPULATION, MEASUREOBSERVATION, MEASUREPOPULATIONEXCLUSION);
                };

        var populationExpression = populationDef.expression();

        var expressionCodesWhitelist = expressionsToEvaluate.stream()
                .map(MeasurePopulationType::getDisplay)
                .toList();

        if (!expressionCodesWhitelist.contains(populationExpression)) {
            System.out.printf(
                    "POPULATION EXPRESSION: [%s] WILL NOT BE EVALUATED: for scoring [%s]\n",
                    populationExpression, scoring);
            return;
        }

        System.out.printf(
                "POPULATION EXPRESSION: [%s] WILL BE EVALUATED: for scoring [%s]\n", populationExpression, scoring);

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
                        "group expression criteria results for expression: [%s] and scoring: [%s] must match the same type: [%s] as population basis: [%s] for Measure: %s",
                        populationExpression,
                        scoring,
                        distinctClassSimpleNames(resultClasses),
                        groupPopulationBasisCode,
                        url));
            }
        }

        // LUKETODO:  what if it's empty?
    }

    private void validateStratifierPopulationBasisType(
            String url, GroupDef groupDef, StratifierDef stratifierDef, EvaluationResult evaluationResult) {

        printDetails(url, groupDef, stratifierDef, evaluationResult);

        if (!stratifierDef.components().isEmpty()) {
            throw new UnsupportedOperationException("multi-component stratifiers are not yet supported.");
        }

        var stratifierExpression = stratifierDef.expression();

        var expressionResult = evaluationResult.forExpression(stratifierExpression);

        var resultClasses = extractClassesFromSingleOrListResult(expressionResult.value());
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            var resultMatchingClassCount = resultClasses.stream()
                    .filter(resultClass -> ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES.contains(resultClass)
                            || Boolean.class == resultClass)
                    .count();

            if (resultMatchingClassCount != resultClasses.size()) {
                throw new InvalidRequestException(String.format(
                        "stratifier expression criteria results for expression: [%s] must fall within accepted types %s for boolean population basis: [%s] for Measure: %s",
                        stratifierExpression, distinctClassSimpleNames(resultClasses), groupPopulationBasisCode, url));
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
                        "stratifier expression criteria results for expression: [%s] must match the same type: %s as population basis: [%s] for Measure: %s",
                        stratifierExpression, distinctClassSimpleNames(resultClasses), groupPopulationBasisCode, url));
            }
        }

        // LUKETODO:  what if it's empty?
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

    // LUKETODO:  get rid of this at the last minute:
    private void printDetails(
            String url, GroupDef groupDef, PopulationDef populationDef, EvaluationResult evaluationResult) {
        var result = Optional.ofNullable(evaluationResult.forExpression(populationDef.expression()))
                .map(ExpressionResult::value)
                .orElse(null);
        final String resultClass;
        if (result != null) {
            if (result instanceof List<?> list) {
                if (!list.isEmpty()) {
                    //                    resultClass = "List: " + list.get(0).getClass().getSimpleName();
                    resultClass = "List: " + list.get(0).getClass().getName();
                } else {
                    resultClass = "List: " + null;
                }
            } else {
                //                resultClass = "Single: " + result.getClass().getSimpleName();
                resultClass = "Single: " + result.getClass().getName();
            }
        } else {
            resultClass = "Single: " + null;
        }

        System.out.printf(
                "POPULATION: %s: expression: [%s], populationBasis: [%s], result class: %s\n",
                url, populationDef.expression(), groupDef.getPopulationBasis().code(), resultClass);
    }

    // LUKETODO:  get rid of this at the last minute:
    private void printDetails(
            String url, GroupDef groupDef, StratifierDef stratifierDef, EvaluationResult evaluationResult) {
        var result = Optional.ofNullable(evaluationResult.forExpression(stratifierDef.expression()))
                .map(ExpressionResult::value)
                .orElse(null);
        final String resultClass;
        if (result != null) {
            if (result instanceof List<?> list) {
                if (!list.isEmpty()) {
                    //                    resultClass = "List: " + list.get(0).getClass().getSimpleName();
                    resultClass = "List: " + list.get(0).getClass().getName();
                } else {
                    resultClass = "List: " + null;
                }
            } else {
                //                resultClass = "Single: " + result.getClass().getSimpleName();
                resultClass = "Single: " + result.getClass().getName();
            }
        } else {
            resultClass = "Single: " + null;
        }

        System.out.printf(
                "STRATIFIER: %s: expression: [%s], populationBasis: [%s], result class: %s\n",
                url, stratifierDef.expression(), groupDef.getPopulationBasis().code(), resultClass);
    }

    @Nonnull
    private Set<String> distinctClassSimpleNames(List<Class<?>> theResultClasses) {
        return theResultClasses.stream()
                //                .map(Class::getSimpleName)
                .map(Class::getName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
