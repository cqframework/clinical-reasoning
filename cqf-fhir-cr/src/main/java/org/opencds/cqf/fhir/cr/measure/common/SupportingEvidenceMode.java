package org.opencds.cqf.fhir.cr.measure.common;

/**
 * How much supporting evidence a MeasureReport carries. Governs what reaches the report, not what
 * the CQL engine evaluates.
 */
public enum SupportingEvidenceMode {
    /** No supporting evidence is written, including evidence the measure declares. */
    OFF,
    /** Only evidence declared on a population through {@code cqf-supportingEvidenceDefinition}. */
    DECLARED,
    /** Declared evidence, plus one report-level entry per other evaluated expression. */
    ALL_EXPRESSIONS
}
