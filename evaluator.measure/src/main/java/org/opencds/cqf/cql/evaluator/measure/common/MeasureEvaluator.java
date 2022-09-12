package org.opencds.cqf.cql.evaluator.measure.common;

import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.DENOMINATOR;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.DENOMINATOREXCEPTION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.DENOMINATOREXCLUSION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.INITIALPOPULATION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.MEASUREOBSERVATION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.MEASUREPOPULATION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.MEASUREPOPULATIONEXCLUSION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.NUMERATOR;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.NUMERATOREXCLUSION;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.Expression;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.cqframework.cql.elm.execution.IntervalTypeSpecifier;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.NamedTypeSpecifier;
import org.cqframework.cql.elm.execution.ParameterDef;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.elm.execution.ExpressionRefEvaluator;
import org.opencds.cqf.cql.engine.elm.execution.InstanceEvaluator;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the core Measure evaluation logic that's defined in the
 * Quality Measure implementation guide and HQMF specifications. There are a
 * number of model-independent concepts such as "groups", "populations", and
 * "stratifiers" that can be used across a number of different data models
 * including FHIR, QDM, and QICore. To the extent feasible, this class is
 * intended to be model-independent so that it can be used in any Java-based
 * implementation of Quality Measure evaluation.
 *
 * @see <a href=
 *      "http://hl7.org/fhir/us/cqfmeasures/introduction.html">http://hl7.org/fhir/us/cqfmeasures/introduction.html</a>
 * @see <a href=
 *      "http://www.hl7.org/implement/standards/product_brief.cfm?product_id=97">http://www.hl7.org/implement/standards/product_brief.cfm?product_id=97</a>
 */
