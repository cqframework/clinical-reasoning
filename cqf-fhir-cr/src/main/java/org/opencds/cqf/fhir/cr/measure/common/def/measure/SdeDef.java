package org.opencds.cqf.fhir.cr.measure.common.def.measure;

import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;

/**
 * Immutable definition of a FHIR Measure Supplemental Data Element (SDE) structure.
 * Contains only the SDE's structural metadata (id, code, expression).
 * Does NOT contain evaluation state like results - use SdeReportDef for that.
 *
 * Converted to record by Claude Sonnet 4.5 on 2025-12-15.
 */
public record SdeDef(String id, ConceptDef code, String expression) {}
