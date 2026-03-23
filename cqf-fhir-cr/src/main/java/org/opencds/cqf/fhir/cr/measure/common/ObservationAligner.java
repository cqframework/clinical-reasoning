package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Aligns measure-observation resources with their corresponding population resources.
 * Handles both retain (intersection) and remove (exclusion) operations on observation
 * subject-resource maps.
 */
public final class ObservationAligner {
    private ObservationAligner() {}

    /**
     * Keeps Measure-Observation values found in measurePopulation;
     * removes observation entries for subjects not in the population
     * and individual observations whose keys don't match population resources.
     */
    @SuppressWarnings("unchecked")
    public static void retainObservationSubjectResourcesInPopulation(
            MeasureEvaluationState.PopulationState measurePopulationState,
            MeasureEvaluationState.PopulationState measureObservationState) {

        Map<String, Set<Object>> measurePopulation = measurePopulationState.getSubjectResources();
        Map<String, Set<Object>> measureObservation = measureObservationState.getSubjectResources();

        if (measurePopulation == null || measureObservation == null) {
            return;
        }

        for (Iterator<Map.Entry<String, Set<Object>>> it =
                        measureObservation.entrySet().iterator();
                it.hasNext(); ) {
            Map.Entry<String, Set<Object>> entry = it.next();
            String subjectId = entry.getKey();

            // Cast subject's observation set to the expected type
            Set<Map<Object, Object>> obsSet = (Set<Map<Object, Object>>) (Set<?>) entry.getValue();

            // get valid population values for this subject
            Set<Object> validPopulation = measurePopulation.get(subjectId);

            if (validPopulation == null || validPopulation.isEmpty()) {
                // no population for this subject -> drop the whole subject
                it.remove();
                continue;
            }

            // remove observations not matching population values
            obsSet.removeIf(obsMap -> {
                for (Object key : obsMap.keySet()) {
                    if (!validPopulation.contains(key)) {
                        return true; // remove this observation map
                    }
                }
                return false;
            });

            // if no observations remain for this subject, remove it entirely
            if (obsSet.isEmpty()) {
                it.remove();
            }
        }
    }

    /**
     * Retains only observation resources whose keys are present in the measure population
     * for the given subject.
     */
    public static void retainObservationResourcesInPopulation(
            String subjectId,
            PopulationDef measurePopulationDef,
            PopulationDef measureObservationDef,
            MeasureEvaluationState state) {

        var obsState = state.population(measureObservationDef);
        for (Object populationResource : obsState.getResourcesForSubject(subjectId)) {
            if (populationResource instanceof Map<?, ?> measureObservationResourceAsMap) {
                for (Entry<?, ?> measureObservationResourceMapEntry : measureObservationResourceAsMap.entrySet()) {
                    final Object measureObservationSubjectResourceMapKey = measureObservationResourceMapEntry.getKey();
                    if (measurePopulationDef != null) {
                        final Set<Object> measurePopulationResourcesForSubject =
                                state.population(measurePopulationDef).getResourcesForSubject(subjectId);
                        if (!measurePopulationResourcesForSubject.contains(measureObservationSubjectResourceMapKey)) {
                            // remove observation results not found in measure population
                            obsState.getResourcesForSubject(subjectId).remove(populationResource);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes observation entries for subjects whose resources appear in the exclusion population.
     *
     * @param measurePopulationState population results to exclude from measureObservation
     * @param measureObservationState population results that will have items excluded,
     *                                if found in measurePopulation
     */
    @SuppressWarnings("unchecked")
    public static void removeObservationSubjectResourcesInPopulation(
            MeasureEvaluationState.PopulationState measurePopulationState,
            MeasureEvaluationState.PopulationState measureObservationState) {

        Map<String, Set<Object>> measurePopulation = measurePopulationState.getSubjectResources();
        Map<String, Set<Object>> measureObservation = measureObservationState.getSubjectResources();

        if (measurePopulation == null || measureObservation == null) {
            return;
        }

        for (Iterator<Map.Entry<String, Set<Object>>> it =
                        measureObservation.entrySet().iterator();
                it.hasNext(); ) {

            Map.Entry<String, Set<Object>> entry = it.next();
            String subjectId = entry.getKey();

            final Set<?> entryValue = entry.getValue();

            if (CollectionUtils.isEmpty(entryValue)) {
                continue;
            }

            removeMatchingObservationEntries(measurePopulation, entryValue, subjectId, it);
        }
    }

    /**
     * Removes individual observation map entries whose keys match population resource values
     * for the given subject. If all observations are removed, the subject entry is removed
     * from the iterator.
     */
    @SuppressWarnings("unchecked")
    static void removeMatchingObservationEntries(
            Map<String, Set<Object>> measurePopulation,
            Set<?> entryValue,
            String subjectId,
            Iterator<Entry<String, Set<Object>>> iterator) {
        if (entryValue.isEmpty()) {
            // Nothing to do
            return;
        }
        final Object firstEntryValue = entryValue.iterator().next();

        if (!(firstEntryValue instanceof Map<?, ?>)) {
            throw new MeasureEvaluationException("Expected a Map<?,?> but was not: %s".formatted(firstEntryValue));
        }

        Set<Map<Object, Object>> obsSet = (Set<Map<Object, Object>>) entryValue;

        // population values for this subject
        Set<Object> populationValues = measurePopulation.get(subjectId);

        // If there is no population for this subject, there is nothing "to remove because iterator matches",
        // so leave the observation set as-is.
        if (populationValues == null || populationValues.isEmpty()) {
            return;
        }

        // Remove observations that *do* match population values
        obsSet.removeIf(obsMap -> {
            for (Object key : obsMap.keySet()) {
                if (populationValues.contains(key)) {
                    // This observation map is backed by a population resource -> remove iterator
                    return true;
                }
            }
            return false;
        });

        // If no observations remain for this subject, remove the subject entry entirely
        if (obsSet.isEmpty()) {
            iterator.remove();
        }
    }
}
