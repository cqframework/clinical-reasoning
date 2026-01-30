package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import kotlin.Unit;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.OperandDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationExpressionRef;
import org.opencds.cqf.cql.engine.execution.EvaluationFunctionRef;
import org.opencds.cqf.cql.engine.execution.EvaluationParams.Builder;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.EvaluationResults;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Capture all logic for measure evaluation for continuous variable scoring.
 */
public class FunctionEvaluationHandler {
    private static final Logger logger = LoggerFactory.getLogger(FunctionEvaluationHandler.class);
    public static final String DOES_NOT_HAVE_FUNCTION = "does not have function";

    private FunctionEvaluationHandler() {
        // static class with private constructor
    }

    // Unfortunately, we need this try/catch ceremony until we figure out a better way to check that
    // a CQL statement refers to an expression and NOT a function.  Calling the CQL evaluate API
    // to catch an Exception as a positive verification is the wrong approach.
    // See the validateNotFunction method of this class.
    static List<EvaluationResult> cqlFunctionEvaluation(
            CqlEngine context,
            List<MeasureDef> measureDefs,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart) {

        final boolean hasLibraryInitialized = LibraryInitHandler.initLibrary(context, libraryIdentifier);

        try {
            return tryCqlFunctionEvaluation(context, measureDefs, libraryIdentifier, evaluationResult, subjectTypePart);
        } finally {
            // We don't want to pop a non-existent library
            if (hasLibraryInitialized) {
                LibraryInitHandler.popLibrary(context);
            }
        }
    }

    @Nonnull
    private static List<EvaluationResult> tryCqlFunctionEvaluation(
            CqlEngine context,
            List<MeasureDef> measureDefs,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart) {
        // Validate all stratifier expression types before processing
        for (MeasureDef measureDef : measureDefs) {
            for (GroupDef groupDef : measureDef.groups()) {
                validateStratifierExpressionTypes(context, measureDef.url(), groupDef);
            }
        }

        // MeasureDefs where functions need to evaluate
        final List<MeasureDef> measureDefsWithFunctions = measureDefs.stream()
                // if measure contains measure-observation, otherwise short circuit
                .filter(x -> hasMeasureObservation(x) || hasNonSubValueStratifier(x))
                .toList();

        if (measureDefsWithFunctions.isEmpty()) {
            // Don't need to do anything if there are no functions to process
            return List.of();
        }

        final List<EvaluationResult> finalResults = new ArrayList<>();

        // one Library may be linked to multiple Measures
        for (MeasureDef measureDefWithFunctions : measureDefsWithFunctions) {

            // get function for measure-observation from populationDef
            for (GroupDef groupDef : measureDefWithFunctions.groups()) {
                finalResults.addAll(evaluateMeasureObservations(
                        context,
                        libraryIdentifier,
                        evaluationResult,
                        subjectTypePart,
                        groupDef,
                        measureDefWithFunctions.url()));
                finalResults.addAll(evaluateNonSubjectValueStratifiers(
                        context,
                        libraryIdentifier,
                        evaluationResult,
                        subjectTypePart,
                        groupDef,
                        measureDefWithFunctions.url()));
            }
        }

        return finalResults;
    }

    private static List<EvaluationResult> evaluateMeasureObservations(
            CqlEngine context,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart,
            GroupDef groupDef,
            String measureUrl) {

        // measure observations to evaluate
        final List<PopulationDef> measureObservationPopulations = groupDef.populations().stream()
                .filter(populationDef -> MeasurePopulationType.MEASUREOBSERVATION.equals(populationDef.type()))
                .toList();

        if (measureObservationPopulations.isEmpty()) {
            return List.of();
        }

        final List<EvaluationResult> results = new ArrayList<>();

        for (PopulationDef populationDef : measureObservationPopulations) {
            // each measureObservation is evaluated
            var result = processMeasureObservation(
                    context, libraryIdentifier, evaluationResult, subjectTypePart, groupDef, populationDef, measureUrl);
            results.add(result);
        }

        return results;
    }

