package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.OperandDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Capture all logic for measure evaluation for continuous variable scoring.
 */
public class ContinuousVariableObservationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ContinuousVariableObservationHandler.class);

    private ContinuousVariableObservationHandler() {
        // static class with private constructor
    }

    static <T extends ICompositeType> List<EvaluationResult> continuousVariableEvaluation(
            CqlEngine context,
            List<MeasureDef> measureDefs,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart,
            // This is a temporary hack to inject FHIR version specific behaviour for
            // Observations and Quantities for continuous variable observations
            ContinuousVariableObservationConverter<T> continuousVariableObservationConverter) {

        final List<MeasureDef> measureDefsWithMeasureObservations = measureDefs.stream()
                // if measure contains measure-observation, otherwise short circuit
                .filter(ContinuousVariableObservationHandler::hasMeasureObservation)
                .toList();

        if (measureDefsWithMeasureObservations.isEmpty()) {
            // Don't need to do anything if there are no measure observations to process
            return List.of();
        }

        final boolean hasLibraryInitialized = LibraryInitHandler.initLibrary(context, libraryIdentifier);

        final List<EvaluationResult> finalResults = new ArrayList<>();

        try {
            // one Library may be linked to multiple Measures
            for (MeasureDef measureDefWithMeasureObservations : measureDefsWithMeasureObservations) {

                // get function for measure-observation from populationDef
                for (GroupDef groupDef : measureDefWithMeasureObservations.groups()) {

                    final List<PopulationDef> measureObservationPopulations = groupDef.populations().stream()
                            .filter(populationDef ->
                                    MeasurePopulationType.MEASUREOBSERVATION.equals(populationDef.type()))
                            .toList();
                    for (PopulationDef populationDef : measureObservationPopulations) {
                        // each measureObservation is evaluated
                        var result = processMeasureObservation(
                                context,
                                evaluationResult,
                                subjectTypePart,
                                groupDef,
                                populationDef,
                                continuousVariableObservationConverter);

                        finalResults.add(result);
                    }
                }
            }
        } finally {
            // We don't want to pop a non-existent library
            if (hasLibraryInitialized) {
                LibraryInitHandler.popLibrary(context);
            }
        }

        return finalResults;
    }

    /**
     * For a given measure observation population, do an ad-hoc function evaluation and
     * accumulate the results that will be subsequently added to the CQL evaluation result.
     */
    private static <T extends ICompositeType> EvaluationResult processMeasureObservation(
            CqlEngine context,
            EvaluationResult evaluationResult,
            String subjectTypePart,
            GroupDef groupDef,
            PopulationDef populationDef,
            ContinuousVariableObservationConverter<T> continuousVariableObservationConverter) {

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
        // makes expression results iterable
        final Iterable<?> resultsIter = getResultIterable(evaluationResult, expressionResult, subjectTypePart);
        // make new expression name for uniquely extracting results
        // this will be used in MeasureEvaluator
        var expressionName = criteriaPopulationId + "-" + observationExpression;

        // loop through measure-population results
        final Map<Object, Object> functionResults = new HashMap<>();
        final Set<Object> evaluatedResources = new HashSet<>();

        for (Object result : resultsIter) {
            final ExpressionResult observationResult =
                    evaluateObservationCriteria(result, observationExpression, groupDef.isBooleanBasis(), context);

            var quantity = continuousVariableObservationConverter.wrapResultAsQuantity(observationResult.value());
            // add function results to existing EvaluationResult under new expression
            // name
            // need a way to capture input parameter here too, otherwise we have no way
            // to connect input objects related to output object
            // key= input parameter to function
            // value= the output Observation resource containing calculated value
            functionResults.put(result, quantity);
            evaluatedResources.addAll(observationResult.evaluatedResources());
        }

        return buildEvaluationResult(expressionName, functionResults, evaluatedResources);
    }

    /**
     * method used to evaluate cql expression defined for 'continuous variable' scoring type
     * measures that have 'measure observation' to calculate This method is called as a second round
     * of processing given it uses 'measure population' results as input data for function
     *
     * @param resource            object that stores results of cql
     * @param criteriaExpression  expression name to call
     * @param isBooleanBasis      the type of result created from expression
     * @param context             cql engine context used to evaluate expression
     * @return cql results for subject requested
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static ExpressionResult evaluateObservationCriteria(
            Object resource, String criteriaExpression, boolean isBooleanBasis, CqlEngine context) {

        var ed = Libraries.resolveExpressionRef(
                criteriaExpression, context.getState().getCurrentLibrary());

        if (!(ed instanceof FunctionDef functionDef)) {
            throw new InvalidRequestException(
                    "Measure observation %s does not reference a function definition".formatted(criteriaExpression));
        }

        Object result;
        context.getState().pushActivationFrame(functionDef, functionDef.getContext());
        try {
            if (!isBooleanBasis) {
                // subject based observations don't have a parameter to pass in
                final List<OperandDef> operands = functionDef.getOperand();

                if (operands.isEmpty()) {
                    throw new InternalErrorException("Measure observation %s does not reference a boolean basis");
                }

                final Variable variableToPush = new Variable(operands.get(0).getName()).withValue(resource);

                context.getState().push(variableToPush);
            }
            result = context.getEvaluationVisitor().visitExpression(ed.getExpression(), context.getState());

        } finally {
            context.getState().popActivationFrame();
        }

        final Set<Object> evaluatedResources = captureEvaluatedResources(context);

        validateObservationResult(resource, result);

        return new ExpressionResult(result, evaluatedResources);
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

        final Map<String, ExpressionResult> expressionResults = evaluationResult.expressionResults;

        if (!expressionResults.containsKey(expressionName)) {
            throw new InvalidRequestException("Could not find expression result for expression: " + expressionName);
        }

        return Optional.of(evaluationResult.expressionResults.get(expressionName));
    }

    private static Iterable<?> getResultIterable(
            EvaluationResult evaluationResult, ExpressionResult expressionResult, String subjectTypePart) {
        if (expressionResult.value() instanceof Boolean) {
            if ((Boolean.TRUE.equals(expressionResult.value()))) {
                // if Boolean, returns context by SubjectType
                Object booleanResult =
                        evaluationResult.forExpression(subjectTypePart).value();
                // remove evaluated resources
                return Collections.singletonList(booleanResult);
            } else {
                // false result shows nothing
                return Collections.emptyList();
            }
        }

        Object value = expressionResult.value();
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        } else {
            return Collections.singletonList(value);
        }
    }

    private static void validateObservationResult(Object result, Object observationResult) {
        if (!(observationResult instanceof String
                || observationResult instanceof Integer
                || observationResult instanceof Double)) {
            throw new IllegalArgumentException(
                    "continuous variable observation CQL \"MeasureObservation\" function result must be of type String, Integer or Double but was: "
                            + result.getClass().getSimpleName());
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

    /**
     * method used to extract evaluated resources touched by CQL criteria expressions
     * @param context cql engine context
     */
    private static Set<Object> captureEvaluatedResources(CqlEngine context) {
        final Set<Object> evaluatedResources;
        if (context.getState().getEvaluatedResources() != null) {
            evaluatedResources = context.getState().getEvaluatedResources();
        } else {
            evaluatedResources = new HashSet<>();
        }
        clearEvaluatedResources(context);

        return evaluatedResources;
    }

    // reset evaluated resources followed by a context evaluation
    private static void clearEvaluatedResources(CqlEngine context) {
        context.getState().clearEvaluatedResources();
    }

    @Nonnull
    private static EvaluationResult buildEvaluationResult(
            String expressionName, Map<Object, Object> functionResults, Set<Object> evaluatedResources) {

        final EvaluationResult evaluationResultToReturn = new EvaluationResult();

        evaluationResultToReturn.expressionResults.put(
                expressionName, new ExpressionResult(functionResults, evaluatedResources));

        return evaluationResultToReturn;
    }
}
