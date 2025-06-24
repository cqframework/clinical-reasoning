package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCEPTION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.INITIALPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATIONEXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOREXCLUSION;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureScoringTypePopulations;

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
@SuppressWarnings({"removal", "squid:S1135", "squid:S3776"})
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
        // find matching expression
        var matchingResult = evaluationResult.forExpression(inclusionDef.expression());

        // Add Resources from SubjectId
        int i = 0;
        for (Object resource : evaluatePopulationCriteria(
                subjectType, matchingResult, evaluationResult, inclusionDef.getEvaluatedResources())) {
            inclusionDef.addResource(resource);
            // hashmap instead of set
            inclusionDef.addResource(subjectId, resource);

            i++;
        }
        // If SubjectId Added Resources to Population
        if (i > 0) {
            inclusionDef.addSubject(subjectId);
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
            denominator.getResources().retainAll(initialPopulation.getResources());
            denominator.getSubjects().retainAll(initialPopulation.getSubjects());
            // remove numerator values if not in Denominator
            numerator.getSubjects().retainAll(denominator.getSubjects());
            numerator.getResources().retainAll(denominator.getResources());
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
                numerator.getSubjects().removeAll(denominatorExclusion.getSubjects());
                numerator.removeOverlaps(denominatorExclusion.getSubjectResources());

                // verify exclusion results are found in denominator
                denominatorExclusion.getResources().retainAll(denominator.getResources());
                denominatorExclusion.getSubjects().retainAll(denominator.getSubjects());
                denominatorExclusion.retainOverlaps(denominator.getSubjectResources());
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify results are in Numerator
                numeratorExclusion.getResources().retainAll(numerator.getResources());
                numeratorExclusion.getSubjects().retainAll(numerator.getSubjects());
                numeratorExclusion.retainOverlaps(numerator.getSubjectResources());
            }
            if (denominatorException != null && applyScoring) {
                // Remove Subjects Exceptions that are present in Numerator
                denominatorException.getSubjects().removeAll(numerator.getSubjects());
                denominatorException.getResources().removeAll(numerator.getResources());
                denominatorException.removeOverlaps(numerator.getSubjectResources());

                // verify exception results are found in denominator
                denominatorException.getResources().retainAll(denominator.getResources());
                denominatorException.getSubjects().retainAll(denominator.getSubjects());
                denominatorException.retainOverlaps(denominator.getSubjectResources());
            }
        } else {
            // Remove Only Resource Exclusions
            // * Multiple resources can be from one subject and represented in multiple populations
            // * This is why we only remove resources and not subjects too for `Resource Basis`.
            if (denominatorExclusion != null && applyScoring) {
                // remove any denominator-exception subjects/resources found in Numerator
                numerator.getResources().removeAll(denominatorExclusion.getResources());
                numerator.removeOverlaps(denominatorExclusion.getSubjectResources());
                // verify exclusion results are found in denominator
                denominatorExclusion.getResources().retainAll(denominator.getResources());
                denominatorExclusion.retainOverlaps(denominator.getSubjectResources());
            }
            if (numeratorExclusion != null && applyScoring) {
                // verify exclusion results are found in numerator results, otherwise remove
                numeratorExclusion.getResources().retainAll(numerator.getResources());
                numeratorExclusion.retainOverlaps(numerator.getSubjectResources());
            }
            if (denominatorException != null && applyScoring) {
                // Remove Resource Exceptions that are present in Numerator
                denominatorException.getResources().removeAll(numerator.getResources());
                denominatorException.removeOverlaps(numerator.getSubjectResources());
                // verify exception results are found in denominator
                denominatorException.getResources().retainAll(denominator.getResources());
                denominatorException.retainOverlaps(denominator.getSubjectResources());
            }
        }
        if (reportType.equals(MeasureReportType.INDIVIDUAL) && dateOfCompliance != null) {
            var doc = evaluateDateOfCompliance(dateOfCompliance, evaluationResult);
            dateOfCompliance.addResource(doc);
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
        // Validate Required Populations are Present
        R4MeasureScoringTypePopulations.validateScoringTypePopulations(
                groupDef.populations().stream().map(PopulationDef::type).toList(), MeasureScoring.CONTINUOUSVARIABLE);

        initialPopulation = evaluatePopulationMembership(subjectType, subjectId, initialPopulation, evaluationResult);
        if (initialPopulation.getSubjects().contains(subjectId)) {
            // Evaluate Population Expressions
            measurePopulation =
                    evaluatePopulationMembership(subjectType, subjectId, measurePopulation, evaluationResult);

            if (measurePopulationExclusion != null) {
                evaluatePopulationMembership(
                        subjectType, subjectId, groupDef.getSingle(MEASUREPOPULATIONEXCLUSION), evaluationResult);
                if (applyScoring) {
                    // verify exclusions are in measure-population
                    measurePopulationExclusion.getResources().retainAll(measurePopulation.getResources());
                    measurePopulationExclusion.getSubjects().retainAll(measurePopulation.getSubjects());
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

    protected Object addStratifierResult(Object result, String subjectId) {
        if (result instanceof Iterable<?> iterable) {
            var resultIter = iterable.iterator();
            if (!resultIter.hasNext()) {
                result = null;
            } else {
                result = resultIter.next();
            }

            if (resultIter.hasNext()) {
                throw new InvalidRequestException(
                        "stratifiers may not return multiple values for subjectId: " + subjectId);
            }
        }
        return result;
    }

    protected void addStratifierComponentResult(
            List<StratifierComponentDef> components, EvaluationResult evaluationResult, String subjectId) {
        for (StratifierComponentDef component : components) {
            var expressionResult = evaluationResult.forExpression(component.expression());
            Object result = addStratifierResult(expressionResult.value(), subjectId);
            if (result != null) {
                component.putResult(subjectId, result, expressionResult.evaluatedResources());
            }
        }
    }

    protected void evaluateStratifiers(
            String subjectId, List<StratifierDef> stratifierDefs, EvaluationResult evaluationResult) {
        for (StratifierDef sd : stratifierDefs) {

            if (!sd.components().isEmpty()) {
                addStratifierComponentResult(sd.components(), evaluationResult, subjectId);
            } else {

                var expressionResult = evaluationResult.forExpression(sd.expression());
                Object result = addStratifierResult(expressionResult.value(), subjectId);
                if (result != null) {
                    sd.putResult(
                            subjectId, // context of CQL expression ex: Patient based
                            result,
                            expressionResult.evaluatedResources());
                }
            }
        }
    }
}
