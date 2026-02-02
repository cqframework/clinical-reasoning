package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures logic for measure evaluation associated with MEASUREOBSERVATION populations, capturing
 * both continuous variable and ratio continuous variable measures.
 */
public class MeasureObservationHandler {
    private static final Logger logger = LoggerFactory.getLogger(MeasureObservationHandler.class);

    private MeasureObservationHandler() {
        // static class with private constructor
    }

    /**
     * Removes observation entries from measureObservationDef when their resource keys
     * match resources in measurePopulationExclusionDef for the given subject.
     * <p/>
     * The observation population stores Map&lt;Resource, QuantityDef&gt; entries. This method
     * removes map entries whose keys match exclusion resources using FHIR resource identity
     * (resource type + logical ID) rather than object instance equality.
     *
     * @param subjectId the subject ID
     * @param measurePopulationExclusionDef population containing resources to exclude (e.g., cancelled encounters)
     * @param measureObservationDef population containing observation maps to filter
     */
    static void removeObservationResourcesInPopulation(
            String subjectId, PopulationDef measurePopulationExclusionDef, PopulationDef measureObservationDef) {

        if (measureObservationDef == null || measurePopulationExclusionDef == null) {
            return;
        }

        final Set<Object> exclusionResources = measurePopulationExclusionDef.getResourcesForSubject(subjectId);
        if (CollectionUtils.isEmpty(exclusionResources)) {
            return;
        }

        final Set<Object> observationResources = measureObservationDef.getResourcesForSubject(subjectId);
        if (CollectionUtils.isEmpty(observationResources)) {
            return;
        }

        logger.debug(
                "Removing {} exclusion resources from {} observation maps for subject {}",
                exclusionResources.size(),
                observationResources.size(),
                subjectId);

        // Make a copy to avoid ConcurrentModificationException when removeExcludedMeasureObservationResource
        // removes empty maps from the original set
        final Set<Object> observationResourcesCopy = new HashSetForFhirResourcesAndCqlTypes<>(observationResources);

        // Iterate over observation resources (which are Maps) and remove matching keys
        for (Object observationResource : observationResourcesCopy) {
            if (observationResource instanceof Map<?, ?> observationMap) {
                removeMatchingKeysFromObservationMap(
                        observationMap, exclusionResources, measureObservationDef, subjectId);
            }
        }
    }

    /**
     * Removes keys from an observation map that match exclusion resources.
     * <p/>
     * This method uses FHIR resource identity (resource type + logical ID) for matching
     * rather than object instance equality, since the exclusion resources and observation
     * map keys may be separate Java object instances representing the same FHIR resource.
     *
     * @param observationMap observation map containing Resource -> QuantityDef entries
     * @param exclusionResources set of resources to exclude
     * @param measureObservationDef the observation population definition
     * @param subjectId the subject ID
     */
    private static void removeMatchingKeysFromObservationMap(
            Map<?, ?> observationMap,
            Set<Object> exclusionResources,
            PopulationDef measureObservationDef,
            String subjectId) {

        // Find observation map keys that match any exclusion resource
        for (Object exclusionResource : exclusionResources) {
            // Check if this exclusion resource matches any key in the observation map
            // Must use custom equality that compares FHIR resource identity, not object instance
            boolean matchFound = observationMap.keySet().stream()
                    .anyMatch(mapKey -> FhirResourceAndCqlTypeUtils.areObjectsEqual(mapKey, exclusionResource));

            if (matchFound) {
                logger.debug(
                        "Removing observation for excluded resource: {}",
                        EvaluationResultFormatter.formatResource(exclusionResource));
                // Remove the entry from the inner map using the PopulationDef's removal method
                // This ensures proper handling of the Map<String, Set<Map<Resource, QuantityDef>>> structure
                measureObservationDef.removeExcludedMeasureObservationResource(subjectId, exclusionResource);
            }
        }
    }
}
