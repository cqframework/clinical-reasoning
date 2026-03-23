package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

/**
 * Helpers for filtering evaluation results by stratum membership.
 * Used by scoring strategies that need stratum-level resource filtering.
 */
final class StratumResultsHelper {

    private StratumResultsHelper() {}

    /**
     * Get results filtered for a specific stratum, reading subject resources from state.
     *
     * @param populationDef the population definition (MEASUREOBSERVATION)
     * @param stratumPopulationDef the stratum population to filter by
     * @param state the mutable evaluation state
     * @return collection of resources belonging to this stratum
     */
    static Collection<Object> getResultsForStratum(
            PopulationDef populationDef, StratumPopulationDef stratumPopulationDef, MeasureEvaluationState state) {

        if (stratumPopulationDef == null || populationDef == null) {
            return List.of();
        }

        Map<String, Set<Object>> subjectResources =
                state.population(populationDef).getSubjectResources();
        if (subjectResources == null) {
            return List.of();
        }

        // For NON_SUBJECT_VALUE stratifiers with non-boolean basis, we must filter by resource IDs.
        if (stratumPopulationDef.measureStratifierType() == MeasureStratifierType.NON_SUBJECT_VALUE
                && !stratumPopulationDef.isBooleanBasis()) {

            if (stratumPopulationDef.resourceIdsForSubjectList() == null
                    || stratumPopulationDef.resourceIdsForSubjectList().isEmpty()) {
                return List.of();
            }

            return getResultsForStratumByResourceIds(subjectResources, populationDef, stratumPopulationDef);
        }

        // Subject-based stratification (VALUE stratifiers, boolean basis, etc.)
        Set<String> stratumSubjectsUnqualified = stratumPopulationDef.getSubjectsUnqualified();

        return subjectResources.entrySet().stream()
                .filter(entry -> stratumSubjectsUnqualified.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Get results filtered for a NON_SUBJECT_VALUE stratum by resource IDs.
     * For resource-basis stratifiers, the stratum contains specific resource IDs that should be included.
     *
     * @param subjectResources the subject resources map (from state or PopulationDef)
     * @param populationDef the population definition (MEASUREOBSERVATION)
     * @param stratumPopulationDef the stratum population containing resource IDs
     * @return collection of resources/observations matching the stratum's resource IDs
     */
    private static Collection<Object> getResultsForStratumByResourceIds(
            Map<String, Set<Object>> subjectResources,
            PopulationDef populationDef,
            StratumPopulationDef stratumPopulationDef) {

        Set<String> stratumResourceIds = stratumPopulationDef.resourceIdsAsSet();

        // For MEASUREOBSERVATION, subjectResources contains Set<Map<inputResource, outputValue>>
        // MeasureScoreCalculator.collectQuantities expects Map objects and extracts values from them.
        // We need to return filtered Maps (not the values directly) so collectQuantities can process them.
        if (populationDef.type() == MeasurePopulationType.MEASUREOBSERVATION) {
            return subjectResources.values().stream()
                    .flatMap(Collection::stream)
                    .filter(Map.class::isInstance)
                    .map(m -> (Map<?, ?>) m)
                    .map(map -> {
                        // Filter the map to only include entries matching stratum resource IDs
                        Map<Object, Object> filteredMap = new java.util.HashMap<>();
                        for (var entry : map.entrySet()) {
                            Object key = entry.getKey();
                            if (key instanceof IBaseResource baseResource) {
                                String resourceId = baseResource
                                        .getIdElement()
                                        .toVersionless()
                                        .getValue();
                                if (stratumResourceIds.contains(resourceId)) {
                                    filteredMap.put(key, entry.getValue());
                                }
                            }
                        }
                        return filteredMap;
                    })
                    .filter(map -> !map.isEmpty()) // Only include non-empty filtered maps
                    .collect(Collectors.toList());
        }

        // For non-MEASUREOBSERVATION populations, filter resources directly
        return subjectResources.values().stream()
                .flatMap(Collection::stream)
                .filter(resource -> {
                    if (resource instanceof IBaseResource baseResource) {
                        String resourceId =
                                baseResource.getIdElement().toVersionless().getValue();
                        return stratumResourceIds.contains(resourceId);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
}
