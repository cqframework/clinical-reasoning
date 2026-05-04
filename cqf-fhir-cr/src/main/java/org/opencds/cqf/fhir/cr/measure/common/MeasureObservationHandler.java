package org.opencds.cqf.fhir.cr.measure.common;

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

        final Set<CqlExpressionValue> exclusionResources =
                measurePopulationExclusionDef.getResourcesForSubject(subjectId);
        if (CollectionUtils.isEmpty(exclusionResources)) {
            return;
        }

        final Set<CqlExpressionValue> observationResources = measureObservationDef.getResourcesForSubject(subjectId);
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
        final Set<CqlExpressionValue> observationResourcesCopy =
                new HashSetForCqlExpressionValues(observationResources);

        // Iterate observation accumulators and drop entries whose input matches any exclusion
        for (CqlExpressionValue observationResource : observationResourcesCopy) {
            observationResource
                    .asObservationAccumulator()
                    .ifPresent(acc -> removeMatchingEntriesFromObservationAccumulator(
                            acc, exclusionResources, measureObservationDef, subjectId));
        }
    }

    /**
     * Drops entries from an observation accumulator whose input matches an exclusion resource.
     * <p/>
     * Uses FHIR resource identity (resource type + logical ID) for matching rather than object
     * instance equality, since the exclusion resources and observation entry inputs may be
     * separate Java object instances representing the same FHIR resource.
     */
    private static void removeMatchingEntriesFromObservationAccumulator(
            ObservationAccumulator accumulator,
            Set<CqlExpressionValue> exclusionResources,
            PopulationDef measureObservationDef,
            String subjectId) {

        for (CqlExpressionValue exclusionResource : exclusionResources) {
            if (exclusionResource == null) {
                continue;
            }
            Object exclusionRaw = exclusionResource.raw();
            boolean matchFound = accumulator.entries().stream()
                    .anyMatch(
                            entry -> FhirResourceAndCqlTypeUtils.areObjectsEqual(entry.inputResource(), exclusionRaw));

            if (matchFound) {
                logger.atDebug().log(
                        "Removing observation for excluded resource: {}",
                        EvaluationResultFormatter.formatResource(exclusionRaw));
                // Delegate to PopulationDef so empty accumulators get purged from the subject set
                measureObservationDef.removeExcludedMeasureObservationResource(subjectId, exclusionRaw);
            }
        }
    }
}