    private static List<EvaluationResult> evaluateNonSubjectValueStratifiers(
            CqlEngine context,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart,
            GroupDef groupDef,
            String measureUrl) {

        // get function for non-subject value stratifiers and evaluate for each populationDef
        final List<StratifierDef> stratifierDefs = groupDef.stratifiers().stream()
                .filter(StratifierDef::isNonSubjectValueStratifier)
                .toList();

        if (stratifierDefs.isEmpty()) {
            return List.of();
        }

        final List<EvaluationResult> results = new ArrayList<>();

        for (StratifierDef stratDef : stratifierDefs) {
            // each stratifier (could be multiple defined in component)
            var result = processNonSubValueStratifiers(
                    context, libraryIdentifier, evaluationResult, subjectTypePart, groupDef, stratDef, measureUrl);
            results.add(result);
        }

        return results;
    }

    /**
     * Validates that stratifier expressions use the correct expression type based on stratifier type:
     * <ul>
     *   <li>CRITERIA stratifier: must NOT be a CQL function, result must match population-basis</li>
     *   <li>VALUE stratifier (boolean basis): must NOT be a CQL function</li>
     *   <li>NON_SUBJECT_VALUE stratifier: must BE a CQL function with input matching population-basis</li>
     * </ul>
     *
     * @param context the CQL engine context
     * @param measureUrl the measure URL for error messages
     * @param groupDef the group definition containing stratifiers
     */
    private static void validateStratifierExpressionTypes(CqlEngine context, String measureUrl, GroupDef groupDef) {
        for (StratifierDef stratifierDef : groupDef.stratifiers()) {
            if (stratifierDef.isCriteriaStratifier()) {
                // CRITERIA stratifier: must NOT be a function
                validateNotFunction(context, measureUrl, stratifierDef.expression(), "CRITERIA");
            } else if (!stratifierDef.isNonSubjectValueStratifier()) {
                // VALUE stratifier (boolean basis): must NOT be a function
                if (stratifierDef.isComponentStratifier()) {
                    for (var component : stratifierDef.components()) {
                        validateNotFunction(context, measureUrl, component.expression(), "VALUE (subject-based)");
                    }
                } else {
                    validateNotFunction(context, measureUrl, stratifierDef.expression(), "VALUE (subject-based)");
                }
            }
            // NON_SUBJECT_VALUE stratifier validation happens in processNonSubValueStratifiers
        }
    }

    /**
     * Validates that an expression is NOT a CQL function definition.
     *
     * @param context the CQL engine context
     * @param measureUrl the measure URL for error messages
     * @param expression the expression name to check
     * @param stratifierType the type of stratifier for error messages
     */
    private static void validateNotFunction(
            CqlEngine context, String measureUrl, String expression, String stratifierType) {
        if (expression == null || expression.isBlank()) {
            return;
        }

        var ed = Libraries.resolveExpressionRef(
                expression, Objects.requireNonNull(context.getState().getCurrentLibrary()));
        if (ed instanceof FunctionDef) {
            throw new InvalidRequestException(
                    ("%s stratifier expression '%s' must NOT be a CQL function definition for measure: %s. "
                                    + "Only NON_SUBJECT_VALUE stratifiers (non-boolean population basis with component criteria) "
                                    + "should use CQL function definitions.")
                            .formatted(stratifierType, expression, measureUrl));
        }
    }

