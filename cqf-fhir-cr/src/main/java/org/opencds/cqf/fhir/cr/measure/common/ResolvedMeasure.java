package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.elm.r1.VersionedIdentifier;

/**
 * A Measure that has been fully translated from version-specific FHIR types into
 * version-agnostic domain types. This is the inbound contract of the shared evaluation
 * pipeline: all version-specific resolution (Measure lookup, MeasureDef building,
 * library resolution) happens before this record is constructed.
 *
 * @param measureDef        the version-agnostic measure definition (groups, populations, SDEs)
 * @param libraryIdentifier the CQL library to evaluate for this measure
 * @param url               the canonical URL of the measure (for logging and error messages)
 */
public record ResolvedMeasure(MeasureDef measureDef, VersionedIdentifier libraryIdentifier, String url) {}
