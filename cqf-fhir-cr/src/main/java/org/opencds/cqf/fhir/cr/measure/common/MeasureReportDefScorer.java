package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluation of Measure Report Data showing raw CQL criteria results compared to resulting Measure Report.
 *
 * <p>Each row represents a subject as raw cql criteria expression output:
 *
 * <pre>{@code
 * Subject | IP | D  | DX | N  | DE | NX | Notes
 * --------|----|----|----|----|----|----|---------------------------------------------------------------
 * A       | A  | A  | A  |    |    |    |
 * B       | B  | B  |    | B  |    |    |
 * C       | C  | C  |    |    | C  |    | InDenominator = true, InDenominatorException = true,
 *                                       | InNumerator = false
 * D       | D  | D  |    | D  |    | D  |
 * E       | E  | E  |    | E  |    |    |
 * F       |    |    |    | F  |    |    | Not in Initial Population or Denominator
 * G       | G  | G  |    | G  | G  |    | InDenominatorException = true & InNumerator = true
 * }</pre>
 *
 * <p>Each row represents a subject and their inclusion/exclusion population criteria on a Measure Report:
 *
 * <pre>{@code
 * Subject | IP | D  | DX | N  | DE | NX | Notes
 * --------|----|----|----|----|----|----|---------------------------------------------------------------
 * A       | A  | A  | A  |    |    |    |
 * B       | B  | B  |    | B  |    |    |
 * C       | C  | C  |    |    | C  |    | InDenominator = true, InDenominatorException = true,
 *                                       | InNumerator = false → Scores as InDenominatorException = true
 * D       | D  | D  |    | D  |    | D  |
 * E       | E  | E  |    | E  |    |    |
 * F       |    |    |    |    |    |    | Excluded: Not in Initial Population or Denominator
 * G       | G  | G  |    | G  |    |    | InDenominatorException = true & InNumerator = true → Remove from DE
 * }</pre>
 *
 * <p><strong>Population Counts:</strong>
 * <ul>
 *   <li>Initial Population (ip): 6</li>
 *   <li>Denominator (d): 6</li>
 *   <li>Denominator Exclusion (dx): 1</li>
 *   <li>Numerator (n): 4</li>
 *   <li>Denominator Exception (de): 1</li>
 *   <li>Numerator Exclusion (nx): 1</li>
 * </ul>
 *
 * <p><strong>Performance Rate Formula:</strong><br>
 * {@code (n - nx) / (d - dx - de)}<br>
 * {@code (4 - 1) / (6 - 1 - 1)} = <b>0.75</b>
 *
 * <p><strong>Measure Score:</strong> {@code 0.75}<br>
 *
 * <p>This is a FHIR-version-agnostic scorer that uses Def classes for iteration order
 * and mutates them by setting computed scores. Unlike R4/DSTU3MeasureReportScorer which
 * iterate over FHIR MeasureReport structures, this scorer iterates using:
 * <ul>
 *   <li>MeasureDef.groups() for group iteration</li>
 *   <li>GroupDef.stratifiers() for stratifier iteration</li>
 *   <li>StratifierDef.getStratum() for stratum iteration</li>
 *   <li>PopulationDef.getCount() for population counts</li>
 *   <li>StratumDef.getPopulationCount(PopulationDef) for stratum counts</li>
 * </ul>
 *
 * <p>This class computes scores and SETS them on Def objects using setScore() methods.
 * All methods are void. This makes Def classes the complete data model for measure scoring.
 */
public class MeasureReportDefScorer {

    private static final Logger logger = LoggerFactory.getLogger(MeasureReportDefScorer.class);

    /**
     * Score all groups in a measure definition - MUTATES GroupDef objects.
     *
     * @param measureUrl the measure URL for error reporting
     * @param measureDef the measure definition containing groups to score
     */
    public void score(String measureUrl, MeasureDef measureDef) {
        // Def-first iteration: iterate over MeasureDef.groups()
        for (GroupDef groupDef : measureDef.groups()) {
            scoreGroup(measureDef, groupDef);
        }
    }

