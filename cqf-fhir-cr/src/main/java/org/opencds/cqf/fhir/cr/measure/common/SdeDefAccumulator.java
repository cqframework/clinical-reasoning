package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Version-agnostic accumulator that aggregates SDE results across subjects
 * into pre-computed maps on SdeDef objects. Follows the same pattern as
 * {@link MeasureReportDefScorer}: a stateless processor that mutates Def objects,
 * allowing builders to simply convert Def → FHIR.
 */
public class SdeDefAccumulator {

    private SdeDefAccumulator() {
        // this is a static class
        throw new IllegalAccessError("Utility class");
    }

    public static void accumulate(MeasureDef measureDef) {
        for (SdeDef sde : measureDef.sdes()) {
            sde.accumulate();
        }
    }
}