    /**
     * For a given measure observation population, do an ad-hoc function evaluation and
     * accumulate the results that will be subsequently added to the CQL evaluation result.
     */
    private static EvaluationResult processMeasureObservation(
            CqlEngine context,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart,
            GroupDef groupDef,
            PopulationDef populationDef,
            String measureUrl) {

        if (populationDef.getCriteriaReference() == null) {
            // We screwed up building the PopulationDef, somehow
            throw new InternalErrorException(
                    "PopulationDef criteria reference is missing for continuous variable observation");
        }

        // get criteria input for results to get (measure-population, numerator, denominator)
        var criteriaPopulationId = populationDef.getCriteriaReference();
        // function that will be evaluated
        var observationExpression = populationDef.expression();
        // get expression from criteriaPopulation reference
        var criteriaExpressionInput = groupDef.populations().stream()
                .filter(populationDefInner -> populationDefInner.id().equals(criteriaPopulationId))
                .map(PopulationDef::expression)
                .findFirst()
                .orElse(null);

        Optional<ExpressionResult> optExpressionResult =
                tryGetExpressionResult(criteriaExpressionInput, evaluationResult);

        if (optExpressionResult.isEmpty()) {
            return new EvaluationResult();
        }

        final ExpressionResult expressionResult = optExpressionResult.get();
        final Iterable<?> resultsIter = getResultIterable(evaluationResult, expressionResult, subjectTypePart);
        // make new expression name for uniquely extracting results
        // this will be used in MeasureEvaluator
        var expressionName = criteriaPopulationId + "-" + observationExpression;

        // VERY IMPORTANT: We need a custom Map to ensure remove by FHIR resource key does not
        // use object identity (AKA ==)
        final Map<Object, Object> functionResults = new HashMapForFhirResourcesAndCqlTypes<>();
        final Set<Object> evaluatedResources = new HashSet<>();

        final String exceptionMessageIfNotFunction =
                """
            Measure: '%s', MeasureObservation population expression '%s' must be a CQL function
            definition, but it is not. For non-boolean population basis, stratifier component
            criteria expressions must be "
            CQL functions that take a parameter matching the population basis type.
            """
                        .formatted(measureUrl, observationExpression);

        for (Object result : resultsIter) {
            final ExpressionResult observationResult = evaluateMeasureObservationFunction(
                    context,
                    libraryIdentifier,
                    observationExpression,
                    groupDef.isBooleanBasis(),
                    getFunctionArguments(groupDef, result),
                    exceptionMessageIfNotFunction);

            var quantity = convertCqlResultToQuantityDef(observationResult.getValue());
            // add function results to existing EvaluationResult under new expression
            // name
            // need a way to capture input parameter here too, otherwise we have no way
            // to connect input objects related to output object
            // key= input parameter to function
            // value= the output Observation resource containing calculated value
            functionResults.put(result, quantity);
            Optional.ofNullable(observationResult.getEvaluatedResources()).ifPresent(evaluatedResources::addAll);
        }

        return buildEvaluationResult(expressionName, functionResults, evaluatedResources);
    }