    public void scoreGroup(MeasureDef measureDef, GroupDef groupDef) {

        var measureUrl = measureDef.url();
        var measureOrGroupScoring = groupDef.getMeasureOrGroupScoring(measureDef);

        checkMissingScoringType(measureUrl, measureOrGroupScoring);

        // Calculate group-level score
        Double groupScore = calculateGroupScore(measureUrl, groupDef, measureOrGroupScoring);

        // MUTATE: Set score on GroupDef
        groupDef.setScoreAndAdaptToImprovementNotation(groupScore, measureOrGroupScoring);

        // Score all stratifiers using Def-first iteration
        // Modified from R4MeasureReportScorer to iterate over Def classes instead of FHIR components
        for (StratifierDef stratifierDef : groupDef.stratifiers()) {
            scoreStratifier(measureUrl, groupDef, stratifierDef, measureOrGroupScoring);
        }
    }

    /**
     * Calculate score for a group based on its scoring type.
     */
    private Double calculateGroupScore(String measureUrl, GroupDef groupDef, MeasureScoring measureScoring) {
        switch (measureScoring) {
            case PROPORTION, RATIO:
                // Special case: RATIO with separate numerator/denominator observations
                if (measureScoring == MeasureScoring.RATIO
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    return scoreRatioMeasureObservationGroup(groupDef);
                }

                // Standard proportion/ratio scoring: (n - nx) / (d - dx - de)
                // Delegate to MeasureScoreCalculator
                return MeasureScoreCalculator.calculateProportionScore(
                        groupDef.getPopulationCount(MeasurePopulationType.NUMERATOR),
                        groupDef.getPopulationCount(MeasurePopulationType.NUMERATOREXCLUSION),
                        groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOR),
                        groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCLUSION),
                        groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCEPTION));

            case CONTINUOUSVARIABLE:
                // Continuous variable scoring - returns aggregate value
                final PopulationDef measureObsPop = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);

                if (measureObsPop == null) {
                    return null;
                }

                final QuantityDef quantityDef = scoreContinuousVariable(measureObsPop);

                // We want to record the aggregate result for later computation for continuous variable reports
                measureObsPop.setAggregationResult(quantityDef);

                return quantityDef != null ? quantityDef.value() : null;

            case COHORT:
                // COHORT measures don't have scores by design
                // They only report population counts (initial-population)
                return null;

            default:
                // Note: COMPOSITE measure scoring not yet implemented (enum value not defined in MeasureScoring)
                // When COMPOSITE is added to MeasureScoring enum, add explicit case here
                // Composite measures combine multiple component measures using different aggregation methods
                logger.warn("Unsupported measure scoring type: {} for measure: {}", measureScoring, measureUrl);
                return null;
        }
    }

    /**
     * Score a group for RATIO measures with MEASUREOBSERVATION populations.
     * Handles continuous variable ratio scoring where numerator and denominator have separate observations.
     *
     * @param groupDef the group definition
     * @return the calculated score or null
     */
    @Nullable
    private Double scoreRatioMeasureObservationGroup(GroupDef groupDef) {
        // Get all MEASUREOBSERVATION populations
        var measureObservationPopulationDefs = groupDef.getPopulationDefs(MeasurePopulationType.MEASUREOBSERVATION);

        // Find Measure Observations for Numerator and Denominator
        final PopulationDef numeratorPopulation =
                findPopulationDef(groupDef, measureObservationPopulationDefs, MeasurePopulationType.NUMERATOR);
        final PopulationDef denominatorPopulation =
                findPopulationDef(groupDef, measureObservationPopulationDefs, MeasurePopulationType.DENOMINATOR);

        // Calculate aggregate quantities for numerator and denominator
        final QuantityDef numeratorAggregate = calculateContinuousVariableAggregateQuantity(
                numeratorPopulation, PopulationDef::getAllSubjectResources);
        final QuantityDef denominatorAggregate = calculateContinuousVariableAggregateQuantity(
                denominatorPopulation, PopulationDef::getAllSubjectResources);

        // If there's no numerator or not denominator result, we still want to capture the
        // other result
        setAggregateResultIfPopNonNull(numeratorPopulation, numeratorAggregate);
        setAggregateResultIfPopNonNull(denominatorPopulation, denominatorAggregate);

        if (numeratorAggregate == null || denominatorAggregate == null) {
            return null;
        }

        return MeasureScoreCalculator.calculateRatioScore(numeratorAggregate.value(), denominatorAggregate.value());
    }

    /**
     * Score continuous variable measure - returns QuantityDef with aggregated value.
     * Simplified version of R4MeasureReportScorer#scoreContinuousVariable that just
     * returns the aggregate without setting it on a FHIR report.
     */
    private QuantityDef scoreContinuousVariable(PopulationDef populationDef) {
        return calculateContinuousVariableAggregateQuantity(populationDef, PopulationDef::getAllSubjectResources);
    }

    /**
     * Score all strata in a stratifier using Def-first iteration - MUTATES StratumDef objects.
     * Modified copy of R4MeasureReportScorer#scoreStratifier without FHIR components.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition containing the stratifier
     * @param stratifierDef the stratifier definition
     * @param measureScoring the scoring type
     */
    private void scoreStratifier(
            String measureUrl, GroupDef groupDef, StratifierDef stratifierDef, MeasureScoring measureScoring) {

        // Def-first iteration: iterate over StratifierDef.getStratum()
        for (StratumDef stratumDef : stratifierDef.getStratum()) {
            scoreStratum(measureUrl, groupDef, stratumDef, measureScoring);
        }
    }

    /**
     * Score a single stratum - MUTATES StratumDef object.
     * Modified copy of R4MeasureReportScorer#scoreStratum without StratifierGroupComponent.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition to score (will be mutated)
     * @param measureScoring the scoring type
     */
    private void scoreStratum(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureScoring measureScoring) {

        Double score = getStratumScoreOrNull(measureUrl, groupDef, stratumDef, measureScoring);

        // MUTATE: Set score on StratumDef
        stratumDef.setScore(score);
    }

    /**
     * Calculate stratum score based on scoring type.
     * Modified copy of R4MeasureReportScorer#getStratumScoreOrNull without StratifierGroupComponent.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @param measureScoring the scoring type
     * @return the calculated score or null
     */
    @Nullable
    private Double getStratumScoreOrNull(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureScoring measureScoring) {

        switch (measureScoring) {
            case PROPORTION, RATIO:
                // Check for special RATIO continuous variable case
                if (measureScoring.equals(MeasureScoring.RATIO)
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    return scoreRatioMeasureObservationStratum(stratumDef);
                } else {
                    return scoreProportionRatioStratum(groupDef, stratumDef);
                }

            case CONTINUOUSVARIABLE:
                return scoreContinuousVariableStratum(measureUrl, groupDef, stratumDef);

            default:
                return null;
        }
    }

    /**
     * Score a stratum for RATIO measures with MEASUREOBSERVATION populations.
     * Handles continuous variable ratio scoring where numerator and denominator have separate observations.
     * Uses pre-computed cache to eliminate redundant lookups during scoring.
     *
     * @param stratumDef the stratum definition
     * @return the calculated score or null
     */
    @Nullable
    private Double scoreRatioMeasureObservationStratum(StratumDef stratumDef) {

        if (stratumDef == null) {
            return null;
        }

        // Use pre-computed cache - eliminates all lookups
        MeasureObservationStratumCache cache = stratumDef.getMeasureObservationCache();
        if (cache == null) {
            return null;
        }

        // Extract cached references
        StratumPopulationDef stratumPopulationDefNum = cache.numeratorObservation();
        StratumPopulationDef stratumPopulationDefDen = cache.denominatorObservation();

        // Get parent PopulationDefs directly from StratumPopulationDef
        PopulationDef numPopDef = stratumPopulationDefNum.populationDef();
        PopulationDef denPopDef = stratumPopulationDefDen.populationDef();

        return scoreRatioContVariableStratum(stratumPopulationDefNum, stratumPopulationDefDen, numPopDef, denPopDef);
    }

    /**
     * Score a stratum for standard PROPORTION or RATIO measures (non-continuous variable).
     * Uses simple numerator/denominator count ratio.
     *
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @return the calculated score or null
     */
    @Nullable
    private Double scoreProportionRatioStratum(GroupDef groupDef, StratumDef stratumDef) {
        int numeratorCount = stratumDef.getPopulationCount(groupDef.getSingle(MeasurePopulationType.NUMERATOR));
        int denominatorCount = stratumDef.getPopulationCount(groupDef.getSingle(MeasurePopulationType.DENOMINATOR));

        // Delegate to MeasureScoreCalculator (pass 0 for exclusions since they're already applied at stratum level)
        return MeasureScoreCalculator.calculateProportionScore(numeratorCount, 0, denominatorCount, 0, 0);
    }

    /**
     * Score a stratum for CONTINUOUSVARIABLE measures.
     * Aggregates MEASUREOBSERVATION population observations filtered by stratum subjects.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @return the calculated score or null
     */
    @Nullable
    private Double scoreContinuousVariableStratum(String measureUrl, GroupDef groupDef, StratumDef stratumDef) {

        // Get the MEASUREOBSERVATION population from GroupDef
        PopulationDef measureObsPop = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);
        if (measureObsPop == null) {
            return null;
        }

        // Find the stratum population corresponding to MEASUREOBSERVATION
        StratumPopulationDef stratumPopulationDef = stratumDef.stratumPopulations().stream()
                .filter(stratumPopDef -> MeasurePopulationType.MEASUREOBSERVATION
                        == stratumPopDef.populationDef().type())
                .findFirst()
                .orElse(null);

        if (stratumPopulationDef == null) {
            return null;
        }

        // Calculate aggregate using stratum-filtered resources
        QuantityDef quantityDef = calculateContinuousVariableAggregateQuantity(
                measureObsPop, populationDef -> getResultsForStratum(populationDef, stratumPopulationDef));

        return quantityDef != null ? quantityDef.value() : null;
    }

    /**
     * Score ratio continuous variable for a stratum.
     * Copied from R4MeasureReportScorer#scoreRatioContVariableStratum.
     *
     * @param measureObsNumStratum stratum population for numerator measure observation
     * @param measureObsDenStratum stratum population for denominator measure observation
     * @param numPopDef numerator population definition
     * @param denPopDef denominator population definition
     * @return the ratio score or null
     */
    private Double scoreRatioContVariableStratum(
            StratumPopulationDef measureObsNumStratum,
            StratumPopulationDef measureObsDenStratum,
            PopulationDef numPopDef,
            PopulationDef denPopDef) {

        // Calculate aggregate for numerator observations filtered by stratum
        QuantityDef aggregateNumQuantityDef = calculateContinuousVariableAggregateQuantity(
                numPopDef, populationDef -> getResultsForStratum(populationDef, measureObsNumStratum));

        // Calculate aggregate for denominator observations filtered by stratum
        QuantityDef aggregateDenQuantityDef = calculateContinuousVariableAggregateQuantity(
                denPopDef, populationDef -> getResultsForStratum(populationDef, measureObsDenStratum));

        if (aggregateNumQuantityDef == null || aggregateDenQuantityDef == null) {
            return null;
        }

        Double num = aggregateNumQuantityDef.value();
        Double den = aggregateDenQuantityDef.value();

        if (num == null || den == null) {
            return null;
        }

        // Delegate ratio scoring to MeasureScoreCalculator
        return MeasureScoreCalculator.calculateRatioScore(num, den);
    }

    /**
     * Find PopulationDef by matching criteria reference.
     * Copied from BaseMeasureReportScorer - version-agnostic helper.
     *
     * @param groupDef the group definition
     * @param populationDefs list of MEASUREOBSERVATION populations to search
     * @param type the population type to find
     * @return matching PopulationDef or null
     */
    @Nullable
    private PopulationDef findPopulationDef(
            GroupDef groupDef, List<PopulationDef> populationDefs, MeasurePopulationType type) {
        PopulationDef firstPop = groupDef.getFirstWithTypeAndNonNullId(type);
        if (firstPop == null) {
            return null;
        }

        String criteriaId = firstPop.id();

        return populationDefs.stream()
                .filter(populationDef -> criteriaId.equals(populationDef.getCriteriaReference()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Extract StratumPopulationDef from StratumDef that matches a PopulationDef.
     * Uses direct PopulationDef reference instead of ID matching.
     *
     * @param stratumDef the stratum definition
     * @param populationDef the population definition to match
     * @return matching StratumPopulationDef or null
     */
    @Nullable
    private StratumPopulationDef getStratumPopDefFromPopDef(StratumDef stratumDef, PopulationDef populationDef) {
        if (populationDef == null) {
            return null;
        }
        return stratumDef.stratumPopulations().stream()
                .filter(t -> t.populationDef() == populationDef)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get results filtered for a specific stratum.
     * Filters at the subject level (Map.Entry key) rather than observation Map keys.
     * Aligned with BaseMeasureReportScorer pattern for correct stratification.
     *
     * <p><strong>CRITICAL:</strong> PopulationDef.subjectResources are keyed on UNQUALIFIED patient IDs
     * (e.g., "patient-1965-female") but StratumPopulationDef may contain QUALIFIED IDs
     * (e.g., "Patient/patient-1965-female"). This method uses getSubjectsUnqualified() to ensure
     * proper matching between the two ID formats.
     *
     * <p>For NON_SUBJECT_VALUE stratifiers with resource basis, the stratum contains resource IDs
     * (e.g., "Encounter/encounter-1") in resourceIdsForSubjectList, and we need to filter the
     * observation resources by those IDs rather than by patient subjects.
     *
     * <p>Fixed in Part 1 (integrate-measure-def-scorer-part1-foundation) to prevent 14 test failures
     * in CONTINUOUSVARIABLE measures when old scorers are removed in Part 2.
     *
     * @param populationDef the population definition (MEASUREOBSERVATION)
     * @param stratumPopulationDef the stratum population to filter by
     * @return collection of resources belonging to this stratum
     */
    private static Collection<Object> getResultsForStratum(
            PopulationDef populationDef, StratumPopulationDef stratumPopulationDef) {

        if (stratumPopulationDef == null || populationDef == null || populationDef.getSubjectResources() == null) {
            return List.of();
        }

        // For NON_SUBJECT_VALUE stratifiers with non-boolean basis, we must filter by resource IDs.
        // An empty resourceIdsForSubjectList means "no qualifying resources" for this stratum,
        // NO "fallback to subject-based stratification".
        if (stratumPopulationDef.measureStratifierType() == MeasureStratifierType.NON_SUBJECT_VALUE
                && !stratumPopulationDef.isBooleanBasis()) {

            if (stratumPopulationDef.resourceIdsForSubjectList() == null
                    || stratumPopulationDef.resourceIdsForSubjectList().isEmpty()) {
                return List.of();
            }

            return getResultsForStratumByResourceIds(populationDef, stratumPopulationDef);
        }

        // Subject-based stratification (VALUE stratifiers, boolean basis, etc.)
        Set<String> stratumSubjectsUnqualified = stratumPopulationDef.getSubjectsUnqualified();

        return populationDef.getSubjectResources().entrySet().stream()
                .filter(entry -> stratumSubjectsUnqualified.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * Get results filtered for a NON_SUBJECT_VALUE stratum by resource IDs.
     * For resource-basis stratifiers, the stratum contains specific resource IDs that should be included.
     *
     * <p>For MEASUREOBSERVATION populations, the subjectResources contain Map&lt;inputResource, outputValue&gt;
     * where we need to filter by the input resource IDs and return the output values (observations).
     *
     * @param populationDef the population definition (MEASUREOBSERVATION)
     * @param stratumPopulationDef the stratum population containing resource IDs
     * @return collection of resources/observations matching the stratum's resource IDs
     */
    private static Collection<Object> getResultsForStratumByResourceIds(
            PopulationDef populationDef, StratumPopulationDef stratumPopulationDef) {

        Set<String> stratumResourceIds = stratumPopulationDef.resourceIdsAsSet();

        // For MEASUREOBSERVATION, subjectResources contains Set<Map<inputResource, outputValue>>
        // MeasureScoreCalculator.collectQuantities expects Map objects and extracts values from them.
        // We need to return filtered Maps (not the values directly) so collectQuantities can process them.
        if (populationDef.type() == MeasurePopulationType.MEASUREOBSERVATION) {
            return populationDef.getSubjectResources().values().stream()
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
        return populationDef.getSubjectResources().values().stream()
                .flatMap(Collection::stream)
                .filter(resource -> {
                    if (resource instanceof IBaseResource baseResource) {
                        String resourceId =
                                baseResource.getIdElement().toVersionless().getValue();
                        return stratumResourceIds.contains(resourceId);
                    }
                    return false;
                })
                .toList();
    }

    /**
     * Calculate continuous variable aggregate quantity.
     * Delegates to {@link MeasureScoreCalculator} for the actual aggregation.
     *
     * @param populationDef the population definition containing observation data
     * @param popDefToResources function to extract resources from population def
     * @return aggregated QuantityDef or null if population is null
     */
    @Nullable
    private static QuantityDef calculateContinuousVariableAggregateQuantity(
            @Nullable PopulationDef populationDef, Function<PopulationDef, Collection<Object>> popDefToResources) {

        if (populationDef == null) {
            return null;
        }

        return calculateContinuousVariableAggregateQuantity(
                populationDef.getAggregateMethod(), popDefToResources.apply(populationDef));
    }

    /**
     * Calculate continuous variable aggregate quantity.
     * Delegates to {@link MeasureScoreCalculator} for collection and aggregation.
     *
     * @param aggregateMethod the aggregation method (SUM, AVG, MIN, MAX, MEDIAN, COUNT)
     * @param qualifyingResources the resources containing QuantityDef observations
     * @return aggregated QuantityDef or null if no resources
     */
    @Nullable
    private static QuantityDef calculateContinuousVariableAggregateQuantity(
            ContinuousVariableObservationAggregateMethod aggregateMethod, Collection<Object> qualifyingResources) {
        // Delegate to MeasureScoreCalculator for collection and aggregation
        var observationQuantity = MeasureScoreCalculator.collectQuantities(qualifyingResources);
        return MeasureScoreCalculator.aggregateContinuousVariable(observationQuantity, aggregateMethod);
    }

    private static void setAggregateResultIfPopNonNull(@Nullable PopulationDef populationDef, QuantityDef quantityDef) {
        Optional.ofNullable(populationDef)
                .ifPresent(nonNullPopulationDef -> nonNullPopulationDef.setAggregationResult(quantityDef));
    }

    /**
     * Validate scoring type is present.
     * Reused from BaseMeasureReportScorer pattern.
     */
    private void checkMissingScoringType(String measureUrl, MeasureScoring measureScoring) {
        if (measureScoring == null) {
            throw new InvalidRequestException(
                    "Measure does not have a scoring methodology defined. Add a \"scoring\" property to the measure definition or the group definition for measure: "
                            + measureUrl);
        }
    }
}
