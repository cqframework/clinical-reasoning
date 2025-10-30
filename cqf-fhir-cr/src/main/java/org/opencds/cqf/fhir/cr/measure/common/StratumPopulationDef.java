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
public class StratumPopulationDef {

    private final String id;
    private final Set<String> subjectsQualifiedOrUnqualified;

    public StratumPopulationDef(String id, Set<String> subjectsQualifiedOrUnqualified) {
        this.id = id;
        this.subjectsQualifiedOrUnqualified = Set.copyOf(subjectsQualifiedOrUnqualified);
    }

    public String getId() {
        return id;
    }

    // LUKETODO:  javadoc
    public Set<String> getSubjectsQualifiedOrUnqualified() {
        return subjectsQualifiedOrUnqualified;
    }

    // LUKETODO:  javadoc
    public Set<String> getSubjectsUnqualified() {
        return subjectsQualifiedOrUnqualified.stream()
                .filter(Objects::nonNull)
                .map(R4ResourceIdUtils::stripAnyResourceQualifier)
                .collect(Collectors.toUnmodifiableSet());
    }
}