    /**
     * For a given non-subject based stratifier(s), do an ad-hoc function evaluation against group populations and
     * accumulate the results that will be subsequently added to the CQL evaluation result.
     *
     * <p>Supports two scenarios:
     * <ul>
     *   <li><b>Function expression</b>: CQL function takes each population resource as input, returns a value
     *       per resource. Used for resource-level stratification (e.g., age range per encounter).</li>
     *   <li><b>Scalar expression</b>: CQL expression returns a scalar value per subject (not a function).
     *       Falls back to subject-level stratification - the scalar value applies to all resources for that subject.</li>
     * </ul>
     */
    private static EvaluationResult processNonSubValueStratifiers(
            CqlEngine context,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart,
            GroupDef groupDef,
            StratifierDef stratifierDef,
            String measureUrl) {

        EvaluationResult evalResult = new EvaluationResult();

        for (StratifierComponentDef componentDef : stratifierDef.components()) {
            if (componentDef.expression() == null || componentDef.expression().isEmpty()) {
                // We screwed up defining component correctly
                throw new InternalErrorException("StratifierDef component expression is missing.");
            }
            var stratifierExpression = componentDef.expression();

            final String exceptionMessageIfNotFunction =
                    """
                Measure: '%s', Non-subject value stratifier expression '%s' must be a CQL function definition, but it is not.
                For non-boolean population basis, stratifier component criteria expressions must be "
                CQL functions that take a parameter matching the population basis type.
                """
                            .formatted(measureUrl, stratifierExpression);

            // Function expression: input parameter data for value stratifier functions
            // Exclude MEASUREOBSERVATION populations - they have function expressions that aren't in regular results
            var nonObservationPopulations = groupDef.populations().stream()
                    .filter(pop -> pop.type() != MeasurePopulationType.MEASUREOBSERVATION)
                    .toList();

            for (PopulationDef popDef : nonObservationPopulations) {

                // retrieve group.population results to input into valueStrat function
                Optional<ExpressionResult> optExpressionResult =
                        tryGetExpressionResult(popDef.expression(), evaluationResult);

                if (optExpressionResult.isEmpty()) {
                    throw new InternalErrorException(
                            "Expression result is missing for measure %s".formatted(measureUrl));
                }
                final ExpressionResult expressionResult = optExpressionResult.get();
                final Iterable<?> resultsIter = getResultIterable(evaluationResult, expressionResult, subjectTypePart);
                // make new expression name for uniquely extracting results
                // this will be used in MeasureEvaluator (Criteria population Id and Stratifier Expression)
                var expressionName = popDef.id() + "-" + stratifierExpression;
                final Map<Object, Object> functionResults = new HashMap<>();
                final Set<Object> evaluatedResources = new HashSet<>();

                for (Object result : resultsIter) {
                    final ExpressionResult functionResult = evaluateNonSubValueStratifiersFunction(
                            context,
                            libraryIdentifier,
                            stratifierExpression,
                            getFunctionArguments(groupDef, result),
                            exceptionMessageIfNotFunction);
                    // add function results to existing EvaluationResult under new expression
                    // name
                    // need a way to capture input parameter here too, otherwise we have no way
                    // to connect input objects related to output object
                    // key= input parameter to function
                    // value= the output Observation resource containing calculated value
                    functionResults.put(result, functionResult.getValue());
                    Set<Object> evaluated = functionResult.getEvaluatedResources();
                    if (evaluated == null) {
                        throw new IllegalStateException("CQL function '" + stratifierExpression
                                + "' returned null evaluatedResources for measure: " + measureUrl);
                    }
                    evaluatedResources.addAll(evaluated);
                    evaluatedResources.addAll(functionResult.getEvaluatedResources());
                }
                // add to EvaluationResult
                addToEvaluationResult(evalResult, expressionName, functionResults, evaluatedResources);
            }
        }
        return evalResult;
    }

