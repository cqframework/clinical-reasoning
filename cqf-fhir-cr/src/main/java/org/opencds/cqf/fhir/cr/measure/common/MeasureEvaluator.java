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

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureScoringTypePopulations;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4ResourceIdUtils;

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

        switch (measureEvalType) {
            case PATIENT, SUBJECT:
                return this.evaluateSubject(
                        measureDef,
                        subjectType,
                        subjectId,
                        MeasureReportType.INDIVIDUAL,
                        evaluationResult,
                        applyScoring);
            case SUBJECTLIST:
                return this.evaluateSubject(
                        measureDef,
                        subjectType,
                        subjectId,
                        MeasureReportType.SUBJECTLIST,
                        evaluationResult,
                        applyScoring);
            case PATIENTLIST:
                // DSTU3 Only
                return this.evaluateSubject(
                        measureDef,
                        subjectType,
                        subjectId,
                        MeasureReportType.PATIENTLIST,
                        evaluationResult,
                        applyScoring);
            case POPULATION:
                return this.evaluateSubject(
                        measureDef, subjectType, subjectId, MeasureReportType.SUMMARY, evaluationResult, applyScoring);
            default:
                // never hit because this value is set upstream
                throw new InvalidRequestException("Unsupported Measure Evaluation type: %s for MeasureDef: %s"
                        .formatted(measureEvalType.getDisplay(), measureDef.url()));
        }
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

        // Retrieve intersection of populations and results
        // add resources
        // add subject

        // Evaluate Population Expressions
        initialPopulation = evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult);
        denominator = evaluatePopulationMembership(subjectType, subjectId, denominator, evaluationResult);
        numerator = evaluatePopulationMembership(subjectType, subjectId, numerator, evaluationResult);
        if (applyScoring) {
            // remove denominator values not in IP
            denominator.retainAllResources(initialPopulation.getResources());
            denominator.retainAllSubjects(initialPopulation.getSubjects());
            // remove numerator values if not in Denominator
            numerator.retainAllSubjects(denominator.getSubjects());
            numerator.retainAllResources(denominator.getResources());
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
                numerator.removeAllSubjects(denominatorExclusion.getSubjects());
                numerator.removeOverlaps(denominatorExclusion.getSubjectResources());

                // verify exclusion results are found in denominator
                denominatorExclusion.retainAllResources(denominator.getResources());
                denominatorExclusion.retainAllSubjects(denominator.getSubjects());
                denominatorExclusion.retainOverlaps(denominator.getSubjectResources());
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify results are in Numerator
                numeratorExclusion.retainAllResources(numerator.getResources());
                numeratorExclusion.retainAllSubjects(numerator.getSubjects());
                numeratorExclusion.retainOverlaps(numerator.getSubjectResources());
            }
            if (denominatorException != null && applyScoring) {
                // Remove Subjects Exceptions that are present in Numerator
                denominatorException.removeAllSubjects(numerator.getSubjects());
                denominatorException.removeAllResources(numerator.getResources());
                denominatorException.removeOverlaps(numerator.getSubjectResources());

                // verify exception results are found in denominator
                denominatorException.retainAllResources(denominator.getResources());
                denominatorException.retainAllSubjects(denominator.getSubjects());
                denominatorException.retainOverlaps(denominator.getSubjectResources());
            }
        } else {
            // Remove Only Resource Exclusions
            // * Multiple resources can be from one subject and represented in multiple populations
            // * This is why we only remove resources and not subjects too for `Resource Basis`.
            if (denominatorExclusion != null && applyScoring) {
                // remove any denominator-exception subjects/resources found in Numerator
                numerator.removeAllResources(denominatorExclusion.getResources());
                numerator.removeOverlaps(denominatorExclusion.getSubjectResources());
                // verify exclusion results are found in denominator
                denominatorExclusion.retainAllResources(denominator.getResources());
                denominatorExclusion.retainOverlaps(denominator.getSubjectResources());
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify exclusion results are found in numerator results, otherwise remove
                numeratorExclusion.retainAllResources(numerator.getResources());
                numeratorExclusion.retainOverlaps(numerator.getSubjectResources());
            }
            if (denominatorException != null && applyScoring) {
                // Remove Resource Exceptions that are present in Numerator
                denominatorException.removeAllResources(numerator.getResources());
                denominatorException.removeOverlaps(numerator.getSubjectResources());
                // verify exception results are found in denominator
                denominatorException.retainAllResources(denominator.getResources());
                denominatorException.retainOverlaps(denominator.getSubjectResources());
            }
        }
        if (reportType.equals(MeasureReportType.INDIVIDUAL) && dateOfCompliance != null) {
            var doc = evaluateDateOfCompliance(dateOfCompliance, evaluationResult);
            dateOfCompliance.addResource(subjectId, doc);
        }
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
            measurePopulation.retainAllResources(initialPopulation.getResources());
            measurePopulation.retainAllSubjects(initialPopulation.getSubjects());
        }

        if (measurePopulationExclusion != null) {
            evaluatePopulationMembership(
                    subjectType, subjectId, groupDef.getSingle(MEASUREPOPULATIONEXCLUSION), evaluationResult);
            if (applyScoring && measurePopulation != null) {
                // verify exclusions are in measure-population
                measurePopulationExclusion.retainAllResources(measurePopulation.getResources());
                measurePopulationExclusion.retainAllSubjects(measurePopulation.getSubjects());
            }
        }
        if (measurePopulationObservation != null) {
            // only Measure Population resources need to be removed
            var expressionName = measurePopulationObservation.getCriteriaReference() + "-"
                    + measurePopulationObservation.expression();
            // assumes only one population
            evaluatePopulationMembership(
                    subjectType, subjectId, groupDef.getSingle(MEASUREOBSERVATION), evaluationResult, expressionName);
            if (applyScoring) {
                // only measureObservations that intersect with finalized measure-population results should be retained
                pruneObservationResources(
                        measurePopulationObservation.getResources(), measurePopulation, measurePopulationObservation);
                // what about subjects?
                if (measurePopulation != null) {
                    pruneObservationSubjectResources(
                            measurePopulation.subjectResources, measurePopulationObservation.getSubjectResources());
                }
            }
        }
        // measure Observation
        // source expression result population.id-function-name?
        // retainAll MeasureObservations found in MeasurePopulation

    }
    /**
     * Removes observation entries from measureObservation if their keys
     * are not found in the corresponding measurePopulation set.
     */
    @SuppressWarnings("unchecked")
    public void pruneObservationSubjectResources(
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

    protected void pruneObservationResources(
            Set<Object> resources, PopulationDef measurePopulation, PopulationDef measurePopulationObservation) {
        for (Object resource : resources) {
            if (resource instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    var measurePopResult = entry.getKey();
                    if (measurePopulation != null
                            && !measurePopulation.getResources().contains(measurePopResult)) {
                        // remove observation results not found in measure population
                        measurePopulationObservation.getResources().remove(resource);
                    }
                }
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
        if (!stratifierDef.components().isEmpty()) {
            addStratifierComponentResult(stratifierDef.components(), evaluationResult, subjectId);
        } else {
            addStratifierNonComponentResult(subjectId, evaluationResult, stratifierDef);
        }
    }

    private void addStratifierComponentResult(
            List<StratifierComponentDef> components, EvaluationResult evaluationResult, String subjectId) {

        for (StratifierComponentDef component : components) {
            var expressionResult = evaluationResult.forExpression(component.expression());
            Optional.ofNullable(expressionResult.value())
                    .ifPresent(nonNullValue ->
                            component.putResult(subjectId, nonNullValue, expressionResult.evaluatedResources()));
        }
    }

    private void addStratifierNonComponentResult(
            String subjectId, EvaluationResult evaluationResult, StratifierDef stratifierDef) {

        var expressionResult = evaluationResult.forExpression(stratifierDef.expression());
        Optional.ofNullable(expressionResult)
                .map(ExpressionResult::value)
                .ifPresent(nonNullValue -> stratifierDef.putResult(
                        subjectId, // context of CQL expression ex: Patient based
                        nonNullValue,
                        expressionResult.evaluatedResources()));
    }

    /**
     * Take the accumulated subject-by-subject evaluation results and use it to build StratumDefs
     * and StratumPopulationDefs
     *
     * @param measureDef to mutate post-evaluation with results of initial stratifier
     *                   subject-by-subject accumulations.
     *
     */
    public void postEvaluation(MeasureDef measureDef) {

        for (GroupDef groupDef : measureDef.groups()) {
            for (StratifierDef stratifierDef : groupDef.stratifiers()) {
                final List<StratumDef> stratumDefs;

                if (!stratifierDef.components().isEmpty()) {
                    stratumDefs = componentStratumPlural(stratifierDef, groupDef.populations());
                } else {
                    stratumDefs = nonComponentStratumPlural(stratifierDef, groupDef.populations());
                }

                stratifierDef.addAllStratum(stratumDefs);
            }
        }
    }

    private StratumDef buildStratumDef(
            StratifierDef stratifierDef,
            Set<StratumValueDef> values,
            List<String> subjectIds,
            List<PopulationDef> populationDefs) {

        boolean isComponent = values.size() > 1;
        String stratumText = null;

        for (StratumValueDef valuePair : values) {
            StratumValueWrapper value = valuePair.value();
            var componentDef = valuePair.def();
            // Set Stratum value to indicate which value is displaying results
            // ex. for Gender stratifier, code 'Male'
            if (value.getValueClass().equals(CodeableConcept.class)) {
                if (isComponent) {
                    // component stratifier example: code: "gender", value: 'M'
                    // value being stratified: 'M'
                    stratumText = componentDef.code().text();
                } else {
                    // non-component stratifiers only set stratified value, code is set on stratifier object
                    // value being stratified: 'M'
                    if (value.getValue() instanceof CodeableConcept codeableConcept) {
                        stratumText = codeableConcept.getText();
                    }
                }
            } else if (isComponent) {
                stratumText = expressionResultToCodableConcept(value).getText();
            } else if (MeasureStratifierType.VALUE == stratifierDef.getStratifierType()) {
                // non-component stratifiers only set stratified value, code is set on stratifier object
                // value being stratified: 'M'
                stratumText = expressionResultToCodableConcept(value).getText();
            }
        }

        return new StratumDef(
                stratumText,
                populationDefs.stream()
                        .map(popDef -> buildStratumPopulationDef(popDef, subjectIds))
                        .toList(),
                values,
                subjectIds);
    }

    private static StratumPopulationDef buildStratumPopulationDef(
            PopulationDef populationDef, List<String> subjectIds) {

        var popSubjectIds = populationDef.getSubjects().stream()
                .map(R4ResourceIdUtils::addPatientQualifier)
                .collect(Collectors.toUnmodifiableSet());

        var qualifiedSubjectIdsCommonToPopulation = Sets.intersection(new HashSet<>(subjectIds), popSubjectIds);

        var unqualifiedSubjectIdsCommonToPopulation = qualifiedSubjectIdsCommonToPopulation.stream()
                .filter(Objects::nonNull)
                .map(R4ResourceIdUtils::stripAnyResourceQualifier)
                .collect(Collectors.toUnmodifiableSet());

        return new StratumPopulationDef(populationDef.id(), unqualifiedSubjectIdsCommonToPopulation);
    }

    private List<StratumDef> componentStratumPlural(StratifierDef stratifierDef, List<PopulationDef> populationDefs) {

        final Table<String, StratumValueWrapper, StratifierComponentDef> subjectResultTable =
                buildSubjectResultsTable(stratifierDef);

        // Stratifiers should be of the same basis as population
        // Split subjects by result values
        // ex. all Male Patients and all Female Patients

        var componentSubjects = groupSubjectsByValueDefSet(subjectResultTable);

        var stratumDefs = new ArrayList<StratumDef>();

        componentSubjects.forEach((valueSet, subjects) -> {
            // converts table into component value combinations
            // | Stratum   | Set<ValueDef>           | List<Subjects(String)> |
            // | --------- | ----------------------- | ---------------------- |
            // | Stratum-1 | <'M','White>            | [subject-a]            |
            // | Stratum-2 | <'F','hispanic/latino'> | [subject-b]            |
            // | Stratum-3 | <'M','hispanic/latino'> | [subject-c]            |
            // | Stratum-4 | <'F','black'>           | [subject-d, subject-e] |

            var stratumDef = buildStratumDef(stratifierDef, valueSet, subjects, populationDefs);

            stratumDefs.add(stratumDef);
        });

        return stratumDefs;
    }

    private Table<String, StratumValueWrapper, StratifierComponentDef> buildSubjectResultsTable(
            StratifierDef stratifierDef) {

        final Table<String, StratumValueWrapper, StratifierComponentDef> subjectResultTable = HashBasedTable.create();

        // Component Stratifier
        // one or more criteria expression defined, one set of criteria results per component specified
        // results of component stratifier are an intersection of membership to both component result sets

        stratifierDef.components().forEach(component -> component.getResults().forEach((subject, result) -> {
            StratumValueWrapper stratumValueWrapper = new StratumValueWrapper(result.rawValue());
            subjectResultTable.put(R4ResourceIdUtils.addPatientQualifier(subject), stratumValueWrapper, component);
        }));

        return subjectResultTable;
    }

    private List<StratumDef> nonComponentStratumPlural(
            StratifierDef stratifierDef, List<PopulationDef> populationDefs) {
        // standard Stratifier
        // one criteria expression defined, one set of criteria results

        // standard Stratifier
        // one criteria expression defined, one set of criteria results
        final Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();

        // nonComponent stratifiers will have a single expression that can generate results, instead of grouping
        // combinations of results
        // example: 'gender' expression could produce values of 'M', 'F'
        // subject1: 'gender'--> 'M'
        // subject2: 'gender'--> 'F'
        // stratifier criteria results are: 'M', 'F'

        if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
            // Seems to be irrelevant for criteria based stratifiers
            var stratValues = Set.<StratumValueDef>of();
            // Seems to be irrelevant for criteria based stratifiers
            var patients = List.<String>of();

            var stratum = buildStratumDef(stratifierDef, stratValues, patients, populationDefs);
            return List.of(stratum);
        }

        Map<StratumValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
                .collect(Collectors.groupingBy(
                        x -> new StratumValueWrapper(subjectValues.get(x).rawValue())));

        var stratumMultiple = new ArrayList<StratumDef>();

        // Stratum 1
        // Value: 'M'--> subjects: subject1
        // Stratum 2
        // Value: 'F'--> subjects: subject2
        // loop through each value key
        for (Map.Entry<StratumValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
            // patch Patient values with prefix of ResourceType to match with incoming population subjects for stratum
            // TODO: should match context of CQL, not only Patient
            var patientsSubjects = stratValue.getValue().stream()
                    .map(R4ResourceIdUtils::addPatientQualifier)
                    .toList();
            // build the stratum for each unique value
            // non-component stratifiers will populate a 'null' for componentStratifierDef, since it doesn't have
            // multiple criteria
            // TODO: build out nonComponent stratum method
            Set<StratumValueDef> stratValues = Set.of(new StratumValueDef(stratValue.getKey(), null));
            var stratum = buildStratumDef(stratifierDef, stratValues, patientsSubjects, populationDefs);
            stratumMultiple.add(stratum);
        }

        return stratumMultiple;
    }

    private static Map<Set<StratumValueDef>, List<String>> groupSubjectsByValueDefSet(
            Table<String, StratumValueWrapper, StratifierComponentDef> table) {
        // input format
        // | Subject (String) | CriteriaResult (ValueWrapper) | StratifierComponentDef |
        // | ---------------- | ----------------------------- | ---------------------- |
        // | subject-a        | M                             | gender                 |
        // | subject-b        | F                             | gender                 |
        // | subject-c        | M                             | gender                 |
        // | subject-d        | F                             | gender                 |
        // | subject-e        | F                             | gender                 |
        // | subject-a        | white                         | race                   |
        // | subject-b        | hispanic/latino               | race                   |
        // | subject-c        | hispanic/latino               | race                   |
        // | subject-d        | black                         | race                   |
        // | subject-e        | black                         | race                   |

        // Step 1: Build Map<Subject, Set<ValueDef>>
        final Map<String, Set<StratumValueDef>> subjectToValueDefs = new HashMap<>();

        for (Table.Cell<String, StratumValueWrapper, StratifierComponentDef> cell : table.cellSet()) {
            subjectToValueDefs
                    .computeIfAbsent(cell.getRowKey(), k -> new HashSet<>())
                    .add(new StratumValueDef(cell.getColumnKey(), cell.getValue()));
        }
        // output format:
        // | Set<ValueDef>           | List<Subjects(String)> |
        // | ----------------------- | ---------------------- |
        // | <'M','White>            | [subject-a]            |
        // | <'F','hispanic/latino'> | [subject-b]            |
        // | <'M','hispanic/latino'> | [subject-c]            |
        // | <'F','black'>           | [subject-d, subject-e] |

        // Step 2: Invert to Map<Set<ValueDef>, List<Subject>>
        return subjectToValueDefs.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collector.of(ArrayList::new, (list, e) -> list.add(e.getKey()), (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        })));
    }

    // This is weird pattern where we have multiple qualifying values within a single stratum,
    // which was previously unsupported.  So for now, comma-delim the first five values.
    private static CodeableConcept expressionResultToCodableConcept(StratumValueWrapper value) {
        return new CodeableConcept().setText(value.getValueAsString());
    }
}
