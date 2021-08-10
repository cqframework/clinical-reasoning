package org.opencds.cqf.cql.evaluator.measure.common;

import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.DENOMINATOR;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.DENOMINATOREXCLUSION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.INITIALPOPULATION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.MEASUREOBSERVATION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.MEASUREPOPULATION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.MEASUREPOPULATIONEXCLUSION;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.NUMERATOR;
import static org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType.NUMERATOREXCLUSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the core Measure evaluation logic that's defined in the Quality Measure
 * implementation guide and HQMF specifications. There are a number of model-independent concepts such as
 * "groups", "populations", and "stratifiers" that can be used across a number of different data models
 * including FHIR, QDM, and QICore. To the extent feasible, this class is intended to be model-independent
 * so that it can be used in any Java-based implementation of Quality Measure evaluation.
 * 
 * @see <a href="http://hl7.org/fhir/us/cqfmeasures/introduction.html">http://hl7.org/fhir/us/cqfmeasures/introduction.html</a>
 * @see <a href="http://www.hl7.org/implement/standards/product_brief.cfm?product_id=97">http://www.hl7.org/implement/standards/product_brief.cfm?product_id=97</a>
 */
public abstract class MeasureEvaluation<BaseT, MeasureT extends BaseT, MeasureReportT extends BaseT, SubjectT> {

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluation.class);

    protected MeasureT measure;
    protected Context context;
    protected Function<SubjectT, String> getId;
    protected String subjectType = null;
    protected MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder;
    protected MeasureDefBuilder<MeasureT> measureDefBuilder;
    protected String modelUri = null;
    protected String measurementPeriodParameterName = null;

    public MeasureEvaluation(Context context, MeasureT measure, Function<SubjectT, String> getId, MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder, MeasureDefBuilder<MeasureT> measureDefBuilder) {
        this(context, measure, getId, measureReportBuilder, measureDefBuilder, MeasureConstants.FHIR_MODEL_URI, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);

    }

    public MeasureEvaluation(Context context, MeasureT measure, Function<SubjectT, String> getId, MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder, MeasureDefBuilder<MeasureT> measureDefBuilder, String modelUri, String measurementPeriodParameterName) {
        this.measure = measure;
        this.context = context;
        this.getId = getId;
        this.measureDefBuilder = measureDefBuilder;
        this.measureReportBuilder = measureReportBuilder;
        this.measurementPeriodParameterName= measurementPeriodParameterName;
        this.modelUri = modelUri;
    }

    public MeasureReportT evaluate(MeasureEvalType type) {
        return this.evaluate(type, null, null);
    }

    public MeasureReportT evaluate(MeasureEvalType type, String subjectOrPractitionerId) {
        return this.evaluate(type, subjectOrPractitionerId, null);
    }

    public MeasureReportT evaluate(MeasureEvalType type, Interval interval) {
        return this.evaluate(type, null, interval);
    }

    public MeasureReportT evaluate(MeasureEvalType type, String subjectOrPractitionerId, Interval measurementPeriod) {
        this.setMeasurementPeriod(measurementPeriod);

        List<SubjectT> subjects = getSubjects(type, subjectOrPractitionerId);
        switch (type) {
            case PATIENT:
            case SUBJECT:
                return this.evaluate(subjects, MeasureReportType.INDIVIDUAL);
            case SUBJECTLIST:
                return this.evaluate(subjects, MeasureReportType.SUBJECTLIST);
            case PATIENTLIST:
                return this.evaluate(subjects, MeasureReportType.PATIENTLIST);
            case POPULATION:
                return this.evaluate(subjects, MeasureReportType.SUMMARY);
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported Measure Evaluation type: %s", type.getDisplay()));
        }
    }

    public List<SubjectT> getSubjects(MeasureEvalType type, String subjectOrPractitionerId) {
        switch (type) {
            case PATIENT:
            case SUBJECT:
                String subjectId = null;
                if (subjectOrPractitionerId != null && subjectOrPractitionerId.contains("/")) {
                    String[] subjectIdParts = subjectOrPractitionerId.split("/");
                    this.subjectType = subjectIdParts[0];
                    subjectId = subjectIdParts[1];
                } else if (subjectOrPractitionerId != null) {
                    this.subjectType = "Patient";
                    subjectId = subjectOrPractitionerId;
                    logger.info("Could not determine subjectType. Defaulting to Patient");
        
                }
                else {
                    throw new IllegalArgumentException("subjectOrPractitionerId can not be null for a Subject report");
                }

                return this.getIndividualSubject(subjectId);
            case SUBJECTLIST:
            case PATIENTLIST:
                    this.subjectType = "Patient";
                    return subjectOrPractitionerId == null ? getAllSubjects() : getPractitionerSubjects(subjectOrPractitionerId);
            case POPULATION:
                this.subjectType = "Patient";
                return this.getAllSubjects();
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported Measure Evaluation type: %s", type.getDisplay()));
        }
    }

    protected Interval getMeasurementPeriod() {
        return (Interval) this.context.resolveParameterRef(null, this.measurementPeriodParameterName);
    }

    protected void setMeasurementPeriod(Interval measurementPeriod) {
        if (measurementPeriod != null) {
                this.context.setParameter(null, this.measurementPeriodParameterName, measurementPeriod);
        }
    }

    protected DataProvider getDataProvider() {
        return this.context.resolveDataProviderByModelUri(this.modelUri);
    }

    @SuppressWarnings("unchecked")
    protected List<SubjectT> getAllSubjects() {
        List<SubjectT> subjects = new ArrayList<>();
        Iterable<Object> subjectRetrieve = this.getDataProvider().retrieve(null, null, null, subjectType, null, null,
                null, null, null, null, null, null);
        subjectRetrieve.forEach(x -> subjects.add((SubjectT) x));
        return subjects;
    }

    @SuppressWarnings("unchecked")
    protected List<SubjectT> getPractitionerSubjects(String practitionerRef) {
        List<SubjectT> subjects = new ArrayList<>();
        Iterable<Object> subjectRetrieve = this.getDataProvider().retrieve("Practitioner", "generalPractitioner",
                practitionerRef, subjectType, null, null, null, null, null, null, null, null);
        subjectRetrieve.forEach(x -> subjects.add((SubjectT) x));
        return subjects;
    }

    @SuppressWarnings("unchecked")
    protected List<SubjectT> getIndividualSubject(String subjectId) {
        Iterable<Object> subjectRetrieve = this.getDataProvider().retrieve(subjectType, "id", subjectId, subjectType,
                null, null, null, null, null, null, null, null);
        SubjectT subject = null;
        if (subjectRetrieve.iterator().hasNext()) {
            subject = (SubjectT) subjectRetrieve.iterator().next();
        }

        return subject == null ? Collections.emptyList() : Collections.singletonList(subject);
    }

    protected void setContextToSubject(SubjectT subject) {
        String subjectId = this.getId.apply(subject);

        if (subjectId.contains("/")) {
            String[] subjectIdParts = subjectId.split("/");
            subjectId = subjectIdParts[1];
        }

        context.setContextValue(subjectType, subjectId);
    }

    protected void captureEvaluatedResources(Set<Object> outEvaluatedResources) {
        if (outEvaluatedResources != null) {
            if (this.context.getEvaluatedResources() != null) {
                for (Object o : this.context.getEvaluatedResources()) {
                    outEvaluatedResources.add(o);
                }
            }
        }

        this.context.clearEvaluatedResources();
    }

    protected MeasureReportT evaluate(List<SubjectT> subjects, MeasureReportType type) {
        MeasureDef measureDef = this.measureDefBuilder.build(this.measure);

        this.innerEvaluate(measureDef, subjects, type);

        return this.measureReportBuilder.build(this.measure, measureDef, type, this.getMeasurementPeriod(), subjects);
    }

    protected void innerEvaluate(
            MeasureDef measureDef,
            List<SubjectT> subjects, MeasureReportType type) {

        logger.info("Evaluating Measure {}, report type {}, with {} subject(s)",  measureDef.getUrl(), type.toCode(), subjects.size());

        MeasureScoring measureScoring = measureDef.getMeasureScoring();
        if (measureScoring == null) {
            throw new RuntimeException("MeasureScoring type is required in order to calculate.");
        }

        for (GroupDef groupDef : measureDef.getGroups()) {
            evaluateGroup(measureScoring, groupDef, measureDef.getSdes(), subjects);
        }
    }

    @SuppressWarnings("unchecked")
    protected Iterable<SubjectT> evaluatePopulationCriteria(SubjectT subject, String criteriaExpression,
            Set<Object> outEvaluatedResources) {
        if (criteriaExpression == null || criteriaExpression.isEmpty()) {
            return Collections.emptyList();
        }

        Object result = this.evaluateCriteria(criteriaExpression, outEvaluatedResources);

        if (result == null) {
            Collections.emptyList();
        }

        if (result instanceof Boolean) {
            if (((Boolean) result)) {
                return Collections.singletonList(subject);
            } else {
                return Collections.emptyList();
            }
        }

        return (Iterable<SubjectT>) result;
    }

    protected Object evaluateCriteria(String criteriaExpression, Set<Object> outEvaluatedResources) {
        Object result = context.resolveExpressionRef(criteriaExpression).evaluate(context);

        captureEvaluatedResources(outEvaluatedResources);

        return result;
    }

    protected Object evaluateObservationCriteria(Object resource, String criteriaExpression,
            Set<Object> outEvaluatedResources) {

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

        captureEvaluatedResources(outEvaluatedResources);
 
        return result;
    }

    protected boolean evaluatePopulationMembership(SubjectT subject, PopulationDef inclusionDef,
            PopulationDef exclusionDef) {
        boolean inPopulation = false;
        for (Object resource : evaluatePopulationCriteria(subject, inclusionDef.getCriteriaExpression(),
                inclusionDef.getEvaluatedResources())) {
            inPopulation = true;
            inclusionDef.getResources().add(resource);
        }

        if (inPopulation) {
            if (exclusionDef != null) {
                for (Object resource : evaluatePopulationCriteria(subject, exclusionDef.getCriteriaExpression(),
                        exclusionDef.getEvaluatedResources())) {
                    inPopulation = false;
                    exclusionDef.getResources().add(resource);
                    inclusionDef.getResources().remove(resource);
                }
            }
        }

        if (inPopulation) {
            inclusionDef.getSubjects().add(subject);
        }

        if (!inPopulation && exclusionDef != null) {
            exclusionDef.getSubjects().add(subject);
        }

        return inPopulation;
    }

    protected void evaluateProportion(GroupDef groupDef, SubjectT subject) {
        // Are they in the initial population?
        boolean inInitialPopulation = evaluatePopulationMembership(subject, groupDef.get(INITIALPOPULATION), null);
        if (inInitialPopulation) {
            // Are they in the denominator?
            boolean inDenominator = evaluatePopulationMembership(subject, groupDef.get(DENOMINATOR),
                    groupDef.get(DENOMINATOREXCLUSION));

            if (inDenominator) {
                // Are they in the numerator?
                boolean inNumerator = evaluatePopulationMembership(subject, groupDef.get(NUMERATOR),
                        groupDef.get(NUMERATOREXCLUSION));

                if (!inNumerator && inDenominator && groupDef.get(DENOMINATOREXCLUSION) != null) {
                    // Are they in the denominator exception?

                    PopulationDef denominatorExclusion = groupDef.get(DENOMINATOREXCLUSION);
                    PopulationDef denominator = groupDef.get(DENOMINATOR);
                    boolean inException = false;
                    for (SubjectT resource : evaluatePopulationCriteria(subject,
                            denominatorExclusion.getCriteriaExpression(),
                            denominatorExclusion.getEvaluatedResources())) {
                        inException = true;
                        denominatorExclusion.getResources().add(resource);
                        denominator.getResources().remove(resource);

                    }

                    if (inException) {
                        denominatorExclusion.getSubjects().add(subject);
                        denominator.getSubjects().remove(subject);
                    }
                }
            }
        }
    }

    protected void evaluateContinuousVariable(GroupDef groupDef, SubjectT subject) {
        boolean inInitialPopulation = evaluatePopulationMembership(subject, groupDef.get(INITIALPOPULATION), null);

        if (inInitialPopulation) {
            // Are they in the MeasureType population?
            PopulationDef measurePopulation = groupDef.get(MEASUREPOPULATION);
            boolean inMeasurePopulation = evaluatePopulationMembership(subject, measurePopulation,
                    groupDef.get(MEASUREPOPULATIONEXCLUSION));

            if (inMeasurePopulation) {
                PopulationDef measureObservation = groupDef.get(MEASUREOBSERVATION);
                if (measureObservation != null) {
                    for (Object resource : measurePopulation.getResources()) {
                        Object observationResult = evaluateObservationCriteria(resource,
                                measureObservation.getCriteriaExpression(), measureObservation.getEvaluatedResources());
                        measureObservation.getResources().add(observationResult);
                    }
                }
            }
        }
    }

    protected void evaluateCohort(GroupDef groupDef, SubjectT subject) {
        evaluatePopulationMembership(subject, groupDef.get(INITIALPOPULATION), null);
    }

    protected void evaluateGroup(MeasureScoring measureScoring, GroupDef groupDef, Set<SdeDef> sdes,
            Collection<SubjectT> subjects) {
        for (SubjectT subject : subjects) {
            setContextToSubject(subject);
            evaluateSdes(sdes);
            evaluateStratifiers(subject, groupDef.getStratifiers());
            switch (measureScoring) {
                case PROPORTION:
                case RATIO:
                    evaluateProportion(groupDef, subject);
                    break;
                case CONTINUOUSVARIABLE:
                    evaluateContinuousVariable(groupDef, subject);
                    break;
                case COHORT:
                    evaluateCohort(groupDef, subject);
                    break;
            }
        }
    }

    protected void evaluateSdes(Set<SdeDef> sdes) {
        for (SdeDef sde : sdes) {
            Object result = this.context.resolveExpressionRef(sde.getExpression()).evaluate(this.context);

            flattenAdd(sde.getValues(), result);
        }
    }

    protected void evaluateStratifiers(SubjectT subject, List<StratifierDef> stratifierDefs) {
        for (StratifierDef sd : stratifierDefs) {
            if (sd.getComponents().size() > 0) {
                throw new UnsupportedOperationException("multi-component stratifiers are not yet supported.");
            }

            Object result = this.context.resolveExpressionRef(sd.getExpression()).evaluate(this.context);

            Set<String> subjectIds = sd.getValues().computeIfAbsent(result, k -> new HashSet<>());
            subjectIds.add(this.getId.apply(subject));
        }
    }

    protected void flattenAdd(List<Object> values, Object item) {
        if (item == null) {
            return;
        }

        if (item instanceof Iterable) {
            for (Object o : (Iterable<?>)item) {
                flattenAdd(values, o);
            }
        } else {
            values.add(item);
        }
    }
}
