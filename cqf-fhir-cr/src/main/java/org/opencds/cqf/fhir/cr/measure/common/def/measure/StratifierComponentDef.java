package org.opencds.cqf.fhir.cr.measure.common.def.measure;

import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;

/**
 * Immutable definition of a FHIR Measure Stratifier Component structure.
 * Contains only the component's structural metadata (id, code, expression).
 * Does NOT contain evaluation state like results - use StratifierComponentReportDef for that.
 *
 * Converted to record by Claude Sonnet 4.5 on 2025-12-15.
 */
public record StratifierComponentDef(String id, ConceptDef code, String expression) {}
