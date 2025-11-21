package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Set;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

/**
 * Equivalent to the FHIR stratum population.
 * <p/>
 * This is meant to be the source of truth for all data points regarding stratum populations.
 */
public record StratumPopulationDef(
        String id,
        /*
         * The subjectIds as they are, whether they are qualified with a resource
         * (ex: [Patient/pat1, Patient/pat2] or [pat1, pat2]
         */
        Set<String> subjectsQualifiedOrUnqualified,
        Set<Object> populationDefEvaluationResultIntersection,
        List<String> resourceIdsForSubjectList,
        MeasureStratifierType measureStratifierType,
        boolean isBooleanBasis) {

    /**
     * @return The subjectIds without a FHIR resource qualifier, whether they previously had a
     * qualifier or not
     */
    public Set<String> getSubjectsUnqualified() {
        return ResourceIdUtils.stripAnyResourceQualifiersAsSet(subjectsQualifiedOrUnqualified);
    }

    // Enhanced by Claude Sonnet 4.5 to properly handle count calculation for all stratifier types
    public int getCount() {
        // For criteria stratifiers, use the intersection count
        if (MeasureStratifierType.CRITERIA == measureStratifierType) {
            return populationDefEvaluationResultIntersection != null
                    ? populationDefEvaluationResultIntersection.size()
                    : 0;
        }

        // For boolean basis (non-criteria), count is based on subjects
        if (isBooleanBasis) {
            return subjectsQualifiedOrUnqualified.size();
        }

        // For resource basis (non-criteria), count is based on resources
        return resourceIdsForSubjectList != null ? resourceIdsForSubjectList.size() : 0;
    }

    // LUKETODO: toString
}
