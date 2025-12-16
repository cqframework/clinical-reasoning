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
import org.opencds.cqf.fhir.cr.measure.common.def.report.GroupReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.PopulationReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.SdeReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratifierComponentReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratifierReportDef;
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

    public void evaluate(
            MeasureReportDef measureReportDef,
            MeasureEvalType measureEvalType,
            String subjectType,
            String subjectId,
            EvaluationResult evaluationResult,
            boolean applyScoring) {
        Objects.requireNonNull(measureReportDef, "measureReportDef is a required argument");
        Objects.requireNonNull(subjectId, "subjectIds is a required argument");

        MeasureReportType reportType =
                switch (measureEvalType) {
                    case PATIENT, SUBJECT -> MeasureReportType.INDIVIDUAL;
                    case SUBJECTLIST -> MeasureReportType.SUBJECTLIST;
                    case PATIENTLIST -> MeasureReportType.PATIENTLIST; // DSTU3 Only
                    case POPULATION -> MeasureReportType.SUMMARY;
                };

        this.evaluateSubject(measureReportDef, subjectType, subjectId, reportType, evaluationResult, applyScoring);
    }

    protected void evaluateSubject(
            MeasureReportDef measureReportDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring) {
        // Mutate the existing MeasureReportDef by adding this subject's results

        // Evaluate SDEs and populate report
        evaluateSdes(subjectId, measureReportDef.sdes(), evaluationResult);

        // Evaluate each group and populate report
        for (GroupReportDef groupReportDef : measureReportDef.groups()) {
            evaluateGroup(
                    measureReportDef,
                    groupReportDef,
                    subjectType,
                    subjectId,
                    reportType,
                    evaluationResult,
                    applyScoring);
        }
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

    protected PopulationReportDef evaluatePopulationMembership(
            String subjectType, String subjectId, PopulationReportDef inclusionDef, EvaluationResult evaluationResult) {
        return evaluatePopulationMembership(subjectType, subjectId, inclusionDef, evaluationResult, null);
    }

    protected PopulationReportDef evaluatePopulationMembership(
            String subjectType,
            String subjectId,
            PopulationReportDef inclusionDef,
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
    private PopulationReportDef getPopulationDefByCriteriaRef(
            GroupReportDef groupDef, MeasurePopulationType populationType, PopulationReportDef inclusionDef) {
        return groupDef.getPopulationDefs(populationType).stream()
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
    protected void validateRatioContinuousVariable(GroupReportDef groupDef) {
        // must have 2 MeasureObservations defined
        if (!groupDef.getPopulationDefs(MEASUREOBSERVATION).isEmpty()
                && groupDef.getPopulationDefs(MEASUREOBSERVATION).size() != 2) {
            throw new InvalidRequestException(
                    "Ratio Continuous Variable requires 2 Measure Observations defined, you have: %s"
                            .formatted(groupDef.getPopulationDefs(MEASUREOBSERVATION)
                                    .size()));
        }
    }

    protected void evaluateProportion(
            GroupReportDef groupDef,
            String subjectType,
            String subjectId,
            MeasureReportType reportType,
            EvaluationResult evaluationResult,
            boolean applyScoring) {
        // check populations
        R4MeasureScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationReportDef::type).toList(), groupDef.measureScoring());

        PopulationReportDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationReportDef numerator = groupDef.getSingle(NUMERATOR);
        PopulationReportDef denominator = groupDef.getSingle(DENOMINATOR);
        PopulationReportDef denominatorExclusion = groupDef.getSingle(DENOMINATOREXCLUSION);
        PopulationReportDef denominatorException = groupDef.getSingle(DENOMINATOREXCEPTION);
        PopulationReportDef numeratorExclusion = groupDef.getSingle(NUMERATOREXCLUSION);
        PopulationReportDef dateOfCompliance = groupDef.getSingle(DATEOFCOMPLIANCE);
        // Ratio Continuous Variable ONLY

        PopulationReportDef observationNum = getPopulationDefByCriteriaRef(groupDef, MEASUREOBSERVATION, numerator);
        PopulationReportDef observationDen = getPopulationDefByCriteriaRef(groupDef, MEASUREOBSERVATION, denominator);
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
                    numerator.getSubjectResources(), observationNum.getSubjectResources());
            // remove Numerator Exclusions
            if (numeratorExclusion != null) {
                removeObservationSubjectResourcesInPopulation(
                        numeratorExclusion.getSubjectResources(), observationNum.getSubjectResources());
                removeObservationResourcesInPopulation(subjectId, numeratorExclusion, observationNum);
            }
            // Den alignment
            var expressionNameDen = getCriteriaExpressionName(observationDen);
            // populate Measure observation function results
            evaluatePopulationMembership(subjectType, subjectId, observationDen, evaluationResult, expressionNameDen);
            // align to Denominator Results
            retainObservationResourcesInPopulation(subjectId, denominator, observationDen);
            retainObservationSubjectResourcesInPopulation(
                    denominator.getSubjectResources(), observationDen.getSubjectResources());
            // remove Denominator Exclusions
            if (denominatorExclusion != null) {
                removeObservationSubjectResourcesInPopulation(
                        denominatorExclusion.getSubjectResources(), observationDen.getSubjectResources());
                removeObservationResourcesInPopulation(subjectId, denominatorExclusion, observationDen);
            }
        }
    }

    protected String getCriteriaExpressionName(PopulationReportDef populationDef) {
        return populationDef.getCriteriaReference() + "-" + populationDef.expression();
    }

    protected void evaluateContinuousVariable(
            GroupReportDef groupDef,
            String subjectType,
            String subjectId,
            EvaluationResult evaluationResult,
            boolean applyScoring) {
        PopulationReportDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationReportDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
        PopulationReportDef measurePopulationExclusion = groupDef.getSingle(MEASUREPOPULATIONEXCLUSION);
        PopulationReportDef measurePopulationObservation = groupDef.getSingle(MEASUREOBSERVATION);
        // Validate Required Populations are Present
        R4MeasureScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationReportDef::type).toList(),
                MeasureScoring.CONTINUOUSVARIABLE);

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
                        measurePopulation.getSubjectResources(), measurePopulationObservation.getSubjectResources());
                // measure observations also need to make sure they remove measure-population-exclusions
                if (measurePopulationExclusion != null) {
                    removeObservationResourcesInPopulation(
                            subjectId, measurePopulationExclusion, measurePopulationObservation);
                    removeObservationSubjectResourcesInPopulation(
                            measurePopulationExclusion.getSubjectResources(),
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
            PopulationReportDef measurePopulationDef,
            //        MeasurePopulationType.MEASUREOBSERVATION
            PopulationReportDef measureObservationDef) {
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
            PopulationReportDef measurePopulationDef,
            // MeasurePopulationType.MEASUREOBSERVATION
            PopulationReportDef measureObservationDef) {

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
            PopulationReportDef measureObservationDef,
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
            GroupReportDef groupDef, String subjectType, String subjectId, EvaluationResult evaluationResult) {
        PopulationReportDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        // Validate Required Populations are Present
        R4MeasureScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationReportDef::type).toList(), MeasureScoring.COHORT);
        // Evaluate Population
        evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult);
    }

    protected void evaluateGroup(
            MeasureReportDef measureDef,
            GroupReportDef groupDef,
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

    protected Object evaluateDateOfCompliance(PopulationReportDef populationDef, EvaluationResult evaluationResult) {
        return evaluationResult.forExpression(populationDef.expression()).value();
    }

    protected void evaluateSdes(String subjectId, List<SdeReportDef> sdes, EvaluationResult evaluationResult) {
        for (SdeReportDef sde : sdes) {
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
            String subjectId, List<StratifierReportDef> stratifierDefs, EvaluationResult evaluationResult) {
        for (StratifierReportDef stratifierDef : stratifierDefs) {

            evaluateStratifier(subjectId, evaluationResult, stratifierDef);
        }
    }

    private void evaluateStratifier(
            String subjectId, EvaluationResult evaluationResult, StratifierReportDef stratifierDef) {
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
            List<StratifierComponentReportDef> components, EvaluationResult evaluationResult, String subjectId) {

        for (StratifierComponentReportDef component : components) {
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
            String subjectId, EvaluationResult evaluationResult, StratifierReportDef stratifierDef) {

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
}
