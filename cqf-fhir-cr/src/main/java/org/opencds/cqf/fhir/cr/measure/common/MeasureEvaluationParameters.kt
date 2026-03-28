package org.opencds.cqf.fhir.cr.measure.common

import java.time.ZonedDateTime

/**
 * Evaluation-specific parameters for a $evaluate-measure invocation.
 *
 * Captures the period, report type, and output metadata. Subject identification lives in
 * [MeasureSubject]. Together they compose [MeasureEvaluationRequest].
 *
 * The `@get:JvmName` annotations expose each property under its plain name (e.g. `periodStart()`)
 * rather than the default Java-bean prefix form (`getPeriodStart()`), matching the calling
 * convention of a Java record for existing Java callers.
 *
 * @property periodStart start of the measurement period; null defers to CQL
 * @property periodEnd end of the measurement period; null defers to CQL
 * @property reportType type of MeasureReport to generate (e.g. "subject", "population"); null
 *   defaults to population-level
 * @property lastReceivedOn date results were last received; informational only (nullable)
 * @property productLine product line for the evaluation; non-standard extension (nullable)
 * @property reporter reporter reference to annotate on the resulting MeasureReport (nullable)
 */
data class MeasureEvaluationParameters(
    @get:JvmName("periodStart") val periodStart: ZonedDateTime? = null,
    @get:JvmName("periodEnd") val periodEnd: ZonedDateTime? = null,
    @get:JvmName("reportType") val reportType: String? = null,
    @get:JvmName("lastReceivedOn") val lastReceivedOn: String? = null,
    @get:JvmName("productLine") val productLine: String? = null,
    @get:JvmName("reporter") val reporter: String? = null,
)
