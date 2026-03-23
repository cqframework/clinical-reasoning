package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;

/**
 * The version-agnostic result of evaluating and scoring a single measure.
 * Contains everything needed to build a version-specific MeasureReport without referencing any
 * FHIR version types.
 *
 * <p>The shared service creates one ScoredMeasure per ResolvedMeasure, containing all evaluated
 * subjects. Per-subject splitting (for individual reports) is handled by the version-specific
 * outbound adapter.
 *
 * @param measureDef the measure definition used for this evaluation
 * @param state      the evaluation state containing scored results and any errors
 * @param subjects   the subject IDs this score applies to
 */
public record ScoredMeasure(MeasureDef measureDef, MeasureEvaluationState state, List<String> subjects) {}
