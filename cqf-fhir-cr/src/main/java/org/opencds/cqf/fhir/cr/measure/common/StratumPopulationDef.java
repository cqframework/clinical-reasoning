package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
        CodeDef populationBasis) {

    /**
     * @return The subjectIds without a FHIR resource qualifier, whether they previously had a
     * qualifier or not
     */
    public Set<String> getSubjectsUnqualified() {
        return ResourceIdUtils.stripAnyResourceQualifiersAsSet(subjectsQualifiedOrUnqualified);
    }

    public boolean isBooleanBasis() {
        return populationBasis.code().equals("boolean");
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

    @Override
    public String toString() {
        return "StratumPopulationDef{"
                + "id='" + id + '\''
                + ", measureStratifierType=" + measureStratifierType
                + ", populationBasis=" + populationBasis.code()
                + ", subjectsQualifiedOrUnqualified=" + limitCollection(subjectsQualifiedOrUnqualified, 5)
                + ", resourceIdsForSubjectList=" + limitCollection(resourceIdsForSubjectList, 5)
                + ", populationDefEvaluationResultIntersection=" + formatEvaluationResults()
                + '}';
    }

    private static <T> String limitCollection(Iterable<T> collection, int limit) {
        if (collection == null) {
            return "null";
        }
        var iterator = collection.iterator();
        var items = new java.util.ArrayList<T>();
        int count = 0;
        while (iterator.hasNext() && count < limit) {
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
                .collect(Collectors.toList());

        String result = limited.toString();
        if (populationDefEvaluationResultIntersection.size() > 5) {
            result = result.substring(0, result.length() - 1) + ", ...]";
        }
        return result;
    }
}
