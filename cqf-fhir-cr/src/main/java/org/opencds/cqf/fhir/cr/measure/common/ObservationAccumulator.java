package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;

/**
 * The bag of {@link ObservationEntry} produced for one subject by one MEASUREOBSERVATION
 * population's observation function. Conceptually a single composite value (one accumulator per
 * subject), which is why this is a record rather than a {@code List<ObservationEntry>} directly:
 * a List is {@link Iterable}, and the upstream evaluation pipeline (
 * {@code MeasureEvaluator.evaluatePopulationCriteria} → {@code CqlExpressionValue.asIterable})
 * unrolls Iterables when stashing values into {@code PopulationDef.subjectResources}. Wrapping in
 * a non-Iterable record keeps the whole accumulator as one stored value.
 */
public record ObservationAccumulator(List<ObservationEntry> entries) {

    public ObservationAccumulator {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }
}