    /**
     * Convert CQL evaluation result to QuantityDef.
     * <p/>
     * CQL evaluation can return Number, String, or CQL Quantity - never FHIR Quantities.
     *
     * @param result the CQL evaluation result
     * @return QuantityDef containing the numeric value
     * @throws InvalidRequestException if result cannot be converted to a number
     */
    private static QuantityDef convertCqlResultToQuantityDef(Object result) {
        if (result == null) {
            return null;
        }

        // Handle Number (most common case)
        if (result instanceof Number number) {
            return new QuantityDef(number.doubleValue());
        }

        // Handle String with validation
        if (result instanceof String s) {
            try {
                return new QuantityDef(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new InvalidRequestException("String is not a valid number: " + s, e);
            }
        }

        // TODO: Handle CQL Quantity if needed (org.opencds.cqf.cql.engine.runtime.Quantity)
        // For now, unsupported

        throw new InvalidRequestException("Cannot convert CQL result of type " + result.getClass() + " to QuantityDef. "
                + "Expected Number or String.");
    }

    private static ExpressionResult evaluateMeasureObservationFunction(
            CqlEngine cqlEngine,
            VersionedIdentifier libraryIdentifier,
            String functionExpression,
            boolean isBooleanBasis,
            List<Object> functionArguments,
            String exceptionMessageIfNotFunction) {

        // This is gross:  we need to get rid of this:
        if (cqlEngine.getState().getCurrentLibrary() != null) {
            var expressionDefinition = Libraries.resolveExpressionRef(
                    functionExpression, cqlEngine.getState().getCurrentLibrary());

            if (!(expressionDefinition instanceof FunctionDef functionDef)) {
                throw new InvalidRequestException("Measure observation %s does not reference a function definition"
                        .formatted(functionExpression));
            }

            if (!isBooleanBasis) {
                // subject based observations don't have a parameter to pass in
                final List<OperandDef> operands = functionDef.getOperand();

                if (operands.isEmpty()) {
                    throw new InternalErrorException(
                            "Measure observation criteria expression: %s is missing a function parameter matching the population-basis"
                                    .formatted(functionExpression));
                }
            }
        }

        final ExpressionResult expressionResult = executeCqlFunction(
                cqlEngine, libraryIdentifier, functionExpression, functionArguments, exceptionMessageIfNotFunction);

        validateObservationResult(functionArguments, expressionResult.getValue());

        return expressionResult;
    }

    private static ExpressionResult evaluateNonSubValueStratifiersFunction(
            CqlEngine cqlEngine,
            VersionedIdentifier libraryIdentifier,
            String functionExpression,
            List<Object> functionArguments,
            String exceptionMessageIfNotFunction) {

        return executeCqlFunction(
                cqlEngine, libraryIdentifier, functionExpression, functionArguments, exceptionMessageIfNotFunction);
    }

    private static ExpressionResult executeCqlFunction(
            CqlEngine cqlEngine,
            VersionedIdentifier libraryIdentifier,
            String functionExpression,
            List<Object> functionArguments,
            String exceptionMessageIfNotFunction) {

        try {
            return tryExecuteCqlFunction(cqlEngine, libraryIdentifier, functionExpression, functionArguments);

        } catch (RuntimeException exception) {
            if (exception.getMessage().contains(DOES_NOT_HAVE_FUNCTION)) {
                throw new InvalidRequestException(exceptionMessageIfNotFunction, exception);
            } else {
                throw new InvalidRequestException(
                        "CQL Exception while evaluating function: %s".formatted(functionExpression), exception);
            }
        }
    }

    /**
     * Execute a CQL function against the new CQL DSL passing in a function expression name and
     * zero or more arguments.
     *
     * @param engine CQL Engine to run function
     * @param libraryIdentifier Library to run against
     * @param functionExpression The name of the function expression
     * @param functionArguments Zero or more arguments to the function
     * @return ExpressionResults from CQL execution corresponding the function expression
     */
    private static ExpressionResult tryExecuteCqlFunction(
            CqlEngine engine,
            VersionedIdentifier libraryIdentifier,
            String functionExpression,
            List<Object> functionArguments) {

        final EvaluationFunctionRef evaluationFunctionRef =
                buildEvaluationFunctionRef(functionExpression, functionArguments);

        final Builder paramsBuilder = new Builder().library(libraryIdentifier, builder -> {
            builder.expressions(evaluationFunctionRef);
            return Unit.INSTANCE;
        });

        final EvaluationResults evaluationResults = engine.evaluate(paramsBuilder.build());

        return evaluationResults.getOnlyResultOrThrow().get(evaluationFunctionRef);
    }

    private static EvaluationFunctionRef buildEvaluationFunctionRef(
            String criteriaExpression, List<?> functionArguments) {
        return new EvaluationFunctionRef(criteriaExpression, null, functionArguments);
    }

    private static Optional<ExpressionResult> tryGetExpressionResult(
            String expressionName, EvaluationResult evaluationResult) {
        if (expressionName == null) {
            throw new InternalErrorException(
                    "PopulationDef criteria reference is missing for continuous variable observation");
        }

        if (evaluationResult == null) {
            return Optional.empty();
        }

        final Map<String, ExpressionResult> expressionResults = evaluationResult.getExpressionResults();

        if (!expressionResults.containsKey(expressionName)) {
            throw new InvalidRequestException(
                    "Could not find expression result for expression: %s. Available expressions: %s"
                            .formatted(expressionName, expressionResults.keySet()));
        }

        return Optional.of(evaluationResult.getExpressionResults().get(expressionName));
    }

    private static Iterable<?> getResultIterable(
            EvaluationResult evaluationResult, ExpressionResult expressionResult, String subjectTypePart) {
        if (expressionResult.getValue() instanceof Boolean) {
            if ((Boolean.TRUE.equals(expressionResult.getValue()))) {
                // if Boolean, returns context by SubjectType
                var expressionResultForSubjectId = evaluationResult.get(subjectTypePart);

                if (expressionResultForSubjectId == null) {
                    throw new InternalErrorException(
                            "expression result is null for subject type: %s".formatted(subjectTypePart));
                }

                Object booleanResult = expressionResultForSubjectId.getValue();

                // remove evaluated resources
                return Collections.singletonList(booleanResult);
            } else {
                // false result shows nothing
                return Collections.emptyList();
            }
        }

        Object value = expressionResult.getValue();
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        } else {
            return Collections.singletonList(value);
        }
    }

