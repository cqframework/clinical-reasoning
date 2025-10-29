package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Capture results of component stratifier stratum calculation.
 */
public record StratumValueDef(StratumValueWrapper value, StratifierComponentDef def) {}
