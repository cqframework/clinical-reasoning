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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.cqframework.cql.elm.execution.IntervalTypeSpecifier;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.NamedTypeSpecifier;
import org.cqframework.cql.elm.execution.ParameterDef;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the core Measure evaluation logic that's defined in the Quality Measure
 * implementation guide and HQMF specifications. There are a number of model-independent concepts
 * such as "groups", "populations", and "stratifiers" that can be used across a number of different
 * data models including FHIR, QDM, and QICore. To the extent feasible, this class is intended to be
 * model-independent so that it can be used in any Java-based implementation of Quality Measure
 * evaluation.
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

  public MeasureDef evaluate(MeasureDef measureDef, MeasureEvalType measureEvalType,
      List<String> subjectIds, Interval measurementPeriod) {
    Objects.requireNonNull(measureDef, "measureDef is a required argument");
    Objects.requireNonNull(subjectIds, "subjectIds is a required argument");

    // default behavior is population for many subjects, individual for one subject
    if (measureEvalType == null) {
      measureEvalType =
          subjectIds.size() > 1 ? MeasureEvalType.POPULATION : MeasureEvalType.SUBJECT;
    }

    // measurementPeriod is not required, because it's often defaulted in CQL
    this.setMeasurementPeriod(measureDef, measurementPeriod);

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
      if (this.measurementPeriodParameterName != null
          && pd.getName().equals(this.measurementPeriodParameterName)) {
        return pd;
      }
    }

    return null;

  }

  protected void setMeasurementPeriod(MeasureDef measureDef, Interval measurementPeriod) {
    if (measurementPeriod == null) {
      measurementPeriod = getMeasurementPeriod();
      measureDef.setDefaultMeasurementPeriod(measurementPeriod);
    }
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

    IntervalTypeSpecifier intervalTypeSpecifier =
        (IntervalTypeSpecifier) pd.getParameterTypeSpecifier();
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

  protected void captureEvaluatedResources(Set<Object> outEvaluatedResources) {
    if (outEvaluatedResources != null && this.context.getEvaluatedResources() != null) {
      for (Object o : this.context.getEvaluatedResources()) {
        outEvaluatedResources.add(o);
      }
    }
    clearEvaluatedResources();
  }

  // reset evaluated resources followed by a context evaluation
  private void clearEvaluatedResources() {
    this.context.clearEvaluatedResources();
  }

  protected MeasureDef evaluate(MeasureDef measureDef, MeasureReportType type,
      List<String> subjectIds) {

    logger.info("Evaluating Measure {}, report type {}, with {} subject(s)", measureDef.url(),
        type.toCode(), subjectIds.size());

    MeasureScoring scoring = measureDef.scoring();
    if (scoring == null) {
      throw new RuntimeException("MeasureScoring type is required in order to calculate.");
    }

    for (String subjectId : subjectIds) {
      if (subjectId == null) {
        throw new RuntimeException("SubjectId is required in order to calculate.");
      }
      Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectId);
      String subjectTypePart = subjectInfo.getLeft();
      String subjectIdPart = subjectInfo.getRight();
      context.setContextValue(subjectTypePart, subjectIdPart);
      evaluateSubject(measureDef, scoring, subjectTypePart, subjectIdPart);
    }

    return measureDef;
  }

  protected void evaluateSubject(MeasureDef measureDef, MeasureScoring scoring, String subjectType,
      String subjectId) {
    evaluateSdes(subjectId, measureDef.sdes());
    for (GroupDef groupDef : measureDef.groups()) {
      evaluateGroup(scoring, groupDef, subjectType, subjectId);
    }
  }

  @SuppressWarnings("unchecked")
  protected Iterable<Object> evaluatePopulationCriteria(String subjectType, String subjectId,
      String criteriaExpression, Set<Object> outEvaluatedResources) {
    if (criteriaExpression == null || criteriaExpression.isEmpty()) {
      return Collections.emptyList();
    }

    Object result = this.evaluateCriteria(criteriaExpression, outEvaluatedResources);

    if (result == null) {
      return Collections.emptyList();
    }

    if (result instanceof Boolean) {
      if ((Boolean.TRUE.equals(result))) {
        Object booleanResult =
            this.context.resolveExpressionRef(subjectType).evaluate(this.context);
        clearEvaluatedResources();
        return Collections.singletonList(booleanResult);
      } else {
        return Collections.emptyList();
      }
    }

    return (Iterable<Object>) result;
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
      throw new IllegalArgumentException(String.format(
          "Measure observation %s does not reference a function definition", criteriaExpression));
    }

    Object result = null;
    context.pushWindow();
    try {
      context.push(new Variable().withName(((FunctionDef) ed).getOperand().get(0).getName())
          .withValue(resource));
      result = ed.getExpression().evaluate(context);
    } finally {
      context.popWindow();
    }

    captureEvaluatedResources(outEvaluatedResources);

    return result;
  }

  protected boolean evaluatePopulationMembership(String subjectType, String subjectId,
      PopulationDef inclusionDef, PopulationDef exclusionDef) {
    boolean inPopulation = false;
    for (Object resource : evaluatePopulationCriteria(subjectType, subjectId,
        inclusionDef.expression(), inclusionDef.getEvaluatedResources())) {
      inPopulation = true;
      inclusionDef.addResource(resource);
    }

    if (inPopulation && exclusionDef != null) {
      for (Object resource : evaluatePopulationCriteria(subjectType, subjectId,
          exclusionDef.expression(), exclusionDef.getEvaluatedResources())) {
        inPopulation = false;
        exclusionDef.addResource(resource);
        inclusionDef.removeResource(resource);
      }
    }

    if (inPopulation) {
      inclusionDef.addSubject(subjectId);
    }

    if (!inPopulation && exclusionDef != null) {
      exclusionDef.addSubject(subjectId);
    }

    return inPopulation;
  }

  protected void evaluateProportion(GroupDef groupDef, String subjectType, String subjectId) {
    // Are they in the initial population?
    boolean inInitialPopulation = evaluatePopulationMembership(subjectType, subjectId,
        groupDef.getSingle(INITIALPOPULATION), null);
    if (inInitialPopulation) {
      // Are they in the denominator?
      boolean inDenominator = evaluatePopulationMembership(subjectType, subjectId,
          groupDef.getSingle(DENOMINATOR), groupDef.getSingle(DENOMINATOREXCLUSION));

      if (inDenominator) {
        // Are they in the numerator?
        boolean inNumerator = evaluatePopulationMembership(subjectType, subjectId,
            groupDef.getSingle(NUMERATOR), groupDef.getSingle(NUMERATOREXCLUSION));

        if (!inNumerator && groupDef.getSingle(DENOMINATOREXCEPTION) != null) {
          // Are they in the denominator exception?

          PopulationDef denominatorException = groupDef.getSingle(DENOMINATOREXCEPTION);
          PopulationDef denominator = groupDef.getSingle(DENOMINATOR);
          boolean inException = false;
          for (Object resource : evaluatePopulationCriteria(subjectType, subjectId,
              denominatorException.expression(), denominatorException.getEvaluatedResources())) {
            inException = true;
            denominatorException.addResource(resource);
            denominator.removeResource(resource);

          }

          if (inException) {
            denominatorException.addSubject(subjectId);
            denominator.removeSubject(subjectId);
          }
        }
      }
    }
  }

  protected void evaluateContinuousVariable(GroupDef groupDef, String subjectType,
      String subjectId) {
    boolean inInitialPopulation = evaluatePopulationMembership(subjectType, subjectId,
        groupDef.getSingle(INITIALPOPULATION), null);

    if (inInitialPopulation) {
      // Are they in the MeasureType population?
      PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
      boolean inMeasurePopulation = evaluatePopulationMembership(subjectType, subjectId,
          measurePopulation, groupDef.getSingle(MEASUREPOPULATIONEXCLUSION));

      if (inMeasurePopulation) {
        PopulationDef measureObservation = groupDef.getSingle(MEASUREOBSERVATION);
        if (measureObservation != null) {
          for (Object resource : measurePopulation.getResources()) {
            Object observationResult = evaluateObservationCriteria(resource,
                measureObservation.expression(), measureObservation.getEvaluatedResources());
            measureObservation.addResource(observationResult);
          }
        }
      }
    }
  }

  protected void evaluateCohort(GroupDef groupDef, String subjectType, String subjectId) {
    evaluatePopulationMembership(subjectType, subjectId, groupDef.getSingle(INITIALPOPULATION),
        null);
  }

  protected void evaluateGroup(MeasureScoring measureScoring, GroupDef groupDef, String subjectType,
      String subjectId) {
    evaluateStratifiers(subjectId, groupDef.stratifiers());
    switch (measureScoring) {
      case PROPORTION:
      case RATIO:
        evaluateProportion(groupDef, subjectType, subjectId);
        break;
      case CONTINUOUSVARIABLE:
        evaluateContinuousVariable(groupDef, subjectType, subjectId);
        break;
      case COHORT:
        evaluateCohort(groupDef, subjectType, subjectId);
        break;
    }
  }

  protected void evaluateSdes(String subjectId, List<SdeDef> sdes) {
    for (SdeDef sde : sdes) {
      ExpressionDef expressionDef = this.context.resolveExpressionRef(sde.expression());
      Object result = expressionDef.evaluate(this.context);

      sde.putResult(subjectId, result, context.getEvaluatedResources());

      clearEvaluatedResources();
    }
  }

  protected void evaluateStratifiers(String subjectId, List<StratifierDef> stratifierDefs) {
    for (StratifierDef sd : stratifierDefs) {
      if (!sd.components().isEmpty()) {
        throw new UnsupportedOperationException(
            "multi-component stratifiers are not yet supported.");
      }

      // TODO: Handle list values as components?
      Object result = this.context.resolveExpressionRef(sd.expression()).evaluate(this.context);
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
        sd.putResult(subjectId, result, this.context.getEvaluatedResources());
      }

      clearEvaluatedResources();
    }
  }
}
