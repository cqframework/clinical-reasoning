package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCEPTION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.INITIALPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREOBSERVATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATIONEXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOREXCLUSION;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the core Measure evaluation logic that's defined in the
 * Quality Measure
 * implementation guide and HQMF specifications. There are a number of
 * model-independent concepts
 * such as "groups", "populations", and "stratifiers" that can be used across a
 * number of different
 * data models including FHIR, QDM, and QICore. To the extent feasible, this
 * class is intended to be
 * model-independent so that it can be used in any Java-based implementation of
 * Quality Measure
 * evaluation.
 *
 * @see <a href=
 *      "http://hl7.org/fhir/us/cqfmeasures/introduction.html">http://hl7.org/fhir/us/cqfmeasures/introduction.html</a>
 * @see <a href=
 *      "http://www.hl7.org/implement/standards/product_brief.cfm?product_id=97">http://www.hl7.org/implement/standards/product_brief.cfm?product_id=97</a>
 */
@SuppressWarnings({"squid:S1135", "squid:S3776"})
public class MeasureEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);

    private final PopulationBasisValidator populationBasisValidator;

    public MeasureEvaluator(PopulationBasisValidator populationBasisValidator) {
        this.populationBasisValidator = populationBasisValidator;
    }

    public MeasureDef evaluate(
            MeasureDef measureDef,
            MeasureEvalType measureEvalType,
            String subjectType,
            String subjectId,
            EvaluationResult evaluationResult,
            boolean applyScoring,
            MeasureEvaluationState state) {
        Objects.requireNonNull(measureDef, "measureDef is a required argument");
        Objects.requireNonNull(subjectId, "subjectIds is a required argument");

        return switch (measureEvalType) {
            case PATIENT, SUBJECT ->
                this.evaluateSubject(
                        measureDef,
                        subjectType,
                        subjectId,
                        MeasureReportType.INDIVIDUAL,
                        evaluationResult,
                        applyScoring,
                        state);
            case SUBJECTLIST ->
                this.evaluateSubject(
                        measureDef,
                        subjectType,
                        subjectId,
                        MeasureReportType.SUBJECTLIST,
                        evaluationResult,
                        applyScoring,
                        state);
            case PATIENTLIST ->
                // DSTU3 Only
                this.evaluateSubject(
                        measureDef,
                        subjectType,
                        subjectId,
                        MeasureReportType.PATIENTLIST,
                        evaluationResult,
                        applyScoring,
                        state);
            case POPULATION ->
                this.evaluateSubject(
                        measureDef,
                        subjectType,
                        subjectId,
                        MeasureReportType.SUMMARY,
                        evaluationResult,
                        applyScoring,
                        state);
        };
    }

    protected MeasureDef evaluateSubject(
            MeasureDef measureDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring,
            MeasureEvaluationState state) {
        evaluateSdes(subjectId, measureDef.sdes(), evaluationResult, state);
        for (GroupDef groupDef : measureDef.groups()) {
            evaluateGroup(
                    measureDef, groupDef, subjectType, subjectId, reportType, evaluationResult, applyScoring, state);
        }
        return measureDef;
    }

    @SuppressWarnings("unchecked")
    protected Iterable<Object> evaluatePopulationCriteria(
            String subjectType,
            ExpressionResult expressionResult,
            EvaluationResult evaluationResult,
            Set<Object> outEvaluatedResources) {

        if (expressionResult != null
                && !expressionResult.getEvaluatedResources().isEmpty()) {
            outEvaluatedResources.addAll(expressionResult.getEvaluatedResources());
        }

        if (expressionResult == null || expressionResult.getValue() == null) {
            return Collections.emptyList();
        }

        if (expressionResult.getValue() instanceof Boolean) {
            if ((Boolean.TRUE.equals(expressionResult.getValue()))) {
                // if Boolean, returns context by SubjectType
                Object booleanResult = evaluationResult.get(subjectType).getValue();
                // remove evaluated resources
                return Collections.singletonList(booleanResult);
            } else {
                // false result shows nothing
                return Collections.emptyList();
            }
        }

        Object value = expressionResult.getValue();
        if (value instanceof Iterable<?>) {
            return (Iterable<Object>) value;
        } else {
            return Collections.singletonList(value);
        }
    }

    @SuppressWarnings("unchecked")
    protected Iterable<Object> evaluateSupportingCriteria(ExpressionResult expressionResult) {

        // Case 1 — true null
        if (expressionResult == null || expressionResult.getValue() == null) {
            return null; // need to preserve result
        }

        Object value = expressionResult.getValue();

        // Case 2 — list
        if (value instanceof Iterable<?>) {
            return (Iterable<Object>) value; // may be empty or not
        }

        // Case 3 — scalar
        return List.of(value);
    }

    protected PopulationDef evaluatePopulationMembership(
            String subjectType,
            String subjectId,
            PopulationDef inclusionDef,
            EvaluationResult evaluationResult,
            MeasureEvaluationState state) {
        return evaluatePopulationMembership(subjectType, subjectId, inclusionDef, evaluationResult, null, state);
    }

    protected PopulationDef evaluatePopulationMembership(
            String subjectType,
            String subjectId,
            PopulationDef inclusionDef,
            EvaluationResult evaluationResult,
            String expression,
            MeasureEvaluationState state) {
        // use expressionName passed in instead of criteria expression defined on populationDef
        // this is mainly for measureObservation functions

        ExpressionResult matchingResult;
        if (expression == null || expression.isEmpty()) {
            // find matching expression
            matchingResult = evaluationResult.get(inclusionDef.expression());
        } else {
            matchingResult = evaluationResult.get(expression);
        }

        var popState = state.population(inclusionDef);

        // Add Resources from SubjectId
        for (Object resource : evaluatePopulationCriteria(
                subjectType, matchingResult, evaluationResult, popState.getEvaluatedResources())) {
            // hashmap instead of set
            popState.addResource(subjectId, resource);
        }

        return inclusionDef;
    }

    /**
     * This method will identify the PopulationDef MeasureObservation linked to the InclusionDef population by PopulationDef.id
     * If this method returns null, it is because MeasureObservation is not defined, or was incorrectly defined.
     * One example is MeasureObservation with criteriaDef value, that does not link to the correct PopulationDef (Numerator or Denominator)
     * @param groupDef the MeasureDef GroupDef object
     * @param populationType MeasurePopulationType like MeasureObservation
     * @param inclusionDef The PopulationDef linked to the criteriaReference
     * @return populationDef
     */
    @Nullable
    private PopulationDef getPopulationDefByCriteriaRef(
            GroupDef groupDef, MeasurePopulationType populationType, PopulationDef inclusionDef) {
        return groupDef.getPopulationDefs(populationType).stream()
                .filter(x -> {
                    if (x.getCriteriaReference() == null) {
                        throw new MeasureValidationException("Criteria reference is null on PopulationDef");
                    }
                    return x.getCriteriaReference().equals(inclusionDef.id());
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Check that Ratio Continuous Variable Measure has required definitions to proceed
     * @param groupDef GroupDef object of MeasureDef
     */
    protected void validateRatioContinuousVariable(GroupDef groupDef) {
        // must have 2 MeasureObservations defined
        if (!groupDef.getPopulationDefs(MEASUREOBSERVATION).isEmpty()
                && groupDef.getPopulationDefs(MEASUREOBSERVATION).size() != 2) {
            throw new MeasureValidationException(
                    "Ratio Continuous Variable requires 2 Measure Observations defined, you have: %s"
                            .formatted(groupDef.getPopulationDefs(MEASUREOBSERVATION)
                                    .size()));
        }
    }

    protected void evaluateProportion(
            GroupDef groupDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring,
            MeasureEvaluationState state) {
        // check populations
        ScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationDef::type).toList(), groupDef.measureScoring());

        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationDef numerator = groupDef.getSingle(NUMERATOR);
        PopulationDef denominator = groupDef.getSingle(DENOMINATOR);
        PopulationDef denominatorExclusion = groupDef.getSingle(DENOMINATOREXCLUSION);
        PopulationDef denominatorException = groupDef.getSingle(DENOMINATOREXCEPTION);
        PopulationDef numeratorExclusion = groupDef.getSingle(NUMERATOREXCLUSION);
        PopulationDef dateOfCompliance = groupDef.getSingle(DATEOFCOMPLIANCE);
        // Ratio Continuous Variable ONLY

        PopulationDef observationNum = getPopulationDefByCriteriaRef(groupDef, MEASUREOBSERVATION, numerator);
        PopulationDef observationDen = getPopulationDefByCriteriaRef(groupDef, MEASUREOBSERVATION, denominator);
        validateRatioContinuousVariable(groupDef);
        // Retrieve intersection of populations and results
        // add resources
        // add subject

        // Evaluate Population Expressions
        initialPopulation =
                evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult, state);
        denominator = evaluatePopulationMembership(subjectType, subjectId, denominator, evaluationResult, state);
        numerator = evaluatePopulationMembership(subjectType, subjectId, numerator, evaluationResult, state);
        // Evaluate Exclusions and Exception Populations
        if (denominatorExclusion != null) {
            denominatorExclusion =
                    evaluatePopulationMembership(subjectType, subjectId, denominatorExclusion, evaluationResult, state);
        }
        if (denominatorException != null) {
            denominatorException =
                    evaluatePopulationMembership(subjectType, subjectId, denominatorException, evaluationResult, state);
        }
        if (numeratorExclusion != null) {
            numeratorExclusion =
                    evaluatePopulationMembership(subjectType, subjectId, numeratorExclusion, evaluationResult, state);
        }
        // Apply set-algebra rules (subset enforcement, exclusions, exceptions)
        PopulationSetAlgebra.applyProportionRules(subjectId, groupDef, applyScoring, state);
        if (reportType.equals(MeasureReportType.INDIVIDUAL) && dateOfCompliance != null) {
            var doc = evaluateDateOfCompliance(dateOfCompliance, evaluationResult);
            state.population(dateOfCompliance).addResource(subjectId, doc);
        }
        for (PopulationDef p : groupDef.populations()) {
            populateSupportingEvidence(p, reportType, evaluationResult, subjectId, state);
        }
        // Ratio Cont Variable Scoring
        if (observationNum != null && observationDen != null) {
            // Num alignment
            var expressionNameNum = getCriteriaExpressionName(observationNum);
            // populate Measure observation function results
            evaluatePopulationMembership(
                    subjectType, subjectId, observationNum, evaluationResult, expressionNameNum, state);
            // Align to Numerator
            ObservationAligner.retainObservationResourcesInPopulation(subjectId, numerator, observationNum, state);
            ObservationAligner.retainObservationSubjectResourcesInPopulation(
                    state.population(numerator), state.population(observationNum));
            // remove Numerator Exclusions
            if (numeratorExclusion != null) {
                ObservationAligner.removeObservationSubjectResourcesInPopulation(
                        state.population(numeratorExclusion), state.population(observationNum));
                MeasureObservationHandler.removeObservationResourcesInPopulation(
                        subjectId, numeratorExclusion, observationNum, state);
            }
            // Den alignment
            var expressionNameDen = getCriteriaExpressionName(observationDen);
            // populate Measure observation function results
            evaluatePopulationMembership(
                    subjectType, subjectId, observationDen, evaluationResult, expressionNameDen, state);
            // align to Denominator Results
            ObservationAligner.retainObservationResourcesInPopulation(subjectId, denominator, observationDen, state);
            ObservationAligner.retainObservationSubjectResourcesInPopulation(
                    state.population(denominator), state.population(observationDen));
            // remove Denominator Exclusions
            if (denominatorExclusion != null) {
                ObservationAligner.removeObservationSubjectResourcesInPopulation(
                        state.population(denominatorExclusion), state.population(observationDen));
                MeasureObservationHandler.removeObservationResourcesInPopulation(
                        subjectId, denominatorExclusion, observationDen, state);
            }
        }
    }

    protected void populateSupportingEvidence(
            PopulationDef populationDef,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            String subjectId,
            MeasureEvaluationState state) {
        // only enabled for subject level reports
        if (reportType == MeasureReportType.INDIVIDUAL
                && !CollectionUtils.isEmpty(populationDef.getSupportingEvidenceDefs())) {
            var extDef = populationDef.getSupportingEvidenceDefs();
            for (SupportingEvidenceDef e : extDef) {
                var result = evaluationResult.get(e.getExpression());
                if (result == null) {
                    throw new MeasureValidationException(
                            "Supporting Evidence defined expression: '%s', is not found in Evaluation Results"
                                    .formatted(e.getExpression()));
                }
                var object = evaluateSupportingCriteria(result);
                state.supportingEvidence(e).addResource(subjectId, object);
            }
        }
    }

    protected String getCriteriaExpressionName(PopulationDef populationDef) {
        return populationDef.getCriteriaReference() + "-" + populationDef.expression();
    }

    protected void evaluateContinuousVariable(
            GroupDef groupDef,
            String subjectType,
            String subjectId,
            EvaluationResult evaluationResult,
            boolean applyScoring,
            MeasureReportType reportType,
            MeasureEvaluationState state) {
        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
        PopulationDef measurePopulationExclusion = groupDef.getSingle(MEASUREPOPULATIONEXCLUSION);
        PopulationDef measurePopulationObservation = groupDef.getSingle(MEASUREOBSERVATION);
        // Validate Required Populations are Present
        ScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationDef::type).toList(), MeasureScoring.CONTINUOUSVARIABLE);

        initialPopulation =
                evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult, state);
        // Evaluate Population Expressions
        measurePopulation =
                evaluatePopulationMembership(subjectType, subjectId, measurePopulation, evaluationResult, state);
        if (measurePopulationExclusion != null) {
            evaluatePopulationMembership(
                    subjectType, subjectId, groupDef.getSingle(MEASUREPOPULATIONEXCLUSION), evaluationResult, state);
        }
        // Apply set-algebra rules (subset enforcement, exclusions)
        PopulationSetAlgebra.applyContinuousVariableRules(subjectId, groupDef, applyScoring, state);
        if (measurePopulationObservation != null) {
            // only Measure Population resources need to be removed
            var expressionName = measurePopulationObservation.getCriteriaReference() + "-"
                    + measurePopulationObservation.expression();
            // assumes only one population
            evaluatePopulationMembership(
                    subjectType,
                    subjectId,
                    groupDef.getSingle(MEASUREOBSERVATION),
                    evaluationResult,
                    expressionName,
                    state);
            if (applyScoring && measurePopulation != null) {
                // only measureObservations that intersect with measureObservation should be retained
                ObservationAligner.retainObservationResourcesInPopulation(
                        subjectId, measurePopulation, measurePopulationObservation, state);
                ObservationAligner.retainObservationSubjectResourcesInPopulation(
                        state.population(measurePopulation), state.population(measurePopulationObservation));
                // measure observations also need to make sure they remove measure-population-exclusions
                if (measurePopulationExclusion != null) {

                    MeasureObservationHandler.removeObservationResourcesInPopulation(
                            subjectId, measurePopulationExclusion, measurePopulationObservation, state);
                    ObservationAligner.removeObservationSubjectResourcesInPopulation(
                            state.population(measurePopulationExclusion),
                            state.population(measurePopulationObservation));
                }
            }
        }
        for (PopulationDef p : groupDef.populations()) {
            populateSupportingEvidence(p, reportType, evaluationResult, subjectId, state);
        }
    }

    protected void evaluateCohort(
            GroupDef groupDef,
            String subjectType,
            String subjectId,
            EvaluationResult evaluationResult,
            MeasureReportType reportType,
            MeasureEvaluationState state) {
        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        // Validate Required Populations are Present
        ScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationDef::type).toList(), MeasureScoring.COHORT);
        // Evaluate Population
        evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult, state);

        // supporting evidence
        for (PopulationDef p : groupDef.populations()) {
            populateSupportingEvidence(p, reportType, evaluationResult, subjectId, state);
        }
    }

    protected void evaluateGroup(
            MeasureDef measureDef,
            GroupDef groupDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring,
            MeasureEvaluationState state) {

        populationBasisValidator.validateGroupPopulations(measureDef, groupDef, evaluationResult);
        populationBasisValidator.validateStratifiers(measureDef, groupDef, evaluationResult);

        evaluateStratifiers(subjectId, groupDef.stratifiers(), evaluationResult, groupDef, state);

        var scoring = groupDef.measureScoring();
        switch (scoring) {
            case PROPORTION, RATIO:
                evaluateProportion(groupDef, subjectType, subjectId, reportType, evaluationResult, applyScoring, state);
                break;
            case CONTINUOUSVARIABLE:
                evaluateContinuousVariable(
                        groupDef, subjectType, subjectId, evaluationResult, applyScoring, reportType, state);
                break;
            case COHORT:
                evaluateCohort(groupDef, subjectType, subjectId, evaluationResult, reportType, state);
                break;
        }
    }

    protected Object evaluateDateOfCompliance(PopulationDef populationDef, EvaluationResult evaluationResult) {
        return evaluationResult.get(populationDef.expression()).getValue();
    }

    protected void evaluateSdes(
            String subjectId, List<SdeDef> sdes, EvaluationResult evaluationResult, MeasureEvaluationState state) {
        for (SdeDef sde : sdes) {
            var expressionResult = evaluationResult.get(sde.expression());
            Object result = expressionResult.getValue();
            // TODO: This is a hack-around for an cql engine bug. Need to investigate.
            if ((result instanceof List<?> list) && (list.size() == 1) && list.get(0) == null) {
                result = null;
            }

            state.sde(sde).putResult(subjectId, result, expressionResult.getEvaluatedResources());
        }
    }

    protected void evaluateStratifiers(
            String subjectId,
            List<StratifierDef> stratifierDefs,
            EvaluationResult evaluationResult,
            GroupDef groupDef,
            MeasureEvaluationState state) {
        for (StratifierDef stratifierDef : stratifierDefs) {

            evaluateStratifier(subjectId, evaluationResult, stratifierDef, groupDef, state);
        }
    }

    private void evaluateStratifier(
            String subjectId,
            EvaluationResult evaluationResult,
            StratifierDef stratifierDef,
            GroupDef groupDef,
            MeasureEvaluationState state) {
        if (stratifierDef.isCriteriaStratifier()) {
            addCriteriaStratifierResult(subjectId, evaluationResult, stratifierDef, state);
        } else {
            addValueOrNonSubjectValueStratifierResult(
                    stratifierDef.components(), evaluationResult, subjectId, groupDef, state);
        }
    }

    /**
     * Modified by Claude: Changed visibility from private to package-private for testability.
     * Replaced Optional.ofNullable() pattern with explicit null checks and added logger.warn()
     * for better observability when stratifier component expressions return null.
     */
    void addValueOrNonSubjectValueStratifierResult(
            List<StratifierComponentDef> components,
            EvaluationResult evaluationResult,
            String subjectId,
            GroupDef groupDef,
            MeasureEvaluationState state) {

        for (StratifierComponentDef component : components) {
            if (groupDef.isBooleanBasis()) {
                handleBooleanBasisComponent(component, evaluationResult, subjectId, state);
            } else {
                handleNonBooleanBasisComponent(component, evaluationResult, subjectId, groupDef, state);
            }
        }
    }

    private void handleBooleanBasisComponent(
            StratifierComponentDef component,
            EvaluationResult evaluationResult,
            String subjectId,
            MeasureEvaluationState state) {

        var expressionResult = evaluationResult.get(component.expression());

        if (expressionResult == null || expressionResult.getValue() == null) {
            logger.warn(
                    "Stratifier component expression '{}' returned null result for subject '{}'",
                    component.expression(),
                    subjectId);
            return; // short-circuit
        }

        state.component(component)
                .putResult(subjectId, expressionResult.getValue(), expressionResult.getEvaluatedResources());
    }

    private void handleNonBooleanBasisComponent(
            StratifierComponentDef component,
            EvaluationResult evaluationResult,
            String subjectId,
            GroupDef groupDef,
            MeasureEvaluationState state) {

        // First: look for function results on INITIALPOPULATION
        for (PopulationDef popDef : groupDef.populations()) {
            if (popDef.type() != MeasurePopulationType.INITIALPOPULATION) {
                continue;
            }

            var expressionResult = evaluationResult.get(popDef.id() + "-" + component.expression());

            if (expressionResult != null && expressionResult.getValue() != null) {
                state.component(component)
                        .putResult(subjectId, expressionResult.getValue(), expressionResult.getEvaluatedResources());
                return; // short-circuit once function result is found
            }
        }

        // Fallback: scalar (non-function) expression
        var fallbackResult = evaluationResult.get(component.expression());
        if (fallbackResult == null || fallbackResult.getValue() == null) {
            logger.warn(
                    "Stratifier component expression '{}' returned null result for subject '{}' (fallback)",
                    component.expression(),
                    subjectId);
            return; // short-circuit
        }

        state.component(component)
                .putResult(subjectId, fallbackResult.getValue(), fallbackResult.getEvaluatedResources());
    }

    /**
     * Modified by Claude: Changed visibility from private to package-private for testability.
     * Replaced Optional.ofNullable() pattern with explicit null checks and added logger.warn()
     * for better observability when stratifier expressions return null.
     */
    void addCriteriaStratifierResult(
            String subjectId,
            EvaluationResult evaluationResult,
            StratifierDef stratifierDef,
            MeasureEvaluationState state) {

        var expressionResult = evaluationResult.get(stratifierDef.expression());

        if (expressionResult == null || expressionResult.getValue() == null) {
            logger.warn(
                    "Stratifier expression '{}' returned null result for subject '{}'",
                    stratifierDef.expression(),
                    subjectId);
            return;
        }

        state.stratifier(stratifierDef)
                .putResult(subjectId, expressionResult.getValue(), expressionResult.getEvaluatedResources());
    }
}
