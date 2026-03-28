package org.opencds.cqf.fhir.cr.measure.common

/**
 * Composed request for a $evaluate-measure invocation.
 *
 * Separates *who* to evaluate ([subject]) from *how* to evaluate ([parameters]). Subject expansion
 * (reference → concrete patient list) is a repository operation performed by the service layer
 * before calling the evaluation core; it is not part of this record.
 *
 * @property subject the subject reference to evaluate; null inside [MeasureSubject] means all
 *   patients
 * @property parameters evaluation parameters: period, report type, and output metadata
 */
data class MeasureEvaluationRequest(
    @get:JvmName("subject") val subject: MeasureSubject = MeasureSubject(null),
    @get:JvmName("parameters")
    val parameters: MeasureEvaluationParameters = MeasureEvaluationParameters(),
)
