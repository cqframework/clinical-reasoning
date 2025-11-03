package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4ResourceIdUtils;

/**
 * Equivalent to the FHIR stratum population.
 * <p/>
 * This is meant to be the source of truth for all data points regarding stratum populations.
 */
public class StratumPopulationDef {

    private final String id;
    private final Set<String> subjectsQualifiedOrUnqualified;
    private final Set<Object> populationDefEvaluationResultIntersection;
    // Temporary:  figure out what to do with this
    private final List<String> resourceIds;
    private final MeasureStratifierType measureStratifierType;

    public StratumPopulationDef(
            String id,
            Set<String> subjectsQualifiedOrUnqualified,
            Set<Object> populationDefEvaluationResultIntersection,
            List<String> resourceIds,
            MeasureStratifierType measureStratifierType) {
        this.id = id;
        this.subjectsQualifiedOrUnqualified = Set.copyOf(subjectsQualifiedOrUnqualified);
        this.populationDefEvaluationResultIntersection = populationDefEvaluationResultIntersection;
        this.resourceIds = resourceIds;
        this.measureStratifierType = measureStratifierType;
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

    public Set<Object> getPopulationDefEvaluationResultIntersection() {
        return populationDefEvaluationResultIntersection;
    }

    // LUKETODO:  javadoc
    public int getCount() {
        if (MeasureStratifierType.CRITERIA == measureStratifierType) {
            return populationDefEvaluationResultIntersection.size();
        }

        return resourceIds.size();
    }

    public List<String> getResourceIds() {
        return resourceIds;
    }

    // LUKETODO: toString
}
