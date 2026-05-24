package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;

/**
 * The bag of {@link FunctionResultEntry} produced for one subject by one NON_SUBJECT_VALUE
 * stratifier component's function. A non-Iterable record wrapping the entries list so the
 * upstream {@link CqlExpressionValue#asIterable()} path doesn't unroll it; mirrors
 * {@link ObservationAccumulator}.
 */
public record FunctionResultAccumulator(List<FunctionResultEntry> entries) {

    public FunctionResultAccumulator {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }
}