public class MeasureEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);

    protected Context context;
    protected String measurementPeriodParameterName = null;

    public MeasureEvaluator(Context context, String measurementPeriodParameterName) {
        this.context = Objects.requireNonNull(context, "context is a required argument");
        this.measurementPeriodParameterName = Objects.requireNonNull(measurementPeriodParameterName,
                "measurementPeriodParameterName is a required argument");
    }

    public MeasureDef evaluate(MeasureDef measureDef, MeasureEvalType measureEvalType, List<String> subjectIds,
            Interval measurementPeriod) {
        Objects.requireNonNull(measureDef, "measureDef is a required argument");
        Objects.requireNonNull(subjectIds, "subjectIds is a required argument");

        // default behavior is population for many subjects, individual for one subject
        if (measureEvalType == null) {
            measureEvalType = subjectIds.size() > 1 ? MeasureEvalType.POPULATION : MeasureEvalType.SUBJECT;
        }

        // measurementPeriod is not required, because it's often defaulted in CQL
        this.setMeasurementPeriod(measurementPeriod);

        switch (measureEvalType) {
            case PATIENT:
            case SUBJECT:
                return this.evaluate(measureDef, MeasureReportType.INDIVIDUAL, subjectIds);
            case SUBJECTLIST:
                return this.evaluate(measureDef, MeasureReportType.SUBJECTLIST, subjectIds);
            case PATIENTLIST:
                return this.evaluate(measureDef, MeasureReportType.PATIENTLIST, subjectIds);
            case POPULATION:
                return this.evaluate(measureDef, MeasureReportType.SUMMARY, subjectIds);
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported Measure Evaluation type: %s", measureEvalType.getDisplay()));
        }
    }

    protected Interval getMeasurementPeriod() {
        return (Interval) this.context.resolveParameterRef(null, this.measurementPeriodParameterName);
    }

    protected ParameterDef getMeasurementPeriodParameterDef() {
        Library lib = this.context.getCurrentLibrary();

        if (lib.getParameters() == null || lib.getParameters().getDef() == null
                || lib.getParameters().getDef().isEmpty()) {
            return null;
        }

        for (ParameterDef pd : lib.getParameters().getDef()) {
            if (pd.getName().equals(this.measurementPeriodParameterName)) {
                return pd;
            }
        }

        return null;

    }

    protected void setMeasurementPeriod(Interval measurementPeriod) {
        if (measurementPeriod == null) {
            return;
        }

        ParameterDef pd = this.getMeasurementPeriodParameterDef();
        if (pd == null) {
            logger.warn("Parameter \"{}\" was not found. Unable to validate type.",
                    this.measurementPeriodParameterName);
            this.context.setParameter(null, this.measurementPeriodParameterName, measurementPeriod);
            return;
        }

        IntervalTypeSpecifier intervalTypeSpecifier = (IntervalTypeSpecifier) pd.getParameterTypeSpecifier();
        if (intervalTypeSpecifier == null) {
            logger.debug("No ELM type information available. Unable to validate type of \"{}\"",
                    this.measurementPeriodParameterName);
            this.context.setParameter(null, this.measurementPeriodParameterName, measurementPeriod);
            return;
        }

        NamedTypeSpecifier pointType = (NamedTypeSpecifier) intervalTypeSpecifier.getPointType();
        String targetType = pointType.getName().getLocalPart();
        Interval convertedPeriod = convertInterval(measurementPeriod, targetType);

        this.context.setParameter(null, this.measurementPeriodParameterName, convertedPeriod);
    }

    protected Interval convertInterval(Interval interval, String targetType) {
        String sourceTypeQualified = interval.getPointType().getTypeName();
        String sourceType = sourceTypeQualified.substring(sourceTypeQualified.lastIndexOf(".") + 1,
                sourceTypeQualified.length());
        if (sourceType.equals(targetType)) {
            return interval;
        }

        if (sourceType.equals("DateTime") && targetType.equals("Date")) {
            logger.debug(
                    "A DateTime interval was provided and a Date interval was expected. The DateTime will be truncated.");
            return new Interval(truncateDateTime((DateTime) interval.getLow()), interval.getLowClosed(),
                    truncateDateTime((DateTime) interval.getHigh()), interval.getHighClosed());
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

    protected void setContextToSubject(String subjectType, String subjectId) {
        context.setContextValue(subjectType, subjectId);
    }

    protected void captureEvaluatedResources(VersionedIdentifier libraryId, String criteriaExpression, List<Object> outEvaluatedResources) {
        if (outEvaluatedResources != null &&
                this.context.isExpressionInCache(libraryId, criteriaExpression)) {
            outEvaluatedResources.addAll(this.context.getExpressionEvaluatedResourceFromCache(libraryId, criteriaExpression));
        }
    }

    // reset evaluated resources followed by a context evaluation
    private void clearEvaluatedResources() {
        this.context.clearEvaluatedResources();
    }

    protected MeasureDef evaluate(MeasureDef measureDef, MeasureReportType type, List<String> subjectIds) {

        logger.info("Evaluating Measure {}, report type {}, with {} subject(s)", measureDef.getUrl(), type.toCode(),
                subjectIds.size());

        MeasureScoring measureScoring = measureDef.getMeasureScoring();
        if (measureScoring == null) {
            throw new RuntimeException("MeasureScoring type is required in order to calculate.");
        }

        for (GroupDef groupDef : measureDef.getGroups()) {
            evaluateGroup(measureDef.getLibraryId(), measureScoring, groupDef, measureDef.getSdes(), subjectIds);
            validateEvaluatedMeasureCount(measureScoring, groupDef);
        }

        return measureDef;
    }

    private void validateEvaluatedMeasureCount(MeasureScoring measureScoring, GroupDef groupDef) {
        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                // the count of denominator + denominator exclusion + denominator exception must
                // be <= the count of initial population.
                if (groupDef.get(INITIALPOPULATION).getResources()
                        .size() < ((groupDef.get(DENOMINATOR) != null ? groupDef.get(DENOMINATOR).getResources().size()
                        : 0) +
                        (groupDef.get(DENOMINATOREXCEPTION) != null
                                ? groupDef.get(DENOMINATOREXCEPTION).getResources().size()
                                : 0)
                        +
                        (groupDef.get(DENOMINATOREXCLUSION) != null
                                ? groupDef.get(DENOMINATOREXCLUSION).getResources().size()
                                : 0))) {
                    logger.debug("For group: {}, Initial population count is less than the sum of denominator," +
                            " denominator exception and denominator exclusion", groupDef.getId());
                }

                // the count of numerator + numerator exclusion must be <= the count of the
                // denominator.
                if ((groupDef.get(DENOMINATOR) != null ? groupDef.get(DENOMINATOR).getResources().size()
                        : 0) < ((groupDef.get(NUMERATOR) != null ? groupDef.get(NUMERATOR).getResources().size() : 0) +
                        (groupDef.get(NUMERATOREXCLUSION) != null
                                ? groupDef.get(NUMERATOREXCLUSION).getResources().size()
                                : 0))) {
                    logger.debug(
                            "For group: {}, Denominator count is less than the sum of numerator and numerator exclusion",
                            groupDef.getId());
                }

                break;
            default:
        }
    }

    @SuppressWarnings("unchecked")
    protected Iterable<Object> evaluatePopulationCriteria(VersionedIdentifier libraryId, String subjectType, String subjectId,
            String criteriaExpression, List<Object> outEvaluatedResources) {
        if (criteriaExpression == null || criteriaExpression.isEmpty()) {
            return Collections.emptyList();
        }

        Object result = this.evaluateCriteria(libraryId, criteriaExpression, outEvaluatedResources);

        if (result == null) {
            return Collections.emptyList();
        }

        if (result instanceof Boolean) {
            if ((Boolean.TRUE.equals(result))) {
                Object booleanResult = this.context.resolveExpressionRef(subjectType).evaluate(this.context);
                return Collections.singletonList(booleanResult);
            } else {
                return Collections.emptyList();
            }
        }

        return (Iterable<Object>) result;
    }

    protected Object evaluateCriteria(VersionedIdentifier libraryId, String criteriaExpression, List<Object> outEvaluatedResources) {
        Object result = context.resolveExpressionRef(criteriaExpression).evaluate(context);

        captureEvaluatedResources(libraryId, criteriaExpression, outEvaluatedResources);

        return result;
    }

    protected Object evaluateObservationCriteria(VersionedIdentifier libraryId, Object resource, String criteriaExpression,
            List<Object> outEvaluatedResources) {

        ExpressionDef ed = context.resolveExpressionRef(criteriaExpression);
        if (!(ed instanceof FunctionDef)) {
            throw new IllegalArgumentException(String
                    .format("Measure observation %s does not reference a function definition", criteriaExpression));
        }

        Object result = null;
        context.pushWindow();
        try {
            context.push(new Variable().withName(((FunctionDef) ed).getOperand().get(0).getName()).withValue(resource));
            result = ed.getExpression().evaluate(context);
        } finally {
            context.popWindow();
        }

        captureEvaluatedResources(libraryId, criteriaExpression, outEvaluatedResources);

        return result;
    }

    protected boolean evaluatePopulationMembership(VersionedIdentifier libraryId, String subjectType, String subjectId, PopulationDef inclusionDef,
            PopulationDef exclusionDef) {
        boolean inPopulation = false;
        for (Object resource : evaluatePopulationCriteria(libraryId, subjectType, subjectId, inclusionDef.getCriteriaExpression(),
                inclusionDef.getEvaluatedResources())) {
            inPopulation = true;
            inclusionDef.getResources().add(resource);
        }

        if (inPopulation && exclusionDef != null) {
            for (Object resource : evaluatePopulationCriteria(libraryId, subjectType, subjectId,
                    exclusionDef.getCriteriaExpression(), exclusionDef.getEvaluatedResources())) {
                inPopulation = false;
                exclusionDef.getResources().add(resource);
                inclusionDef.getResources().remove(resource);
            }
        }

        if (inPopulation) {
            inclusionDef.getSubjects().add(subjectId);
        }

        if (!inPopulation && exclusionDef != null) {
            exclusionDef.getSubjects().add(subjectId);
        }

        return inPopulation;
    }

    protected void evaluateProportion(VersionedIdentifier libraryId, GroupDef groupDef, String subjectType, String subjectId) {
        // Are they in the initial population?
        boolean inInitialPopulation = evaluatePopulationMembership(libraryId, subjectType, subjectId,
                groupDef.get(INITIALPOPULATION), null);
        if (inInitialPopulation) {
            // Are they in the denominator?
            boolean inDenominator = evaluatePopulationMembership(libraryId, subjectType, subjectId, groupDef.get(DENOMINATOR),
                    groupDef.get(DENOMINATOREXCLUSION));

            if (inDenominator) {
                // Are they in the numerator?
                boolean inNumerator = evaluatePopulationMembership(libraryId, subjectType, subjectId, groupDef.get(NUMERATOR),
                        groupDef.get(NUMERATOREXCLUSION));

                if (!inNumerator && groupDef.get(DENOMINATOREXCEPTION) != null) {
                    // Are they in the denominator exception?

                    PopulationDef denominatorException = groupDef.get(DENOMINATOREXCEPTION);
                    PopulationDef denominator = groupDef.get(DENOMINATOR);
                    boolean inException = false;
                    for (Object resource : evaluatePopulationCriteria(libraryId, subjectType, subjectId,
                            denominatorException.getCriteriaExpression(),
                            denominatorException.getEvaluatedResources())) {
                        inException = true;
                        denominatorException.getResources().add(resource);
                        denominator.getResources().remove(resource);

                    }

                    if (inException) {
                        denominatorException.getSubjects().add(subjectId);
                        denominator.getSubjects().remove(subjectId);
                    }
                }
            }
        }
    }

    protected void evaluateContinuousVariable(VersionedIdentifier libraryId, GroupDef groupDef, String subjectType, String subjectId) {
        boolean inInitialPopulation = evaluatePopulationMembership(libraryId, subjectType, subjectId,
                groupDef.get(INITIALPOPULATION), null);

        if (inInitialPopulation) {
            // Are they in the MeasureType population?
            PopulationDef measurePopulation = groupDef.get(MEASUREPOPULATION);
            boolean inMeasurePopulation = evaluatePopulationMembership(libraryId, subjectType, subjectId, measurePopulation,
                    groupDef.get(MEASUREPOPULATIONEXCLUSION));

            if (inMeasurePopulation) {
                PopulationDef measureObservation = groupDef.get(MEASUREOBSERVATION);
                if (measureObservation != null) {
                    for (Object resource : measurePopulation.getResources()) {
                        Object observationResult = evaluateObservationCriteria(libraryId, resource,
                                measureObservation.getCriteriaExpression(), measureObservation.getEvaluatedResources());
                        measureObservation.getResources().add(observationResult);
                    }
                }
            }
        }
    }

    protected void evaluateCohort(VersionedIdentifier libraryId, GroupDef groupDef, String subjectType, String subjectId) {
        evaluatePopulationMembership(libraryId, subjectType, subjectId, groupDef.get(INITIALPOPULATION), null);
    }

    protected void evaluateGroup(VersionedIdentifier libraryId, MeasureScoring measureScoring, GroupDef groupDef,
         List<SdeDef> sdes, Collection<String> subjectIdentifiers) {
        for (String subjectIdentifier : subjectIdentifiers) {
            Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectIdentifier);
            String subjectType = subjectInfo.getLeft();
            String subjectId = subjectInfo.getRight();
            this.setContextToSubject(subjectType, subjectId);
            switch (measureScoring) {
                case PROPORTION:
                case RATIO:
                    evaluateProportion(libraryId, groupDef, subjectType, subjectId);
                    break;
                case CONTINUOUSVARIABLE:
                    evaluateContinuousVariable(libraryId, groupDef, subjectType, subjectId);
                    break;
                case COHORT:
                    evaluateCohort(libraryId, groupDef, subjectType, subjectId);
                    break;
            }
            evaluateSdes(sdes);
            evaluateStratifiers(subjectId, groupDef.getStratifiers());
        }
    }

    protected void evaluateSdes(List<SdeDef> sdes) {
        for (SdeDef sde : sdes) {
            ExpressionDef expressionDef = this.context.resolveExpressionRef(sde.getExpression());
            inspectInstanceEvaluation(sde, expressionDef);
            Object result = expressionDef.evaluate(this.context);

            // TODO: Is it valid for an SDE to give multiple results?
            flattenAdd(sde.getValues(), result);
        }
    }

    // consider more complex expression in future
    private void inspectInstanceEvaluation(SdeDef sdeDef, ExpressionDef expressionDef) {
        Expression expression = expressionDef.getExpression();
        if (expression.getClass() == InstanceEvaluator.class) {
            sdeDef.setIsInstanceExpression(true);
        } else if (expression.getClass() == ExpressionRefEvaluator.class &&
                ((ExpressionRefEvaluator) expression).getLibraryName() != null) {
            ExpressionRefEvaluator expressionRef = ((ExpressionRefEvaluator) expression);
            context.enterLibrary(expressionRef.getLibraryName());
            ExpressionDef nextExpressionDef = this.context.resolveExpressionRef(expressionRef.getName());
            inspectInstanceEvaluation(sdeDef, nextExpressionDef);
            context.exitLibrary(true);
        } else if (expression.getClass() == ExpressionRefEvaluator.class &&
                ((ExpressionRefEvaluator) expression).getLibraryName() == null) {
            ExpressionRefEvaluator expressionRef = ((ExpressionRefEvaluator) expression);
            ExpressionDef nextExpressionDef = this.context.resolveExpressionRef(expressionRef.getName());
            inspectInstanceEvaluation(sdeDef, nextExpressionDef);
        }
    }

    protected void evaluateStratifiers(String subjectId, List<StratifierDef> stratifierDefs) {
        for (StratifierDef sd : stratifierDefs) {
            if (!sd.getComponents().isEmpty()) {
                throw new UnsupportedOperationException("multi-component stratifiers are not yet supported.");
            }

            // TODO: Handle list values as components?
            Object result = this.context.resolveExpressionRef(sd.getExpression()).evaluate(this.context);
            if (result instanceof Iterable) {
                Iterator<?> resultIter = ((Iterable<?>) result).iterator();
                if (resultIter.hasNext()) {
                    result = resultIter.next();
                } else {
                    result = null;
                }
            }

            if (result != null) {
                sd.getSubjectValues().put(subjectId, result);
            }
        }
    }

    protected void flattenAdd(List<Object> values, Object item) {
        if (item == null) {
            return;
        }

        if (item instanceof Iterable) {
            for (Object o : (Iterable<?>) item) {
                flattenAdd(values, o);
            }
        } else {
            values.add(item);
        }
    }
}