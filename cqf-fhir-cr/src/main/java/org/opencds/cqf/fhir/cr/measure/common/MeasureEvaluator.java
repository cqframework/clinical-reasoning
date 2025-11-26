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

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureScoringTypePopulations;
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
            boolean applyScoring) {
        Objects.requireNonNull(measureDef, "measureDef is a required argument");
        Objects.requireNonNull(subjectId, "subjectIds is a required argument");

        return switch (measureEvalType) {
            case PATIENT, SUBJECT -> this.evaluateSubject(
                    measureDef, subjectType, subjectId, MeasureReportType.INDIVIDUAL, evaluationResult, applyScoring);
            case SUBJECTLIST -> this.evaluateSubject(
                    measureDef, subjectType, subjectId, MeasureReportType.SUBJECTLIST, evaluationResult, applyScoring);
            case PATIENTLIST ->
            // DSTU3 Only
            this.evaluateSubject(
                    measureDef, subjectType, subjectId, MeasureReportType.PATIENTLIST, evaluationResult, applyScoring);
            case POPULATION -> this.evaluateSubject(
                    measureDef, subjectType, subjectId, MeasureReportType.SUMMARY, evaluationResult, applyScoring);
        };
    }

    protected MeasureDef evaluateSubject(
            MeasureDef measureDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring) {
        evaluateSdes(subjectId, measureDef.sdes(), evaluationResult);
        for (GroupDef groupDef : measureDef.groups()) {
            evaluateGroup(measureDef, groupDef, subjectType, subjectId, reportType, evaluationResult, applyScoring);
        }
        return measureDef;
    }

    @SuppressWarnings("unchecked")
    protected Iterable<Object> evaluatePopulationCriteria(
            String subjectType,
            ExpressionResult expressionResult,
            EvaluationResult evaluationResult,
            Set<Object> outEvaluatedResources) {

        if (expressionResult != null && !expressionResult.evaluatedResources().isEmpty()) {
            outEvaluatedResources.addAll(expressionResult.evaluatedResources());
        }

        if (expressionResult == null || expressionResult.value() == null) {
            return Collections.emptyList();
        }

        if (expressionResult.value() instanceof Boolean) {
            if ((Boolean.TRUE.equals(expressionResult.value()))) {
                // if Boolean, returns context by SubjectType
                Object booleanResult =
                        evaluationResult.forExpression(subjectType).value();
                // remove evaluated resources
                return Collections.singletonList(booleanResult);
            } else {
                // false result shows nothing
                return Collections.emptyList();
            }
        }

        Object value = expressionResult.value();
        if (value instanceof Iterable<?>) {
            return (Iterable<Object>) value;
        } else {
            return Collections.singletonList(value);
        }
    }

    protected PopulationDef evaluatePopulationMembership(
            String subjectType, String subjectId, PopulationDef inclusionDef, EvaluationResult evaluationResult) {
        return evaluatePopulationMembership(subjectType, subjectId, inclusionDef, evaluationResult, null);
    }

    protected PopulationDef evaluatePopulationMembership(
            String subjectType,
            String subjectId,
            PopulationDef inclusionDef,
            EvaluationResult evaluationResult,
            String expression) {
        // use expressionName passed in instead of criteria expression defined on populationDef
        // this is mainly for measureObservation functions

        ExpressionResult matchingResult;
        if (expression == null || expression.isEmpty()) {
            // find matching expression
            matchingResult = evaluationResult.forExpression(inclusionDef.expression());
        } else {
            matchingResult = evaluationResult.forExpression(expression);
        }

        // Add Resources from SubjectId
        for (Object resource : evaluatePopulationCriteria(
                subjectType, matchingResult, evaluationResult, inclusionDef.getEvaluatedResources())) {
            // hashmap instead of set
            inclusionDef.addResource(subjectId, resource);
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
        return groupDef.get(populationType).stream()
                .filter(x -> {
                    if (x.getCriteriaReference() == null) {
                        throw new InvalidRequestException("Criteria reference is null on PopulationDef");
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
        if (!groupDef.get(MEASUREOBSERVATION).isEmpty()
                && groupDef.get(MEASUREOBSERVATION).size() != 2) {
            throw new InvalidRequestException(
                    "Ratio Continuous Variable requires 2 Measure Observations defined, you have: %s"
                            .formatted(groupDef.get(MEASUREOBSERVATION).size()));
        }
    }

    protected void evaluateProportion(
            GroupDef groupDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring) {
        // check populations
        R4MeasureScoringTypePopulations.validateScoringTypePopulations(
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
        initialPopulation = evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult);
        denominator = evaluatePopulationMembership(subjectType, subjectId, denominator, evaluationResult);
        numerator = evaluatePopulationMembership(subjectType, subjectId, numerator, evaluationResult);
        if (applyScoring) {
            // remove denominator values not in IP
            denominator.retainAllResources(subjectId, initialPopulation);
            denominator.retainAllSubjects(initialPopulation);
            // remove numerator values if not in Denominator
            numerator.retainAllSubjects(denominator);
            numerator.retainAllResources(subjectId, denominator);
        }
        // Evaluate Exclusions and Exception Populations
        if (denominatorExclusion != null) {
            denominatorExclusion =
                    evaluatePopulationMembership(subjectType, subjectId, denominatorExclusion, evaluationResult);
        }
        if (denominatorException != null) {
            denominatorException =
                    evaluatePopulationMembership(subjectType, subjectId, denominatorException, evaluationResult);
        }
        if (numeratorExclusion != null) {
            numeratorExclusion =
                    evaluatePopulationMembership(subjectType, subjectId, numeratorExclusion, evaluationResult);
        }
        // Apply Exclusions and Exceptions
        if (groupDef.isBooleanBasis()) {
            // Remove Subject and Resource Exclusions
            if (denominatorExclusion != null && applyScoring) {
                // numerator should not include den-exclusions
                numerator.removeAllSubjects(denominatorExclusion);

                // verify exclusion results are found in denominator
                denominatorExclusion.retainAllResources(subjectId, denominator);
                denominatorExclusion.retainAllSubjects(denominator);
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify results are in Numerator
                numeratorExclusion.retainAllResources(subjectId, numerator);
                numeratorExclusion.retainAllSubjects(numerator);
            }
            if (denominatorException != null && applyScoring) {
                // Remove Subjects Exceptions that are present in Numerator
                denominatorException.removeAllSubjects(numerator);
                denominatorException.removeAllResources(subjectId, numerator);

                // verify exception results are found in denominator
                denominatorException.retainAllResources(subjectId, denominator);
                denominatorException.retainAllSubjects(denominator);
            }
        } else {
            // Remove Only Resource Exclusions
            // * Multiple resources can be from one subject and represented in multiple populations
            // * This is why we only remove resources and not subjects too for `Resource Basis`.
            if (denominatorExclusion != null && applyScoring) {
                // remove any denominator-exclusion subjects/resources found in Numerator
                numerator.removeAllResources(subjectId, denominatorExclusion);
                // verify exclusion results are found in denominator
                denominatorExclusion.retainAllResources(subjectId, denominator);
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify exclusion results are found in numerator results, otherwise remove
                numeratorExclusion.retainAllResources(subjectId, numerator);
            }
            if (denominatorException != null && applyScoring) {
                // Remove Resource Exceptions that are present in Numerator
                denominatorException.removeAllResources(subjectId, numerator);
                // verify exception results are found in denominator
                denominatorException.retainAllResources(subjectId, denominator);
            }
        }
        if (reportType.equals(MeasureReportType.INDIVIDUAL) && dateOfCompliance != null) {
            var doc = evaluateDateOfCompliance(dateOfCompliance, evaluationResult);
            dateOfCompliance.addResource(subjectId, doc);
        }
        // Ratio Cont Variable Scoring
        if (observationNum != null && observationDen != null) {
            // Num alignment
            var expressionNameNum = getCriteriaExpressionName(observationNum);
            // populate Measure observation function results
            evaluatePopulationMembership(subjectType, subjectId, observationNum, evaluationResult, expressionNameNum);
            // Align to Numerator
            retainObservationResourcesInPopulation(subjectId, numerator, observationNum);
            retainObservationSubjectResourcesInPopulation(
                    numerator.subjectResources, observationNum.getSubjectResources());
            // remove Numerator Exclusions
            if (numeratorExclusion != null) {
                removeObservationSubjectResourcesInPopulation(
                        numeratorExclusion.subjectResources, observationNum.subjectResources);
                removeObservationResourcesInPopulation(subjectId, numeratorExclusion, observationNum);
            }
            // Den alignment
            var expressionNameDen = getCriteriaExpressionName(observationDen);
            // populate Measure observation function results
            evaluatePopulationMembership(subjectType, subjectId, observationDen, evaluationResult, expressionNameDen);
            // align to Denominator Results
            retainObservationResourcesInPopulation(subjectId, denominator, observationDen);
            retainObservationSubjectResourcesInPopulation(
                    denominator.subjectResources, observationDen.getSubjectResources());
            // remove Denominator Exclusions
            if (denominatorExclusion != null) {
                removeObservationSubjectResourcesInPopulation(
                        denominatorExclusion.subjectResources, observationDen.subjectResources);
                removeObservationResourcesInPopulation(subjectId, denominatorExclusion, observationDen);
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
            boolean applyScoring) {
        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
        PopulationDef measurePopulationExclusion = groupDef.getSingle(MEASUREPOPULATIONEXCLUSION);
        PopulationDef measurePopulationObservation = groupDef.getSingle(MEASUREOBSERVATION);
        // Validate Required Populations are Present
        R4MeasureScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationDef::type).toList(), MeasureScoring.CONTINUOUSVARIABLE);

        initialPopulation = evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult);
        measurePopulation = evaluatePopulationMembership(subjectType, subjectId, measurePopulation, evaluationResult);
        // Evaluate Population Expressions
        measurePopulation = evaluatePopulationMembership(subjectType, subjectId, measurePopulation, evaluationResult);
        if (measurePopulation != null && initialPopulation != null && applyScoring) {
            // verify initial-population are in measure-population
            measurePopulation.retainAllResources(subjectId, initialPopulation);
            measurePopulation.retainAllSubjects(initialPopulation);
        }

        if (measurePopulationExclusion != null) {
            evaluatePopulationMembership(
                    subjectType, subjectId, groupDef.getSingle(MEASUREPOPULATIONEXCLUSION), evaluationResult);
            if (applyScoring && measurePopulation != null) {
                // verify exclusions are in measure-population
                measurePopulationExclusion.retainAllResources(subjectId, measurePopulation);
                measurePopulationExclusion.retainAllSubjects(measurePopulation);
            }
        }
        if (measurePopulationObservation != null) {
            // only Measure Population resources need to be removed
            var expressionName = measurePopulationObservation.getCriteriaReference() + "-"
                    + measurePopulationObservation.expression();
            // assumes only one population
            evaluatePopulationMembership(
                    subjectType, subjectId, groupDef.getSingle(MEASUREOBSERVATION), evaluationResult, expressionName);
            if (applyScoring && measurePopulation != null) {
                // only measureObservations that intersect with measureObservation should be retained
                retainObservationResourcesInPopulation(subjectId, measurePopulation, measurePopulationObservation);
                retainObservationSubjectResourcesInPopulation(
                        measurePopulation.subjectResources, measurePopulationObservation.getSubjectResources());
                // measure observations also need to make sure they remove measure-population-exclusions
                if (measurePopulationExclusion != null) {
                    removeObservationResourcesInPopulation(
                            subjectId, measurePopulationExclusion, measurePopulationObservation);
                    removeObservationSubjectResourcesInPopulation(
                            measurePopulationExclusion.subjectResources,
                            measurePopulationObservation.getSubjectResources());
                }
            }
        }
    }
    /**
     * Keeps Measure-Observation values found in measurePopulation
     * are not found in the corresponding measurePopulation set.
     */
    @SuppressWarnings("unchecked")
    public void retainObservationSubjectResourcesInPopulation(
            Map<String, Set<Object>> measurePopulation, Map<String, Set<Object>> measureObservation) {

        if (measurePopulation == null || measureObservation == null) {
            return;
        }

        for (Iterator<Map.Entry<String, Set<Object>>> it =
                        measureObservation.entrySet().iterator();
                it.hasNext(); ) {
            Map.Entry<String, Set<Object>> entry = it.next();
            String subjectId = entry.getKey();

            // Cast subject's observation set to the expected type
            Set<Map<Object, Object>> obsSet = (Set<Map<Object, Object>>) (Set<?>) entry.getValue();

            // get valid population values for this subject
            Set<Object> validPopulation = measurePopulation.get(subjectId);

            if (validPopulation == null || validPopulation.isEmpty()) {
                // no population for this subject -> drop the whole subject
                it.remove();
                continue;
            }

            // remove observations not matching population values
            obsSet.removeIf(obsMap -> {
                for (Object key : obsMap.keySet()) {
                    if (!validPopulation.contains(key)) {
                        return true; // remove this observation map
                    }
                }
                return false;
            });

            // if no observations remain for this subject, remove it entirely
            if (obsSet.isEmpty()) {
                it.remove();
            }
        }
    }

    protected void retainObservationResourcesInPopulation(
            String subjectId,
            //        MeasurePopulationType.MEASUREPOPULATION
            PopulationDef measurePopulationDef,
            //        MeasurePopulationType.MEASUREOBSERVATION
            PopulationDef measureObservationDef) {
        for (Object populationResource : measureObservationDef.getResourcesForSubject(subjectId)) {
            if (populationResource instanceof Map<?, ?> measureObservationResourceAsMap) {
                for (Entry<?, ?> measureObservationResourceMapEntry : measureObservationResourceAsMap.entrySet()) {
                    final Object measureObservationSubjectResourceMapKey = measureObservationResourceMapEntry.getKey();
                    if (measurePopulationDef != null) {
                        final Set<Object> measurePopulationResourcesForSubject =
                                measurePopulationDef.getResourcesForSubject(subjectId);
                        if (!measurePopulationResourcesForSubject.contains(measureObservationSubjectResourceMapKey)) {
                            // remove observation results not found in measure population
                            measureObservationDef
                                    .getResourcesForSubject(subjectId)
                                    .remove(populationResource);
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param measurePopulation population results that you would like to exclude from measureObservation
     * @param measureObservation population results that will have items excluded from it, if found in measurePopulation
     */
    @SuppressWarnings("unchecked")
    public void removeObservationSubjectResourcesInPopulation(
            Map<String, Set<Object>> measurePopulation, Map<String, Set<Object>> measureObservation) {

        if (measurePopulation == null || measureObservation == null) {
            return;
        }

        for (Iterator<Map.Entry<String, Set<Object>>> it =
                        measureObservation.entrySet().iterator();
                it.hasNext(); ) {

            Map.Entry<String, Set<Object>> entry = it.next();
            String subjectId = entry.getKey();

            final Set<?> entryValue = entry.getValue();

            if (CollectionUtils.isEmpty(entryValue)) {
                continue;
            }

            final Object firstEntryValue = entryValue.iterator().next();

            if (!(firstEntryValue instanceof Map<?, ?>)) {
                throw new InternalErrorException("Expected a Map<?,?> but was not: %s".formatted(firstEntryValue));
            }

            Set<Map<Object, Object>> obsSet = (Set<Map<Object, Object>>) entryValue;

            // population values for this subject
            Set<Object> populationValues = measurePopulation.get(subjectId);

            // If there is no population for this subject, there is nothing "to remove because it matches",
            // so leave the observation set as-is.
            if (populationValues == null || populationValues.isEmpty()) {
                continue;
            }

            // Remove observations that *do* match population values
            obsSet.removeIf(obsMap -> {
                for (Object key : obsMap.keySet()) {
                    if (populationValues.contains(key)) {
                        // This observation map is backed by a population resource -> remove it
                        return true;
                    }
                }
                return false;
            });

            // If no observations remain for this subject, remove the subject entry entirely
            if (obsSet.isEmpty()) {
                it.remove();
            }
        }
    }

    /**
     * Removes measureObservationDef resources for a subject when their "key" is
     * found in the corresponding measurePopulationDef resources for that subject.
     *
     * In other words: delete observation results that are ALSO present in the
     * population resources.
     */
    protected void removeObservationResourcesInPopulation(
            String subjectId,
            // MeasurePopulationType.MEASUREPOPULATIONEXCLUSION
            PopulationDef measurePopulationDef,
            // MeasurePopulationType.MEASUREOBSERVATION
            PopulationDef measureObservationDef) {

        if (measureObservationDef == null || measurePopulationDef == null) {
            return;
        }

        // Population keys to match against
        final Set<Object> measurePopulationResourcesForSubject = measurePopulationDef.getResourcesForSubject(subjectId);

        if (measurePopulationResourcesForSubject == null || measurePopulationResourcesForSubject.isEmpty()) {
            // nothing to compare against -> nothing to remove
            return;
        }

        // Work on a copy to avoid concurrent modification issues while removing
        HashSetForFhirResourcesAndCqlTypes<Object> observationResources =
                new HashSetForFhirResourcesAndCqlTypes<>(measureObservationDef.getResourcesForSubject(subjectId));

        for (Object populationResource : observationResources) {

            if (!(populationResource instanceof Map<?, ?> measureObservationResourceAsMap)) {
                continue;
            }

            // process this single populationResource
            processSingleResource(
                    populationResource,
                    measureObservationResourceAsMap,
                    measurePopulationResourcesForSubject,
                    measureObservationDef,
                    subjectId);
        }
    }

    private void processSingleResource(
            Object populationResource,
            Map<?, ?> measureObservationResourceAsMap,
            Set<Object> measurePopulationResourcesForSubject,
            PopulationDef measureObservationDef,
            String subjectId) {

        for (Map.Entry<?, ?> entry : measureObservationResourceAsMap.entrySet()) {
            Object key = entry.getKey();

            // If the key is present in the population resources → remove this item
            if (measurePopulationResourcesForSubject.contains(key)) {
                measureObservationDef.getResourcesForSubject(subjectId).remove(populationResource);

                // short-circuits this resource entirely
                return;
            }
        }
    }

    protected void evaluateCohort(
            GroupDef groupDef, String subjectType, String subjectId, EvaluationResult evaluationResult) {
        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        // Validate Required Populations are Present
        R4MeasureScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationDef::type).toList(), MeasureScoring.COHORT);
        // Evaluate Population
        evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult);
    }

    protected void evaluateGroup(
            MeasureDef measureDef,
            GroupDef groupDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring) {

        populationBasisValidator.validateGroupPopulations(measureDef, groupDef, evaluationResult);
        populationBasisValidator.validateStratifiers(measureDef, groupDef, evaluationResult);

        evaluateStratifiers(subjectId, groupDef.stratifiers(), evaluationResult);

        var scoring = groupDef.measureScoring();
        switch (scoring) {
            case PROPORTION, RATIO:
                evaluateProportion(groupDef, subjectType, subjectId, reportType, evaluationResult, applyScoring);
                break;
            case CONTINUOUSVARIABLE:
                evaluateContinuousVariable(groupDef, subjectType, subjectId, evaluationResult, applyScoring);
                break;
            case COHORT:
                evaluateCohort(groupDef, subjectType, subjectId, evaluationResult);
                break;
        }
    }

    protected Object evaluateDateOfCompliance(PopulationDef populationDef, EvaluationResult evaluationResult) {
        return evaluationResult.forExpression(populationDef.expression()).value();
    }

    protected void evaluateSdes(String subjectId, List<SdeDef> sdes, EvaluationResult evaluationResult) {
        for (SdeDef sde : sdes) {
            var expressionResult = evaluationResult.forExpression(sde.expression());
            Object result = expressionResult.value();
            // TODO: This is a hack-around for an cql engine bug. Need to investigate.
            if ((result instanceof List<?> list) && (list.size() == 1) && list.get(0) == null) {
                result = null;
            }

            sde.putResult(subjectId, result, expressionResult.evaluatedResources());
        }
    }

    protected void evaluateStratifiers(
            String subjectId, List<StratifierDef> stratifierDefs, EvaluationResult evaluationResult) {
        for (StratifierDef stratifierDef : stratifierDefs) {

            evaluateStratifier(subjectId, evaluationResult, stratifierDef);
        }
    }

    private void evaluateStratifier(String subjectId, EvaluationResult evaluationResult, StratifierDef stratifierDef) {
        if (stratifierDef.isComponentStratifier()) {
            addStratifierComponentResult(stratifierDef.components(), evaluationResult, subjectId);
        } else {
            addStratifierNonComponentResult(subjectId, evaluationResult, stratifierDef);
        }
    }

    /**
     * Modified by Claude: Changed visibility from private to package-private for testability.
     * Replaced Optional.ofNullable() pattern with explicit null checks and added logger.warn()
     * for better observability when stratifier component expressions return null.
     */
    void addStratifierComponentResult(
            List<StratifierComponentDef> components, EvaluationResult evaluationResult, String subjectId) {

        for (StratifierComponentDef component : components) {
            var expressionResult = evaluationResult.forExpression(component.expression());

            if (expressionResult == null || expressionResult.value() == null) {
                logger.warn(
                        "Stratifier component expression '{}' returned null result for subject '{}'",
                        component.expression(),
                        subjectId);
                continue;
            }

            component.putResult(subjectId, expressionResult.value(), expressionResult.evaluatedResources());
        }
    }

    /**
     * Modified by Claude: Changed visibility from private to package-private for testability.
     * Replaced Optional.ofNullable() pattern with explicit null checks and added logger.warn()
     * for better observability when stratifier expressions return null.
     */
    void addStratifierNonComponentResult(
            String subjectId, EvaluationResult evaluationResult, StratifierDef stratifierDef) {

        var expressionResult = evaluationResult.forExpression(stratifierDef.expression());

        if (expressionResult == null || expressionResult.value() == null) {
            logger.warn(
                    "Stratifier expression '{}' returned null result for subject '{}'",
                    stratifierDef.expression(),
                    subjectId);
            return;
        }

        stratifierDef.putResult(subjectId, expressionResult.value(), expressionResult.evaluatedResources());
    }

    /**
     * Calculates and stores measure scores for all groups and stratifiers.
     * Added by Claude Sonnet 4.5 - version-agnostic score calculation.
     *
     * <p>This method calculates scores after population evaluation is complete,
     * storing results in GroupDef and StratumDef objects. The FHIR-specific
     * scorer classes can then read these pre-calculated scores and map them
     * to the appropriate FHIR structures.
     *
     * @param measureDef the measure definition containing groups and populations
     */
    public void calculateScores(MeasureDef measureDef) {
        for (GroupDef groupDef : measureDef.groups()) {
            calculateGroupScore(groupDef);

            for (StratifierDef stratifierDef : groupDef.stratifiers()) {
                for (StratumDef stratumDef : stratifierDef.getStratum()) {
                    calculateStratumScore(groupDef, stratifierDef, stratumDef);
                }
            }
        }
    }

    /**
     * Calculates the score for a single group based on its scoring type.
     * Added by Claude Sonnet 4.5 - version-agnostic group score calculation.
     *
     * @param groupDef the group definition to calculate score for
     */
    private void calculateGroupScore(GroupDef groupDef) {
        MeasureScoring scoring = groupDef.measureScoring();
        if (scoring == null) {
            return;
        }

        switch (scoring) {
            case PROPORTION, RATIO -> {
                if (scoring == MeasureScoring.RATIO
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    // Ratio continuous variable - complex calculation
                    calculateRatioContVariableGroupScore(groupDef);
                } else {
                    // Standard proportion/ratio
                    int numerator =
                            getPopulationCount(groupDef, NUMERATOR) - getPopulationCount(groupDef, NUMERATOREXCLUSION);
                    int denominator = getPopulationCount(groupDef, DENOMINATOR)
                            - getPopulationCount(groupDef, DENOMINATOREXCLUSION)
                            - getPopulationCount(groupDef, DENOMINATOREXCEPTION);

                    Double score = calcProportionScore(numerator, denominator);
                    groupDef.setMeasureScore(score);
                }
            }
            case CONTINUOUSVARIABLE -> {
                calculateContinuousVariableGroupScore(groupDef);
            }
            default -> {
                // COHORT doesn't have scoring
            }
        }
    }

    /**
     * Calculates proportion score (numerator / denominator).
     * Added by Claude Sonnet 4.5.
     *
     * @param numeratorCount the numerator count
     * @param denominatorCount the denominator count
     * @return the calculated score, or null if denominator is 0
     */
    private Double calcProportionScore(Integer numeratorCount, Integer denominatorCount) {
        if (numeratorCount == null) {
            numeratorCount = 0;
        }
        if (denominatorCount != null && denominatorCount != 0) {
            return numeratorCount / (double) denominatorCount;
        }
        return null;
    }

    /**
     * Gets the count for a population type in a group.
     * Added by Claude Sonnet 4.5.
     *
     * @param groupDef the group definition
     * @param type the population type
     * @return the population count
     */
    private int getPopulationCount(GroupDef groupDef, MeasurePopulationType type) {
        PopulationDef popDef = groupDef.getSingle(type);
        if (popDef == null) {
            return 0;
        }
        return popDef.getCountForScoring();
    }

    /**
     * Calculates continuous variable score for a group.
     * Added by Claude Sonnet 4.5.
     *
     * @param groupDef the group definition
     */
    private void calculateContinuousVariableGroupScore(GroupDef groupDef) {
        PopulationDef measurePop = groupDef.getSingle(MEASUREPOPULATION);
        if (measurePop == null) {
            return;
        }

        List<PopulationDef> measureObservations = groupDef.get(MEASUREOBSERVATION);
        if (measureObservations.isEmpty()) {
            return;
        }

        PopulationDef measureObservation = measureObservations.get(0);
        ContinuousVariableObservationAggregateMethod aggregateMethod = measureObservation.getAggregateMethod();

        if (aggregateMethod == null) {
            return;
        }

        // Extract numeric values from observations
        List<Double> values = extractNumericValues(measureObservation.getAllSubjectResources());

        // Aggregate the values
        Double score = aggregateValues(values, aggregateMethod);
        groupDef.setMeasureScore(score);
    }

    /**
     * Calculates ratio continuous variable score for a group.
     * Added by Claude Sonnet 4.5.
     *
     * @param groupDef the group definition
     */
    private void calculateRatioContVariableGroupScore(GroupDef groupDef) {
        List<PopulationDef> measureObservations = groupDef.get(MEASUREOBSERVATION);
        if (measureObservations.size() < 2) {
            return;
        }

        PopulationDef numeratorObs = measureObservations.get(0);
        PopulationDef denominatorObs = measureObservations.get(1);

        List<Double> numValues = extractNumericValues(numeratorObs.getAllSubjectResources());
        List<Double> denValues = extractNumericValues(denominatorObs.getAllSubjectResources());

        ContinuousVariableObservationAggregateMethod numMethod = numeratorObs.getAggregateMethod();
        ContinuousVariableObservationAggregateMethod denMethod = denominatorObs.getAggregateMethod();

        if (numMethod == null || denMethod == null) {
            return;
        }

        Double numScore = aggregateValues(numValues, numMethod);
        Double denScore = aggregateValues(denValues, denMethod);

        if (numScore != null && denScore != null && denScore != 0.0) {
            groupDef.setMeasureScore(numScore / denScore);
        }
    }

    /**
     * Calculates the score for a single stratum.
     * Added by Claude Sonnet 4.5.
     *
     * @param groupDef the group definition
     * @param stratifierDef the stratifier definition
     * @param stratumDef the stratum definition to calculate score for
     */
    private void calculateStratumScore(GroupDef groupDef, StratifierDef stratifierDef, StratumDef stratumDef) {
        MeasureScoring scoring = groupDef.measureScoring();
        if (scoring == null) {
            return;
        }

        switch (scoring) {
            case PROPORTION, RATIO -> {
                if (scoring == MeasureScoring.RATIO
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    // Ratio continuous variable for stratum
                    calculateRatioContVariableStratumScore(groupDef, stratumDef);
                } else {
                    // Standard proportion/ratio for stratum
                    int numerator = getStratumPopulationCount(stratumDef, NUMERATOR)
                            - getStratumPopulationCount(stratumDef, NUMERATOREXCLUSION);
                    int denominator = getStratumPopulationCount(stratumDef, DENOMINATOR)
                            - getStratumPopulationCount(stratumDef, DENOMINATOREXCLUSION)
                            - getStratumPopulationCount(stratumDef, DENOMINATOREXCEPTION);

                    Double score = calcProportionScore(numerator, denominator);
                    stratumDef.setMeasureScore(score);
                }
            }
            case CONTINUOUSVARIABLE -> {
                calculateContinuousVariableStratumScore(groupDef, stratumDef);
            }
            default -> {
                // COHORT doesn't have scoring
            }
        }
    }

    /**
     * Gets the count for a population type in a stratum.
     * Added by Claude Sonnet 4.5.
     *
     * @param stratumDef the stratum definition
     * @param type the population type
     * @return the population count
     */
    private int getStratumPopulationCount(StratumDef stratumDef, MeasurePopulationType type) {
        for (StratumPopulationDef stratumPopDef : stratumDef.stratumPopulations()) {
            // Match by comparing the population type code
            if (stratumPopDef.id().contains(type.toCode())) {
                return stratumPopDef.getCount();
            }
        }
        return 0;
    }

    /**
     * Calculates continuous variable score for a stratum.
     * Added by Claude Sonnet 4.5.
     *
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     */
    private void calculateContinuousVariableStratumScore(GroupDef groupDef, StratumDef stratumDef) {
        List<PopulationDef> measureObservations = groupDef.get(MEASUREOBSERVATION);
        if (measureObservations.isEmpty()) {
            return;
        }

        PopulationDef measureObservation = measureObservations.get(0);
        ContinuousVariableObservationAggregateMethod aggregateMethod = measureObservation.getAggregateMethod();

        if (aggregateMethod == null) {
            return;
        }

        // Get the stratum population for measure observation
        StratumPopulationDef stratumPopDef = findStratumPopulation(stratumDef, MEASUREOBSERVATION);
        if (stratumPopDef == null) {
            return;
        }

        // Extract values from stratum resources
        List<Double> values = extractNumericValuesFromStratumResources(stratumPopDef);

        Double score = aggregateValues(values, aggregateMethod);
        stratumDef.setMeasureScore(score);
    }

    /**
     * Calculates ratio continuous variable score for a stratum.
     * Added by Claude Sonnet 4.5.
     *
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     */
    private void calculateRatioContVariableStratumScore(GroupDef groupDef, StratumDef stratumDef) {
        List<PopulationDef> measureObservations = groupDef.get(MEASUREOBSERVATION);
        if (measureObservations.size() < 2) {
            return;
        }

        // Find stratum populations for numerator and denominator observations
        StratumPopulationDef numStratumPop = stratumDef.stratumPopulations().stream()
                .filter(sp -> sp.id().equals(measureObservations.get(0).id()))
                .findFirst()
                .orElse(null);

        StratumPopulationDef denStratumPop = stratumDef.stratumPopulations().stream()
                .filter(sp -> sp.id().equals(measureObservations.get(1).id()))
                .findFirst()
                .orElse(null);

        if (numStratumPop == null || denStratumPop == null) {
            return;
        }

        List<Double> numValues = extractNumericValuesFromStratumResources(numStratumPop);
        List<Double> denValues = extractNumericValuesFromStratumResources(denStratumPop);

        ContinuousVariableObservationAggregateMethod numMethod =
                measureObservations.get(0).getAggregateMethod();
        ContinuousVariableObservationAggregateMethod denMethod =
                measureObservations.get(1).getAggregateMethod();

        if (numMethod == null || denMethod == null) {
            return;
        }

        Double numScore = aggregateValues(numValues, numMethod);
        Double denScore = aggregateValues(denValues, denMethod);

        if (numScore != null && denScore != null && denScore != 0.0) {
            stratumDef.setMeasureScore(numScore / denScore);
        }
    }

    /**
     * Finds a stratum population by type.
     * Added by Claude Sonnet 4.5.
     *
     * @param stratumDef the stratum definition
     * @param type the population type
     * @return the matching stratum population, or null if not found
     */
    @Nullable
    private StratumPopulationDef findStratumPopulation(StratumDef stratumDef, MeasurePopulationType type) {
        for (StratumPopulationDef stratumPopDef : stratumDef.stratumPopulations()) {
            if (stratumPopDef.id().contains(type.toCode())) {
                return stratumPopDef;
            }
        }
        return null;
    }

    /**
     * Extracts numeric values from a collection of resources.
     * Added by Claude Sonnet 4.5.
     *
     * <p>This method handles the extraction of numeric values from various resource types,
     * particularly for continuous variable observations which may be stored as Map entries.
     *
     * @param resources the collection of resources
     * @return list of extracted numeric values
     */
    private List<Double> extractNumericValues(List<Object> resources) {
        return resources.stream()
                .filter(Map.class::isInstance)
                .map(r -> (Map<?, ?>) r)
                .flatMap(map -> map.values().stream())
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).doubleValue())
                .toList();
    }

    /**
     * Extracts numeric values from stratum population resources.
     * Added by Claude Sonnet 4.5.
     *
     * @param stratumPopDef the stratum population definition
     * @return list of extracted numeric values
     */
    private List<Double> extractNumericValuesFromStratumResources(StratumPopulationDef stratumPopDef) {
        // The resources in stratum population are stored differently
        // This is a simplified extraction - may need refinement based on actual data structure
        return List.of(); // Placeholder - needs proper implementation based on actual data structure
    }

    /**
     * Aggregates numeric values using the specified method.
     * Added by Claude Sonnet 4.5.
     *
     * @param values the values to aggregate
     * @param method the aggregation method
     * @return the aggregated result, or null if values is empty
     */
    @Nullable
    private Double aggregateValues(List<Double> values, ContinuousVariableObservationAggregateMethod method) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        if (ContinuousVariableObservationAggregateMethod.N_A == method) {
            throw new InvalidRequestException(
                    "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
        }

        return switch (method) {
            case SUM -> values.stream().mapToDouble(Double::doubleValue).sum();
            case MAX -> values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
            case MIN -> values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
            case AVG -> values.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(Double.NaN);
            case COUNT -> (double) values.size();
            case MEDIAN -> {
                List<Double> sorted = values.stream().sorted().toList();
                int n = sorted.size();
                if (n % 2 == 1) {
                    yield sorted.get(n / 2);
                } else {
                    yield (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
                }
            }
            default -> throw new IllegalArgumentException("Unsupported aggregation method: " + method);
        };
    }
}
