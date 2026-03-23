package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import org.opencds.cqf.cql.engine.runtime.Interval;

/**
 * Result of evaluating one or more measures through the shared evaluation service.
 *
 * <p>Contains per-measure scored results grouped by subject (one {@link ScoredMeasure} per
 * measure × subject-group), plus the shared evaluation context needed by outbound adapters.
 *
 * @param scoredMeasures    scored results, one per (measure, subject-group) pair
 * @param measurementPeriod the resolved measurement period interval
 * @param evalType          the resolved evaluation type
 */
public record MeasureEvaluationResults(
        List<ScoredMeasure> scoredMeasures, Interval measurementPeriod, MeasureEvalType evalType) {}
