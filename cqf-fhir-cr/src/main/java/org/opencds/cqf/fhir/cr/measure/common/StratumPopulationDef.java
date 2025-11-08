package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4ResourceIdUtils;

/**
 * Equivalent to the FHIR stratum population.
 * <p/>
 * This is meant to be the source of truth for all data points regarding stratum populations.
 */
public record StratumPopulationDef(String id, Set<String> subjectsQualifiedOrUnqualified) {

    public StratumPopulationDef(String id, Set<String> subjectsQualifiedOrUnqualified) {
        this.id = id;
        // subjects either with or without the resource qualifier (ex Patient/123 or 123)
        this.subjectsQualifiedOrUnqualified = Set.copyOf(subjectsQualifiedOrUnqualified);
    }

    /**
     * @return subjects always without the resource qualifier (ex 123 for Patient/123)
     */
    public Set<String> getSubjectsUnqualified() {
        return subjectsQualifiedOrUnqualified.stream()
                .filter(Objects::nonNull)
                .map(R4ResourceIdUtils::stripAnyResourceQualifier)
                .collect(Collectors.toUnmodifiableSet());
    }
}
