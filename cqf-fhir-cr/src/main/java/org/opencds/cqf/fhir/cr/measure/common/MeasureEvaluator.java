package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCEPTION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOREXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.INITIALPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREOBSERVATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATIONEXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOREXCLUSION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALDENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALNUMERATOR;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.IntervalTypeSpecifier;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.NamedTypeSpecifier;
import org.hl7.elm.r1.ParameterDef;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
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
@SuppressWarnings("removal")
public class MeasureEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);

    protected CqlEngine context;
    protected String measurementPeriodParameterName = null;

    public MeasureEvaluator(CqlEngine context, String measurementPeriodParameterName) {
        this.context = Objects.requireNonNull(context, "context is a required argument");
        this.measurementPeriodParameterName = Objects.requireNonNull(
                measurementPeriodParameterName, "measurementPeriodParameterName is a required argument");
    }

    public MeasureDef evaluate(
            MeasureDef measureDef,
            MeasureEvalType measureEvalType,
            List<String> subjectIds,
            Interval measurementPeriod) {
        Objects.requireNonNull(measureDef, "measureDef is a required argument");
        Objects.requireNonNull(subjectIds, "subjectIds is a required argument");

        // measurementPeriod is not required, because it's often defaulted in CQL
        this.setMeasurementPeriod(measurementPeriod);

        switch (measureEvalType) {
            case PATIENT:
            case SUBJECT:
                return this.evaluate(measureDef, MeasureReportType.INDIVIDUAL, subjectIds);
            case SUBJECTLIST:
                return this.evaluate(measureDef, MeasureReportType.SUBJECTLIST, subjectIds);
            case PATIENTLIST:
                // DSTU3 Only
                return this.evaluate(measureDef, MeasureReportType.PATIENTLIST, subjectIds);
            case POPULATION:
                return this.evaluate(measureDef, MeasureReportType.SUMMARY, subjectIds);
            default:
                // never hit because this value is set upstream
                throw new IllegalArgumentException(
                        String.format("Unsupported Measure Evaluation type: %s", measureEvalType.getDisplay()));
        }
    }

    protected ParameterDef getMeasurementPeriodParameterDef() {
        Library lib = this.context.getState().getCurrentLibrary();

        if (lib.getParameters() == null
                || lib.getParameters().getDef() == null
                || lib.getParameters().getDef().isEmpty()) {
            return null;
        }

        for (ParameterDef pd : lib.getParameters().getDef()) {
            if (this.measurementPeriodParameterName != null
                    && pd.getName().equals(this.measurementPeriodParameterName)) {
                return pd;
            }
        }

        return null;
    }

    protected void setMeasurementPeriod(Interval measurementPeriod) {
        ParameterDef pd = this.getMeasurementPeriodParameterDef();
        if (pd == null) {
            logger.warn(
                    "Parameter \"{}\" was not found. Unable to validate type.", this.measurementPeriodParameterName);
            this.context.getState().setParameter(null, this.measurementPeriodParameterName, measurementPeriod);
            return;
        }

        if (measurementPeriod == null && pd.getDefault() == null) {
            logger.warn(
                    "No default or value supplied for Parameter \"{}\". This may result in incorrect results or errors.",
                    this.measurementPeriodParameterName);
            return;
        }

        // Use the default, skip validation
        if (measurementPeriod == null) {
            measurementPeriod =
                    (Interval) this.context.getEvaluationVisitor().visitParameterDef(pd, this.context.getState());
            this.context.getState().setParameter(null, this.measurementPeriodParameterName, measurementPeriod);
            return;
        }

        IntervalTypeSpecifier intervalTypeSpecifier = (IntervalTypeSpecifier) pd.getParameterTypeSpecifier();
        if (intervalTypeSpecifier == null) {
            logger.debug(
                    "No ELM type information available. Unable to validate type of \"{}\"",
                    this.measurementPeriodParameterName);
            this.context.getState().setParameter(null, this.measurementPeriodParameterName, measurementPeriod);
            return;
        }

        NamedTypeSpecifier pointType = (NamedTypeSpecifier) intervalTypeSpecifier.getPointType();
        String targetType = pointType.getName().getLocalPart();
        Interval convertedPeriod = convertInterval(measurementPeriod, targetType);

        this.context.getState().setParameter(null, this.measurementPeriodParameterName, convertedPeriod);
    }

    protected Interval convertInterval(Interval interval, String targetType) {
        String sourceTypeQualified = interval.getPointType().getTypeName();
        String sourceType =
                sourceTypeQualified.substring(sourceTypeQualified.lastIndexOf(".") + 1, sourceTypeQualified.length());
        if (sourceType.equals(targetType)) {
            return interval;
        }

        if (sourceType.equals("DateTime") && targetType.equals("Date")) {
            logger.debug(
                    "A DateTime interval was provided and a Date interval was expected. The DateTime will be truncated.");
            return new Interval(
                    truncateDateTime((DateTime) interval.getLow()),
                    interval.getLowClosed(),
                    truncateDateTime((DateTime) interval.getHigh()),
                    interval.getHighClosed());
        }

        throw new IllegalArgumentException(String.format(
                "The interval type of %s did not match the expected type of %s and no conversion was possible.",
                sourceType, targetType));
    }

    protected Date truncateDateTime(DateTime dateTime) {
        OffsetDateTime odt = dateTime.getDateTime();
        return new Date(odt.getYear(), odt.getMonthValue(), odt.getDayOfMonth());
    }

    protected Pair<String, String> getSubjectTypeAndId(String subjectId) {
        if (subjectId.contains("/")) {
            String[] subjectIdParts = subjectId.split("/");
            return Pair.of(subjectIdParts[0], subjectIdParts[1]);
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unable to determine Subject type for id: %s. SubjectIds must be in the format {subjectType}/{subjectId} (e.g. Patient/123)",
                    subjectId));
        }
    }

    protected void captureEvaluatedResources(Set<Object> outEvaluatedResources) {
        if (outEvaluatedResources != null && this.context.getState().getEvaluatedResources() != null) {
            for (Object o : this.context.getState().getEvaluatedResources()) {
                outEvaluatedResources.add(o);
            }
        }
        clearEvaluatedResources();
    }

    // reset evaluated resources followed by a context evaluation
    private void clearEvaluatedResources() {
        this.context.getState().clearEvaluatedResources();
    }

    protected MeasureDef evaluate(MeasureDef measureDef, MeasureReportType type, List<String> subjectIds) {

        logger.info(
                "Evaluating Measure {}, report type {}, with {} subject(s)",
                measureDef.url(),
                type.toCode(),
                subjectIds.size());

        Map<GroupDef, MeasureScoring> scoring = measureDef.scoring();

        for (String subjectId : subjectIds) {
            if (subjectId == null) {
                throw new RuntimeException("SubjectId is required in order to calculate.");
            }
            Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectId);
            String subjectTypePart = subjectInfo.getLeft();
            String subjectIdPart = subjectInfo.getRight();
            context.getState().setContextValue(subjectTypePart, subjectIdPart);
            evaluateSubject(measureDef, scoring, subjectTypePart, subjectIdPart);
        }

        return measureDef;
    }

    protected void evaluateSubject(
            MeasureDef measureDef, Map<GroupDef, MeasureScoring> scoring, String subjectType, String subjectId) {
        evaluateSdes(subjectId, measureDef.sdes());
        for (GroupDef groupDef : measureDef.groups()) {
            evaluateGroup(measureDef, groupDef, subjectType, subjectId);
        }
    }

    @SuppressWarnings("unchecked")
    protected Iterable<Object> evaluatePopulationCriteria(
            String subjectType, String subjectId, String criteriaExpression, Set<Object> outEvaluatedResources) {
        if (criteriaExpression == null || criteriaExpression.isEmpty()) {
            return Collections.emptyList();
        }

        Object result = this.evaluateCriteria(criteriaExpression, outEvaluatedResources);

        if (result == null) {
            return Collections.emptyList();
        }

        if (result instanceof Boolean) {
            if ((Boolean.TRUE.equals(result))) {
                var ref = Libraries.resolveExpressionRef(
                        subjectType, this.context.getState().getCurrentLibrary());
                Object booleanResult =
                        this.context.getEvaluationVisitor().visitExpressionDef(ref, this.context.getState());
                clearEvaluatedResources();
                return Collections.singletonList(booleanResult);
            } else {
                return Collections.emptyList();
            }
        }

        return (Iterable<Object>) result;
    }

    protected Object evaluateCriteria(String criteriaExpression, Set<Object> outEvaluatedResources) {
        var ref = Libraries.resolveExpressionRef(
                criteriaExpression, this.context.getState().getCurrentLibrary());
        Object result = this.context.getEvaluationVisitor().visitExpressionDef(ref, this.context.getState());

        captureEvaluatedResources(outEvaluatedResources);

        return result;
    }

    protected Object evaluateObservationCriteria(
            Object resource, String criteriaExpression, Set<Object> outEvaluatedResources, boolean isBooleanBasis) {

        var ed = Libraries.resolveExpressionRef(
                criteriaExpression, this.context.getState().getCurrentLibrary());

        if (!(ed instanceof FunctionDef)) {
            throw new IllegalArgumentException(String.format(
                    "Measure observation %s does not reference a function definition", criteriaExpression));
        }

        Object result;
        context.getState().pushWindow();
        try {
            if (!isBooleanBasis) {
                // subject based observations don't have a parameter to pass in
                context.getState()
                        .push(new Variable()
                                .withName(((FunctionDef) ed).getOperand().get(0).getName())
                                .withValue(resource));
            }
            result = context.getEvaluationVisitor().visitExpression(ed.getExpression(), context.getState());
        } finally {
            context.getState().popWindow();
        }

        captureEvaluatedResources(outEvaluatedResources);

        return result;
    }

    protected PopulationDef evaluatePopulationMembership(
            String subjectType, String subjectId, PopulationDef inclusionDef) {
        // Add Resources from SubjectId
        int i = 0;
        for (Object resource : evaluatePopulationCriteria(
                subjectType, subjectId, inclusionDef.expression(), inclusionDef.getEvaluatedResources())) {
            inclusionDef.addResource(resource);
            i++;
        }
        // If SubjectId Added Resources to Population
        if (i > 0) {
            inclusionDef.addSubject(subjectId);
        }
        return inclusionDef;
    }

    protected void evaluateProportion(MeasureDef measureDef, GroupDef groupDef, String subjectType, String subjectId) {
        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationDef numerator = groupDef.getSingle(NUMERATOR);
        PopulationDef denominator = groupDef.getSingle(DENOMINATOR);
        PopulationDef denominatorExclusion = groupDef.getSingle(DENOMINATOREXCLUSION);
        PopulationDef denominatorException = groupDef.getSingle(DENOMINATOREXCEPTION);
        PopulationDef numeratorExclusion = groupDef.getSingle(NUMERATOREXCLUSION);

        // Validate Required Populations are Present
        if (initialPopulation == null || denominator == null || numerator == null) {
            throw new NullPointerException("`" + INITIALPOPULATION.getDisplay() + "`, `" + NUMERATOR.getDisplay()
                    + "`, `" + DENOMINATOR.getDisplay()
                    + "` are required Population Definitions for Measure Scoring Type: "
                    + measureDef.scoring().get(groupDef).toCode());
        }
        // Ratio Populations Check
        if (measureDef.scoring().get(groupDef).toCode().equals("ratio") && denominatorException != null) {
            throw new IllegalArgumentException(
                    "`" + DENOMINATOREXCEPTION.getDisplay() + "` are not permitted " + "for MeasureScoring type: "
                            + measureDef.scoring().get(groupDef).toCode());
        }

        initialPopulation = evaluatePopulationMembership(subjectType, subjectId, initialPopulation);
        if (initialPopulation.getSubjects().contains(subjectId)) {
            // Evaluate Population Expressions
            denominator = evaluatePopulationMembership(subjectType, subjectId, denominator);
            numerator = evaluatePopulationMembership(subjectType, subjectId, numerator);
            var totalDenominator = groupDef.getSingle(TOTALDENOMINATOR);
            var totalNumerator = groupDef.getSingle(TOTALNUMERATOR);

            // Evaluate Exclusions and Exception Populations
            if (denominatorExclusion != null) {
                denominatorExclusion = evaluatePopulationMembership(subjectType, subjectId, denominatorExclusion);
            }
            if (denominatorException != null) {
                denominatorException = evaluatePopulationMembership(subjectType, subjectId, denominatorException);
            }
            if (numeratorExclusion != null) {
                numeratorExclusion = evaluatePopulationMembership(subjectType, subjectId, numeratorExclusion);
            }
            // Apply Exclusions and Exceptions
            if (measureDef.isBooleanBasis()) {
                // Remove Subject and Resource Exclusions
                if (denominatorExclusion != null) {
                    denominator.getSubjects().removeAll(denominatorExclusion.getSubjects());
                    denominator.getResources().removeAll(denominatorExclusion.getResources());
                    numerator.getSubjects().removeAll(denominatorExclusion.getSubjects());
                    numerator.getResources().removeAll(denominatorExclusion.getResources());
                }
                if (numeratorExclusion != null) {
                    numerator.getSubjects().removeAll(numeratorExclusion.getSubjects());
                    numerator.getResources().removeAll(numeratorExclusion.getResources());
                }
                if (denominatorException != null) {
                    // Remove Subjects Exceptions that are present in Numerator
                    denominatorException.getSubjects().removeAll(numerator.getSubjects());
                    denominatorException.getResources().removeAll(numerator.getResources());
                    // Remove Subjects in Denominator that are not in Numerator
                    denominator.getSubjects().removeAll(denominatorException.getSubjects());
                    denominator.getResources().removeAll(denominatorException.getResources());
                }
                totalDenominator.getSubjects().addAll(denominator.getSubjects());
                totalNumerator.getSubjects().addAll(numerator.getSubjects());
            } else {
                // Remove Only Resource Exclusions
                // * Multiple resources can be from one subject and represented in multiple populations
                // * This is why we only remove resources and not subjects too for `Resource Basis`.
                if (denominatorExclusion != null) {
                    denominator.getResources().removeAll(denominatorExclusion.getResources());
                    numerator.getResources().removeAll(denominatorExclusion.getResources());
                }
                if (numeratorExclusion != null) {
                    numerator.getResources().removeAll(numeratorExclusion.getResources());
                }
                if (denominatorException != null) {
                    // Remove Resource Exceptions that are present in Numerator
                    denominatorException.getResources().removeAll(numerator.getResources());
                    // Remove Resources in Denominator that are not in Numerator
                    denominator.getResources().removeAll(denominatorException.getResources());
                }
                // TODO: Evaluate validity of TotalDenominator & TotalDenominator
                totalDenominator.getResources().addAll(denominator.getResources());
                totalNumerator.getResources().addAll(numerator.getResources());
            }
        }
    }

    protected void evaluateContinuousVariable(
            MeasureDef measureDef, GroupDef groupDef, String subjectType, String subjectId) {
        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
        PopulationDef measureObservation = groupDef.getSingle(MEASUREOBSERVATION);
        PopulationDef measurePopulationExclusion = groupDef.getSingle(MEASUREPOPULATIONEXCLUSION);
        // Validate Required Populations are Present
        if (initialPopulation == null || measurePopulation == null) {
            throw new NullPointerException(
                    "`" + INITIALPOPULATION.getDisplay() + "` & `" + MEASUREPOPULATION.getDisplay()
                            + "` are required Population Definitions for Measure Scoring Type: "
                            + measureDef.scoring().get(groupDef).toCode());
        }

        initialPopulation = evaluatePopulationMembership(subjectType, subjectId, initialPopulation);
        if (initialPopulation.getSubjects().contains(subjectId)) {
            // Evaluate Population Expressions
            measurePopulation = evaluatePopulationMembership(subjectType, subjectId, measurePopulation);

            if (measurePopulationExclusion != null) {
                measurePopulationExclusion = evaluatePopulationMembership(
                        subjectType, subjectId, groupDef.getSingle(MEASUREPOPULATIONEXCLUSION));
            }
            // Apply Exclusions to Population
            if (measureDef.isBooleanBasis()) {
                if (measurePopulationExclusion != null) {
                    measurePopulation.getSubjects().removeAll(measurePopulationExclusion.getSubjects());
                    measurePopulation.getResources().removeAll(measurePopulationExclusion.getResources());
                }
            } else {
                if (measurePopulationExclusion != null) {
                    measurePopulation.getResources().removeAll(measurePopulationExclusion.getResources());
                }
            }
            // Evaluate Observation Population
            if (measureObservation != null) {
                for (Object resource : measurePopulation.getResources()) {
                    Object observationResult = evaluateObservationCriteria(
                            resource,
                            measureObservation.expression(),
                            measureObservation.getEvaluatedResources(),
                            measureDef.isBooleanBasis());
                    measureObservation.addResource(observationResult);
                }
            }
        }
    }

    protected void evaluateCohort(MeasureDef measureDef, GroupDef groupDef, String subjectType, String subjectId) {
        PopulationDef initialPopulation = groupDef.getSingle(INITIALPOPULATION);
        // Validate Required Populations are Present
        if (initialPopulation == null) {
            throw new NullPointerException("`" + INITIALPOPULATION.getDisplay()
                    + "` is a required Population Definition for Measure Scoring Type: "
                    + measureDef.scoring().get(groupDef).toCode());
        }
        // Evaluate Population
        evaluatePopulationMembership(subjectType, subjectId, initialPopulation);
    }

    protected void evaluateGroup(MeasureDef measureDef, GroupDef groupDef, String subjectType, String subjectId) {
        evaluateStratifiers(subjectId, groupDef.stratifiers());

        var scoring = measureDef.scoring().get(groupDef);
        switch (scoring) {
            case PROPORTION:
            case RATIO:
                evaluateProportion(measureDef, groupDef, subjectType, subjectId);
                break;
            case CONTINUOUSVARIABLE:
                evaluateContinuousVariable(measureDef, groupDef, subjectType, subjectId);
                break;
            case COHORT:
                evaluateCohort(measureDef, groupDef, subjectType, subjectId);
                break;
        }
    }

    protected void evaluateSdes(String subjectId, List<SdeDef> sdes) {
        for (SdeDef sde : sdes) {
            var ref = Libraries.resolveExpressionRef(
                    sde.expression(), this.context.getState().getCurrentLibrary());
            Object result = this.context.getEvaluationVisitor().visitExpressionDef(ref, this.context.getState());

            // TODO: This is a hack-around for an cql engine bug. Need to investigate.
            if ((result instanceof List) && (((List<?>) result).size() == 1) && ((List<?>) result).get(0) == null) {
                result = null;
            }

            sde.putResult(subjectId, result, context.getState().getEvaluatedResources());

            clearEvaluatedResources();
        }
    }

    protected void evaluateStratifiers(String subjectId, List<StratifierDef> stratifierDefs) {
        for (StratifierDef sd : stratifierDefs) {
            if (!sd.components().isEmpty()) {
                throw new UnsupportedOperationException("multi-component stratifiers are not yet supported.");
            }

            // TODO: Handle list values as components?
            var ref = Libraries.resolveExpressionRef(
                    sd.expression(), this.context.getState().getCurrentLibrary());
            Object result = this.context.getEvaluationVisitor().visitExpressionDef(ref, this.context.getState());
            if (result instanceof Iterable) {
                var resultIter = ((Iterable<?>) result).iterator();
                if (!resultIter.hasNext()) {
                    result = null;
                } else {
                    result = resultIter.next();
                }

                if (resultIter.hasNext()) {
                    throw new IllegalArgumentException("stratifiers may not return multiple values");
                }
            }

            if (result != null) {
                sd.putResult(subjectId, result, this.context.getState().getEvaluatedResources());
            }

            clearEvaluatedResources();
        }
    }
}
