package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * Version-agnostic request parameters for measure evaluation.
 *
 * <p>Holds the domain-typed operation parameters that describe <em>what</em> to evaluate,
 * independent of FHIR version. Period strings are parsed to {@link ZonedDateTime} by the
 * transport adapter before this record is constructed.</p>
 *
 * @param periodStart    start of the measurement period (nullable when defaulting to CQL)
 * @param periodEnd      end of the measurement period (nullable when defaulting to CQL)
 * @param reportType     the type of MeasureReport to generate (e.g. "subject", "population")
 * @param subjectId      the subject or practitioner reference for evaluation
 * @param practitioner   the practitioner reference, if distinct from subject
 * @param lastReceivedOn date results were last received (informational)
 * @param productLine    product line for the evaluation (non-standard extension)
 */
public record MeasureEvaluationRequest(
        @Nullable ZonedDateTime periodStart,
        @Nullable ZonedDateTime periodEnd,
        @Nullable String reportType,
        @Nullable String subjectId,
        @Nullable String practitioner,
        @Nullable String lastReceivedOn,
        @Nullable String productLine) {}
