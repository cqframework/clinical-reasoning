package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.Variable;

/**
 * Capture all logic for measure evaluation for continuous variable scoring.
 */
public class ContinuousVariableObservationHandler {

    private ContinuousVariableObservationHandler() {
        // static class with private constructor
    }

    static List<MeasureObservationResult> continuousVariableEvaluation(
            CqlEngine context,
            List<MeasureDef> measureDefs,
            List<VersionedIdentifier> libraryIdentifiers,
            EvaluationResult evaluationResult,
            String subjectTypePart) {

        final List<MeasureDef> measureDefsWithMeasureObservations = measureDefs.stream()
                // if measure contains measure-observation, otherwise short circuit
                .filter(ContinuousVariableObservationHandler::hasMeasureObservation)
                .toList();

        if (measureDefsWithMeasureObservations.isEmpty()) {
            // Don't need to do anything if there are no measure observations to process
            return List.of();
        }

        // measure Observation Path, have to re-initialize everything again
        LibraryHandler.initLibraries(context, libraryIdentifiers);

        final List<MeasureObservationResult> finalResults = new ArrayList<>();

        // one Library may be linked to multiple Measures
        for (MeasureDef measureDefWithMeasureObservations : measureDefsWithMeasureObservations) {

            // get function for measure-observation from populationDef
            for (GroupDef groupDef : measureDefWithMeasureObservations.groups()) {

                final List<PopulationDef> measureObservationPopulations = groupDef.populations().stream()
                        .filter(populationDef -> MeasurePopulationType.MEASUREOBSERVATION.equals(populationDef.type()))
                        .toList();
                for (PopulationDef populationDef : measureObservationPopulations) {
                    // each measureObservation is evaluated
                    var results = processMeasureObservation(
                            context, evaluationResult, subjectTypePart, groupDef, populationDef);

                    finalResults.addAll(results);
                }
            }
        }

        return finalResults;
    }

    // LUKETODO:  javadoc
    private static List<MeasureObservationResult> processMeasureObservation(
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
            return List.of();
        }

        final ExpressionResult expressionResult = optExpressionResult.get();
        // makes expression results iterable
        var resultsIter = getResultIterable(evaluationResult, expressionResult, subjectTypePart);
        // make new expression name for uniquely extracting results
        // this will be used in MeasureEvaluator
        var expressionName = criteriaPopulationId + "-" + observationExpression;
        // loop through measure-population results
        int index = 0;
        Map<Object, Object> functionResults = new HashMap<>();
        Set<Object> evaluatedResources = new HashSet<>();
        final List<MeasureObservationResult> results = new ArrayList<>();
        for (Object result : resultsIter) {
            Object observationResult = evaluateObservationCriteria(
                    result, observationExpression, evaluatedResources, groupDef.isBooleanBasis(), context);

            validateObservationResult(result, observationResult);

            var observationId = expressionName + "-" + index;
            // wrap result in Observation resource to avoid duplicate results data loss
            // in set object
            Observation observation = wrapResultAsObservation(observationId, observationId, observationResult);
            // add function results to existing EvaluationResult under new expression
            // name
            // need a way to capture input parameter here too, otherwise we have no way
            // to connect input objects related to output object
            // key= input parameter to function
            // value= the output Observation resource containing calculated value
            functionResults.put(result, observation);

            final ExpressionResult expressionResultFromFunction =
                    new ExpressionResult(functionResults, evaluatedResources);

            final MeasureObservationResult measureObservationResult =
                    new MeasureObservationResult(expressionName, expressionResultFromFunction);

            results.add(measureObservationResult);

            index++;
        }

        return results;
    }

    /**
     * method used to evaluate cql expression defined for 'continuous variable' scoring type measures that have 'measure observation' to calculate
     * This method is called as a second round of processing given it uses 'measure population' results as input data for function
     * @param resource object that stores results of cql
     * @param criteriaExpression expression name to call
     * @param outEvaluatedResources set to store evaluated resources touched
     * @param isBooleanBasis the type of result created from expression
     * @param context cql engine context used to evaluate expression
     * @return cql results for subject requested
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static Object evaluateObservationCriteria(
            Object resource,
            String criteriaExpression,
            Set<Object> outEvaluatedResources,
            boolean isBooleanBasis,
            CqlEngine context) {

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
                // LUKETODO:  error handling
                context.getState()
                        .push(new Variable(functionDef.getOperand().get(0).getName()).withValue(resource));
            }
            result = context.getEvaluationVisitor().visitExpression(ed.getExpression(), context.getState());

        } finally {
            context.getState().popActivationFrame();
        }

        captureEvaluatedResources(outEvaluatedResources, context);

        return result;
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
     * @param outEvaluatedResources set object used to capture resources touched
     * @param context cql engine context
     */
    private static void captureEvaluatedResources(Set<Object> outEvaluatedResources, CqlEngine context) {
        if (outEvaluatedResources != null && context.getState().getEvaluatedResources() != null) {
            outEvaluatedResources.addAll(context.getState().getEvaluatedResources());
        }
        clearEvaluatedResources(context);
    }

    // reset evaluated resources followed by a context evaluation
    private static void clearEvaluatedResources(CqlEngine context) {
        context.getState().clearEvaluatedResources();
    }

    private static Observation wrapResultAsObservation(String id, String observationName, Object result) {

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(id);
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setValue(convertToQuantity(result));
        obs.setCode(cc);

        return obs;
    }

    private static Quantity convertToQuantity(Object obj) {
        if (obj == null) return null;

        Quantity q = new Quantity();

        if (obj instanceof Quantity existing) {
            return existing;
        } else if (obj instanceof Number number) {
            q.setValue(number.doubleValue());
        } else if (obj instanceof String s) {
            try {
                q.setValue(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("String is not a valid number: " + s, e);
            }
        } else {
            throw new IllegalArgumentException("Cannot convert object of type " + obj.getClass() + " to Quantity");
        }

        return q;
    }

    // LUKETODO:  this is used by DSTU3 only:  what to do with this?
    /**
     * Measures with defined scoring type of 'continuous-variable' where a defined 'measure-observation' population is used to evaluate results of 'measure-population'.
     * This method is a downstream calculation given it requires calculated results before it can be called.
     * Results are then added to associated MeasureDef
     * @param measureDef measure defined objects that are populated from criteria expression results
     * @param context cql engine context used to evaluate results
     */
    public static void continuousVariableObservation(MeasureDef measureDef, CqlEngine context) {
        // Continuous Variable?
        for (GroupDef groupDef : measureDef.groups()) {
            // Measure Observation defined?
            if (groupDef.measureScoring().equals(MeasureScoring.CONTINUOUSVARIABLE)
                    && groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION) != null) {

                PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
                PopulationDef measureObservation = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);

                // Inject MeasurePopulation results into Measure Observation Function
                Map<String, Set<Object>> subjectResources = measurePopulation.getSubjectResources();

                for (Map.Entry<String, Set<Object>> entry : subjectResources.entrySet()) {
                    String subjectId = entry.getKey();
                    Set<Object> resourcesForSubject = entry.getValue();

                    for (Object resource : resourcesForSubject) {
                        Object observationResult = evaluateObservationCriteria(
                                resource,
                                measureObservation.expression(),
                                measureObservation.getEvaluatedResources(),
                                groupDef.isBooleanBasis(),
                                context);
                        measureObservation.addResource(observationResult);
                        measureObservation.addResource(subjectId, observationResult);
                    }
                }
            }
        }
    }
}
