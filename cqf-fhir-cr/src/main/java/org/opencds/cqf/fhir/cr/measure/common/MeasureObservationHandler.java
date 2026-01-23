package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.OperandDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationExpressionRef;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures logic for measure evaluation associated with MEASUREOBSERVATION populations, capturing
 * both continuous variable and ratio continuous variable measures.
 */
public class MeasureObservationHandler {
    private static final Logger logger = LoggerFactory.getLogger(MeasureObservationHandler.class);

    private MeasureObservationHandler() {
        // static class with private constructor
    }

    static List<EvaluationResult> continuousVariableEvaluation(
            CqlEngine context,
            List<MeasureDef> measureDefs,
            VersionedIdentifier libraryIdentifier,
            EvaluationResult evaluationResult,
            String subjectTypePart) {

        final List<MeasureDef> measureDefsWithMeasureObservations = measureDefs.stream()
                // if measure contains measure-observation, otherwise short circuit
                .filter(MeasureObservationHandler::hasMeasureObservation)
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
                                context, evaluationResult, subjectTypePart, groupDef, populationDef);

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
     * Removes observation entries from measureObservationDef when their resource keys
     * match resources in measurePopulationExclusionDef for the given subject.
     * <p/>
     * The observation population stores Map&lt;Resource, QuantityDef&gt; entries. This method
     * removes map entries whose keys match exclusion resources using FHIR resource identity
     * (resource type + logical ID) rather than object instance equality.
     *
     * @param subjectId the subject ID
     * @param measurePopulationExclusionDef population containing resources to exclude (e.g., cancelled encounters)
     * @param measureObservationDef population containing observation maps to filter
     */
    static void removeObservationResourcesInPopulation(
            String subjectId, PopulationDef measurePopulationExclusionDef, PopulationDef measureObservationDef) {

        if (measureObservationDef == null || measurePopulationExclusionDef == null) {
            return;
        }

        final Set<Object> exclusionResources = measurePopulationExclusionDef.getResourcesForSubject(subjectId);
        if (CollectionUtils.isEmpty(exclusionResources)) {
            return;
        }

        final Set<Object> observationResources = measureObservationDef.getResourcesForSubject(subjectId);
        if (CollectionUtils.isEmpty(observationResources)) {
            return;
        }

        logger.debug(
                "Removing {} exclusion resources from {} observation maps for subject {}",
                exclusionResources.size(),
                observationResources.size(),
                subjectId);

        // Make a copy to avoid ConcurrentModificationException when removeExcludedMeasureObservationResource
        // removes empty maps from the original set
        final Set<Object> observationResourcesCopy = new HashSetForFhirResourcesAndCqlTypes<>(observationResources);

        // Iterate over observation resources (which are Maps) and remove matching keys
        for (Object observationResource : observationResourcesCopy) {
            if (observationResource instanceof Map<?, ?> observationMap) {
                removeMatchingKeysFromObservationMap(
                        observationMap, exclusionResources, measureObservationDef, subjectId);
            }
        }
    }

    /**
     * For a given measure observation population, do an ad-hoc function evaluation and
     * accumulate the results that will be subsequently added to the CQL evaluation result.
     */
    private static EvaluationResult processMeasureObservation(
            CqlEngine context,
            EvaluationResult evaluationResult,
            String subjectTypePart,
            GroupDef groupDef,
            PopulationDef populationDef) {

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

        for (Object result : resultsIter) {
            final ExpressionResult observationResult =
                    evaluateObservationCriteria(result, observationExpression, groupDef.isBooleanBasis(), context);

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

    // Added by Claude Sonnet 4.5 on 2025-12-02
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

        var currentLibrary = context.getState().getCurrentLibrary();

        if (currentLibrary == null) {
            throw new InternalErrorException("Current library is null.");
        }

        var expressionDef = Libraries.resolveExpressionRef(criteriaExpression, currentLibrary);

        if (!(expressionDef instanceof FunctionDef functionDef)) {
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
                    throw new InternalErrorException(
                            "Measure observation criteria expression: %s is missing a function parameter matching the population-basis"
                                    .formatted(criteriaExpression));
                }

                final Variable variableToPush = new Variable(operands.get(0).getName()).withValue(resource);

                context.getState().push(variableToPush);
            }
            if (expressionDef.getExpression() == null) {
                throw new InternalErrorException("Current library is null.");
            }
            result = context.getEvaluationVisitor().visitExpression(expressionDef.getExpression(), context.getState());

        } finally {
            context.getState().popActivationFrame();
        }

        final Set<Object> evaluatedResources = captureEvaluatedResources(context);

        validateObservationResult(resource, result);

        return new ExpressionResult(result, evaluatedResources);
    }

    /**
     * Removes keys from an observation map that match exclusion resources.
     * <p/>
     * This method uses FHIR resource identity (resource type + logical ID) for matching
     * rather than object instance equality, since the exclusion resources and observation
     * map keys may be separate Java object instances representing the same FHIR resource.
     *
     * @param observationMap observation map containing Resource -> QuantityDef entries
     * @param exclusionResources set of resources to exclude
     * @param measureObservationDef the observation population definition
     * @param subjectId the subject ID
     */
    private static void removeMatchingKeysFromObservationMap(
            Map<?, ?> observationMap,
            Set<Object> exclusionResources,
            PopulationDef measureObservationDef,
            String subjectId) {

        // Find observation map keys that match any exclusion resource
        for (Object exclusionResource : exclusionResources) {
            // Check if this exclusion resource matches any key in the observation map
            // Must use custom equality that compares FHIR resource identity, not object instance
            boolean matchFound = observationMap.keySet().stream()
                    .anyMatch(mapKey -> CqlFhirResourceAndCqlTypeUtils.areObjectsEqual(mapKey, exclusionResource));

            if (matchFound) {
                logger.debug(
                        "Removing observation for excluded resource: {}",
                        EvaluationResultFormatter.formatResource(exclusionResource));
                // Remove the entry from the inner map using the PopulationDef's removal method
                // This ensures proper handling of the Map<String, Set<Map<Resource, QuantityDef>>> structure
                measureObservationDef.removeExcludedMeasureObservationResource(subjectId, exclusionResource);
            }
        }
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

    private static EvaluationResult buildEvaluationResult(
            String expressionName, Map<Object, Object> functionResults, Set<Object> evaluatedResources) {

        final EvaluationResult evaluationResultToReturn = new EvaluationResult();

        evaluationResultToReturn.set(
                new EvaluationExpressionRef(expressionName), new ExpressionResult(functionResults, evaluatedResources));

        return evaluationResultToReturn;
    }
}
