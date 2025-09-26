package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Map;
import java.util.Set;

// LUKETODO:  move to own class
// LUKETODO:  evolve this contract
// LUKETODO:  unit test
class MeasureEvaluatorPopulationDefHandler {

    static <T> void retainAll(Set<T> retainer, Set<T> toRetain) {
        // Sanity check
        if (retainer == null || toRetain == null) {
            return;
        }
        retainer.retainAll(toRetain);
    }

    static void retainOverlaps(PopulationDef retainer, Map<String, Set<Object>> toRetain) {
        // Sanity check
        if (retainer == null || toRetain == null) {
            return;
        }
        retainer.retainOverlaps(toRetain);
    }

    static <T> void removeAll(Set<T> remover, Set<T> toRemove) {
        // Sanity check
        if (remover == null || toRemove == null) {
            return;
        }
        remover.removeAll(toRemove);
    }

    static void removeOverlaps(PopulationDef remover, Map<String, Set<Object>> toRemove) {
        // Sanity check
        if (remover == null || toRemove == null) {
            return;
        }
        remover.removeOverlaps(toRemove);
    }
}
