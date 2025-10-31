package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.List;
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
    // Temporary:  this needs to be captured as number of intersected resources
    private int count = 0;
    // Temporary:  figure out what to do with this
    private List<String> resourceIds = new ArrayList<>();

    public StratumPopulationDef(String id, Set<String> subjectsQualifiedOrUnqualified) {
        this.id = id;
        this.subjectsQualifiedOrUnqualified = Set.copyOf(subjectsQualifiedOrUnqualified);
    }

    public String getId() {
        return id;
    }

    /**
     * @return The subjectIds as they are, whether they are qualified with a resource
     * (ex: [Patient/pat1, Patient/pat2] or [pat1, pat2]
     */
    public Set<String> getSubjectsQualifiedOrUnqualified() {
        return subjectsQualifiedOrUnqualified;
    }

    /**
     * @return The subjectIds without a FHIR resource qualifier, whether they previously had a
     * qualifier or not
     */
    public Set<String> getSubjectsUnqualified() {
        return subjectsQualifiedOrUnqualified.stream()
                .filter(Objects::nonNull)
                .map(R4ResourceIdUtils::stripAnyResourceQualifier)
                .collect(Collectors.toUnmodifiableSet());
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getResourceIds() {
        return resourceIds;
    }

    public void addAllResourceIds(List<String> resourceIds) {
        this.resourceIds.addAll(resourceIds);
    }

    // LUKETODO: toString
}
