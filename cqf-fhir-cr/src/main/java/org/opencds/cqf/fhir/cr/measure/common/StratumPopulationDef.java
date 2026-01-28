package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

/**
 * Equivalent to the FHIR stratum population.
 *
 * This is meant to be the source of truth for all data points regarding stratum populations.
 */
public record StratumPopulationDef(
        PopulationDef populationDef,
        /*
         * The subjectIds as they are, whether they are qualified with a resource
         * (ex: [Patient/pat1, Patient/pat2] or [pat1, pat2]
         */
        Set<String> subjectsQualifiedOrUnqualified,
        Set<Object> populationDefEvaluationResultIntersection,
        List<String> resourceIdsForSubjectList,
        MeasureStratifierType measureStratifierType,
        CodeDef populationBasis) {

    /**
     * Get the ID from the associated PopulationDef.
     * @return the population ID, or null if populationDef is null
     */
    @Nullable
    public String id() {
        return populationDef != null ? populationDef.id() : null;
    }

    /**
     * @return The subjectIds without a FHIR resource qualifier, whether they previously had a
     * qualifier or not
     */
    public Set<String> getSubjectsUnqualified() {
        return FhirResourceUtils.stripAnyResourceQualifiersAsSet(subjectsQualifiedOrUnqualified);
    }

    public boolean isBooleanBasis() {
        return populationBasis.code().equals("boolean");
    }

    /**
     * Returns the resource IDs as an immutable Set for efficient lookup/intersection operations.
     * <p>
     * This is useful when you need to check membership or perform set operations
     * on the resource IDs, rather than iterating through the list.
     *
     * @return an immutable Set view of the resource IDs
     */
    @Nonnull
    public Set<String> resourceIdsAsSet() {
        return Set.copyOf(resourceIdsForSubjectList);
    }

    // Enhanced by Claude Sonnet 4.5 to properly handle count calculation for all stratifier types
    public int getCount() {
        // For criteria stratifiers, use the intersection count
        if (MeasureStratifierType.CRITERIA == measureStratifierType) {
            return populationDefEvaluationResultIntersection.size();
        }

        // For boolean basis (non-criteria), count is based on subjects
        if (isBooleanBasis()) {
            return subjectsQualifiedOrUnqualified.size();
        }

        // For resource basis (non-criteria), count is based on resources
        return resourceIdsForSubjectList.size();
    }

    @Nonnull
    @Override
    public String toString() {
        String popDefStr = (populationDef != null) ? populationDef.toString() : "null";
        return "StratumPopulationDef{"
                + "populationDef=" + popDefStr
                + ", measureStratifierType=" + measureStratifierType
                + ", populationBasis=" + populationBasis.code()
                + ", subjectsQualifiedOrUnqualified=" + limitCollection(subjectsQualifiedOrUnqualified)
                + ", resourceIdsForSubjectList=" + limitCollection(resourceIdsForSubjectList)
                + ", populationDefEvaluationResultIntersection=" + formatEvaluationResults()
                + '}';
    }

    private static <T> String limitCollection(Iterable<T> collection) {
        if (collection == null) {
            return "null";
        }
        var iterator = collection.iterator();
        var items = new java.util.ArrayList<T>();
        int count = 0;
        while (iterator.hasNext() && count < 5) {
            items.add(iterator.next());
            count++;
        }
        String result = items.toString();
        if (iterator.hasNext()) {
            result = result.substring(0, result.length() - 1) + ", ...]";
        }
        return result;
    }

    private String formatEvaluationResults() {
        if (populationDefEvaluationResultIntersection == null) {
            return "null";
        }

        var limited = populationDefEvaluationResultIntersection.stream()
                .limit(5)
                .map(obj -> {
                    if (obj instanceof IBaseResource resource) {
                        return resource.getIdElement().getValueAsString();
                    } else if (obj instanceof IBase base) {
                        return base.fhirType();
                    } else {
                        return obj.toString();
                    }
                })
                .toList();

        String result = limited.toString();
        if (populationDefEvaluationResultIntersection.size() > 5) {
            result = result.substring(0, result.length() - 1) + ", ...]";
        }
        return result;
    }
}
