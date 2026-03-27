package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * Version-agnostic request parameters for a $evaluate-measure invocation.
 *
 * <p>Captures all caller-supplied operation parameters that describe <em>what</em> to evaluate and
 * how the output should be attributed. Infrastructure inputs (endpoints, data sources) live in
 * {@link MeasureEnvironment}; this record holds only the domain parameters.
 *
 * <p>Period strings from the transport layer must be parsed to {@link ZonedDateTime} by the
 * transport adapter before this record is constructed. Null periods signal "use the default from
 * the CQL measure definition".
 *
 * @param periodStart    start of the measurement period; null defers to CQL
 * @param periodEnd      end of the measurement period; null defers to CQL
 * @param reportType     type of MeasureReport to generate (e.g. "subject", "population"); null
 *                       defaults to population-level
 * @param subjectId      subject or practitioner reference for the evaluation (nullable)
 * @param practitioner   practitioner reference when distinct from subject (nullable); when set,
 *                       takes precedence over {@code subjectId} in single-measure evaluation
 * @param lastReceivedOn date results were last received; informational only (nullable)
 * @param productLine    product line for the evaluation; non-standard extension (nullable)
 * @param reporter       reporter reference to annotate on the resulting MeasureReport (nullable)
 */
public record MeasureEvaluationRequest(
        @Nullable ZonedDateTime periodStart,
        @Nullable ZonedDateTime periodEnd,
        @Nullable String reportType,
        @Nullable String subjectId,
        @Nullable String practitioner,
        @Nullable String lastReceivedOn,
        @Nullable String productLine,
        @Nullable String reporter) {}
