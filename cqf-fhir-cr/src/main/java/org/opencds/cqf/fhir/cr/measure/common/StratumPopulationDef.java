package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

/**
 * Equivalent to the FHIR stratum population.
 *
 * This is meant to be the source of truth for all data points regarding stratum populations.
 * <p/>
 * Converted from record to class to support mutable aggregationResult field
 * for persisting per-stratum observation aggregates needed by downstream aggregation.
 */
public class StratumPopulationDef {

    private final PopulationDef populationDef;
    /*
     * The subjectIds as they are, whether they are qualified with a resource
     * (ex: [Patient/pat1, Patient/pat2] or [pat1, pat2]
     */
    private final Set<String> subjectsQualifiedOrUnqualified;
    private final Set<Object> populationDefEvaluationResultIntersection;
    private final List<String> resourceIdsForSubjectList;
    private final MeasureStratifierType measureStratifierType;
    private final CodeDef populationBasis;

    // Mutable field for per-stratum aggregation result
    private Double aggregationResult;

    public StratumPopulationDef(
            PopulationDef populationDef,
            Set<String> subjectsQualifiedOrUnqualified,
            Set<Object> populationDefEvaluationResultIntersection,
            List<String> resourceIdsForSubjectList,
            MeasureStratifierType measureStratifierType,
            CodeDef populationBasis) {
        this.populationDef = populationDef;
        this.subjectsQualifiedOrUnqualified = subjectsQualifiedOrUnqualified;
        this.populationDefEvaluationResultIntersection = populationDefEvaluationResultIntersection;
        this.resourceIdsForSubjectList = resourceIdsForSubjectList;
        this.measureStratifierType = measureStratifierType;
        this.populationBasis = populationBasis;
    }

    // Record-style accessor methods (maintain compatibility)
    public PopulationDef populationDef() {
        return populationDef;
    }

    public Set<String> subjectsQualifiedOrUnqualified() {
        return subjectsQualifiedOrUnqualified;
    }

    public Set<Object> populationDefEvaluationResultIntersection() {
        return populationDefEvaluationResultIntersection;
    }

    public List<String> resourceIdsForSubjectList() {
        return resourceIdsForSubjectList;
    }

    public MeasureStratifierType measureStratifierType() {
        return measureStratifierType;
    }

    public CodeDef populationBasis() {
        return populationBasis;
    }

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

    /**
     * Get the per-stratum aggregation result computed during scoring.
     * This is the intermediate observation aggregate needed for downstream distributed aggregation.
     *
     * @return the aggregation result, or null if not computed
     */
    @Nullable
    public Double getAggregationResult() {
        return aggregationResult;
    }

    /**
     * Set the per-stratum aggregation result.
     * Called by MeasureReportDefScorer to persist intermediate observation aggregates
     * that would otherwise be discarded.
     *
     * @param aggregationResult the computed aggregation result
     */
    public void setAggregationResult(@Nullable Double aggregationResult) {
        this.aggregationResult = aggregationResult;
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
        var items = new ArrayList<T>();
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