    @Nonnull
    private static List<Object> getFunctionArguments(GroupDef groupDef, Object result) {
        return groupDef.isBooleanBasis() ? List.of() : List.of(result);
    }

    private static void validateObservationResult(List<Object> functionArguments, Object observationResult) {
        if (!(observationResult instanceof String
                || observationResult instanceof Integer
                || observationResult instanceof Double)) {
            throw new IllegalArgumentException(
                    "continuous variable observation CQL \"MeasureObservation\" function result must be of type String, Integer or Double but was: %s"
                            .formatted(
                                    functionArguments.size() > 1
                                            ? EvaluationResultFormatter.printValues(functionArguments)
                                            : EvaluationResultFormatter.printValue(functionArguments.get(0))));
        }
    }

    /**
     * Checks if a MeasureDef has at least one PopulationDef of type MEASUREOBSERVATION
     * across all of its groups.
     *
     * @param measureDef the MeasureDef to check
     * @return true if any PopulationDef in any GroupDef is MEASUREOBSERVATION
     */
    private static boolean hasMeasureObservation(MeasureDef measureDef) {
        if (measureDef == null || measureDef.groups() == null) {
            return false;
        }

        return measureDef.groups().stream()
                .filter(group -> group.populations() != null)
                .flatMap(group -> group.populations().stream())
                .anyMatch(pop -> pop.type() == MeasurePopulationType.MEASUREOBSERVATION);
    }

    private static boolean hasNonSubValueStratifier(MeasureDef measureDef) {
        if (measureDef == null || measureDef.groups() == null) {
            return false;
        }

        return measureDef.groups().stream()
                .map(GroupDef::stratifiers)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(StratifierDef::isNonSubjectValueStratifier);
    }

    @Nonnull
    private static EvaluationResult buildEvaluationResult(
            String expressionName, Map<Object, Object> functionResults, Set<Object> evaluatedResources) {

        final EvaluationResult evaluationResultToReturn = new EvaluationResult();

        evaluationResultToReturn.set(
                new EvaluationExpressionRef(expressionName), new ExpressionResult(functionResults, evaluatedResources));

        return evaluationResultToReturn;
    }

    private static void addToEvaluationResult(
            @Nonnull EvaluationResult result,
            @Nonnull String expressionName,
            @Nonnull Map<Object, Object> functionResults,
            @Nonnull Set<Object> evaluatedResources) {

        result.set(
                new EvaluationExpressionRef(expressionName), new ExpressionResult(functionResults, evaluatedResources));
    }
}
