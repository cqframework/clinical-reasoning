package org.opencds.cqf.fhir.cr.measure.common

import java.time.ZonedDateTime

/**
 * Version-agnostic request parameters for a $evaluate-measure invocation.
 *
 * Captures all caller-supplied operation parameters that describe *what* to evaluate and how the
 * output should be attributed. Infrastructure inputs (endpoints, data sources) live in
 * [MeasureEnvironment]; this class holds only the domain parameters.
 *
 * Period strings from the transport layer must be parsed to [ZonedDateTime] by the transport
 * adapter before this object is constructed. Null periods signal "use the default from the CQL
 * measure definition".
 *
 * The `@get:JvmName` annotations expose each property accessor under its plain name (e.g.
 * `periodStart()`) rather than the default Java-bean prefix form (`getPeriodStart()`). This keeps
 * the calling convention identical to a Java record, so existing Java callers require no changes
 * during the incremental migration to Kotlin.
 *
 * @property periodStart start of the measurement period; null defers to CQL
 * @property periodEnd end of the measurement period; null defers to CQL
 * @property reportType type of MeasureReport to generate (e.g. "subject", "population"); null
 *   defaults to population-level
 * @property subjectId subject or practitioner reference for the evaluation (nullable)
 * @property practitioner practitioner reference when distinct from subject (nullable); when set,
 *   takes precedence over [subjectId] in single-measure evaluation
 * @property lastReceivedOn date results were last received; informational only (nullable)
 * @property productLine product line for the evaluation; non-standard extension (nullable)
 * @property reporter reporter reference to annotate on the resulting MeasureReport (nullable)
 */
data class MeasureEvaluationRequest(
    @get:JvmName("periodStart") val periodStart: ZonedDateTime? = null,
    @get:JvmName("periodEnd") val periodEnd: ZonedDateTime? = null,
    @get:JvmName("reportType") val reportType: String? = null,
    @get:JvmName("subjectId") val subjectId: String? = null,
    @get:JvmName("practitioner") val practitioner: String? = null,
    @get:JvmName("lastReceivedOn") val lastReceivedOn: String? = null,
    @get:JvmName("productLine") val productLine: String? = null,
    @get:JvmName("reporter") val reporter: String? = null,
)
