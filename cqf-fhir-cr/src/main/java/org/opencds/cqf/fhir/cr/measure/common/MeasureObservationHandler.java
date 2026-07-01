package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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

        final var exclusionResources = measurePopulationExclusionDef.getResourcesForSubject(subjectId);
        if (exclusionResources == null || exclusionResources.isEmpty()) {
            return;
        }

        final var observationResources = measureObservationDef.getResourcesForSubject(subjectId);
        if (observationResources == null || observationResources.isEmpty()) {
            return;
        }

        logger.debug(
                "Removing {} exclusion resources from {} observation maps for subject {}",
                exclusionResources.size(),
                observationResources.size(),
                subjectId);

        // Make a copy to avoid ConcurrentModificationException when removeExcludedMeasureObservationResource
        // removes empty maps from the original set
        final var observationResourcesCopy = new HashSetForCqlExpressionValues(observationResources);

        // Each observation wrapper holds the subject's ObservationAccumulator (or raw Map) of
        // input-resource -> observation entries. Match each input resource against the exclusion
        // resources and remove the matching ones.
        for (CqlExpressionValue observationResource : observationResourcesCopy) {
            if (observationResource == null) {
                continue;
            }
            removeMatchingObservationInputs(
                    observationResource.observationInputs(), exclusionResources, measureObservationDef, subjectId);
        }
    }

    /**
     * Removes observation entries whose input resource matches an exclusion resource.
     * <p/>
     * This method uses FHIR resource identity (resource type + logical ID) for matching
     * rather than object instance equality, since the exclusion resources and observation
     * input resources may be separate Java object instances representing the same FHIR resource.
     * Both sides are unwrapped to their raw FHIR/CQL value before comparison, since
     * {@code exclusionResources} are {@link CqlExpressionValue} wrappers.
     *
     * @param observationInputs the input resources of one subject's observation accumulator
     * @param exclusionResources set of resource wrappers to exclude
     * @param measureObservationDef the observation population definition
     * @param subjectId the subject ID
     */
    private static void removeMatchingObservationInputs(
            List<Object> observationInputs,
            Set<CqlExpressionValue> exclusionResources,
            PopulationDef measureObservationDef,
            String subjectId) {

        for (CqlExpressionValue exclusionWrapper : exclusionResources) {
            final Object exclusionResource = exclusionWrapper == null ? null : exclusionWrapper.raw();
            if (exclusionResource == null) {
                continue;
            }

            // Check if this exclusion resource matches any input in the observation accumulator.
            // Must use custom equality that compares FHIR resource identity, not object instance.
            boolean matchFound = observationInputs.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(input -> FhirResourceAndCqlTypeUtils.areObjectsEqual(input, exclusionResource));

            if (matchFound) {
                logger.debug(
                        "Removing observation for excluded resource: {}",
                        EvaluationResultFormatter.formatResource(exclusionResource));
                // Remove the entry from the accumulator using the PopulationDef's removal method
                // This ensures proper handling of the Map<String, Set<ObservationAccumulator>> structure
                measureObservationDef.removeExcludedMeasureObservationResource(subjectId, exclusionResource);
            }
        }
    }
}
