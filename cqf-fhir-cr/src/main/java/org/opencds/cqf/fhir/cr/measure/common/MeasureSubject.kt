package org.opencds.cqf.fhir.cr.measure.common

/**
 * An unexpanded subject reference for measure evaluation.
 *
 * [id] is a qualified FHIR reference such as `"Patient/123"`, `"Practitioner/abc"`, `"Group/g1"`,
 * or `"Organization/o1"`. Null means evaluate against all patients in the repository.
 *
 * Expansion from this reference to a concrete list of patient IDs is performed by the
 * SubjectProvider at the service layer. Callers (e.g. HAPI transport adapters) are responsible for
 * coalescing transport-level parameters (e.g. a separate `practitioner` operation param) into a
 * single [MeasureSubject] before constructing a request. Practitioner override is not a domain
 * concern: it is resolved once at the transport boundary and expressed here as a qualified
 * `Practitioner/` reference.
 */
data class MeasureSubject(@get:JvmName("id") val id: String?)
