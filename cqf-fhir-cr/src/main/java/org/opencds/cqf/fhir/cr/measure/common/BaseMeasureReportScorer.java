package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for version-specific MeasureReport scorers.
 *
 * <p>Extracted version-agnostic patterns from R4MeasureReportScorer
 *
 * <p><strong>DEPRECATION NOTICE:</strong> This class is deprecated and will be removed in a future release.
 * For internal use, this class will be replaced by {@link MeasureDefScorer}
 * integrated into the evaluation workflow in Part 2.
 * See: integrate-measure-def-scorer-part2-integration PRP
 */
public abstract class BaseMeasureReportScorer<MeasureReportT> implements IMeasureReportScorer<MeasureReportT> {

    // Version-agnostic population type constants
    protected static final String NUMERATOR = "numerator";
    protected static final String DENOMINATOR = "denominator";
    protected static final String DENOMINATOR_EXCLUSION = "denominator-exclusion";
    protected static final String DENOMINATOR_EXCEPTION = "denominator-exception";
    protected static final String NUMERATOR_EXCLUSION = "numerator-exclusion";

    /**
     * Calculate proportion score: numerator / denominator.
     * Delegates to {@link MeasureScoreCalculator} for the calculation.
     *
     * @param numeratorCount Effective numerator count (after exclusions applied)
     * @param denominatorCount Effective denominator count (after exclusions/exceptions applied)
     * @return The calculated score, or {@code null} if denominator is 0
     */
    protected Double calcProportionScore(Integer numeratorCount, Integer denominatorCount) {
        if (numeratorCount == null) {
            numeratorCount = 0;
        }
        // Delegate to MeasureScoreCalculator (pass 0 for exclusions since they're already applied)
        return MeasureScoreCalculator.calculateProportionScore(numeratorCount, 0, denominatorCount, 0, 0);
    }

    protected MeasureScoring checkMissingScoringType(MeasureDef measureDef, MeasureScoring measureScoring) {
        if (measureScoring == null) {
            throw new InvalidRequestException(
                    "Measure does not have a scoring methodology defined. Add a \"scoring\" property to the measure definition or the group definition for MeasureDef: "
                            + measureDef.url());
        }
        return measureScoring;
    }

    protected void groupHasValidId(MeasureDef measureDef, String id) {
        if (id == null || id.isEmpty()) {
            throw new InvalidRequestException(
                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately for MeasureDef: "
                            + measureDef.url());
        }
    }

    @Nullable
    protected PopulationDef getFirstMeasureObservation(GroupDef groupDef) {
        var measureObservations = getMeasureObservations(groupDef);
        if (!measureObservations.isEmpty()) {
            return getMeasureObservations(groupDef).get(0);
        } else {
            return null;
        }
    }

    protected List<PopulationDef> getMeasureObservations(GroupDef groupDef) {
        return groupDef.populations().stream()
                .filter(t -> t.type().equals(MeasurePopulationType.MEASUREOBSERVATION))
                .toList();
    }

    @Nullable
    protected PopulationDef findPopulationDef(
            GroupDef groupDef, List<PopulationDef> populationDefs, MeasurePopulationType type) {
        var groupPops = groupDef.getPopulationDefs(type);
        if (groupPops == null || groupPops.isEmpty() || groupPops.get(0).id() == null) {
            return null;
        }

        String criteriaId = groupPops.get(0).id();

        return populationDefs.stream()
                .filter(p -> criteriaId.equals(p.getCriteriaReference()))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    protected Double toDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Extract StratumPopulationDef from populationDef
     *
     * @param stratumDef the Stratum definition object that contains the population to target
     * @param populationDef the measureObservation population related to the stratumPopulationDef to extract
     * @return the matching StratumPopulationDef or null if not found
     */
    @Nullable
    protected StratumPopulationDef getStratumPopDefFromPopDef(StratumDef stratumDef, PopulationDef populationDef) {
        return stratumDef.stratumPopulations().stream()
                .filter(t -> t.id().equals(populationDef.id()))
                .findFirst()
                .orElse(null);
    }

    /**
     * The goal here is to extract the resources references by the population def for the subjects
     * in the stratum populationDef.
     * <p/>
     * So, for example, if the stratum population def has subjects:
     * <ul>
     *     <li>patient123</li>
     *     <li>patient456</li>
     *     <li>patient567</li>
     * </ul>
     * and the population has:
     * <ul>
     *     <li>patient000 -> Patient000 -> Quantity(57)</li>
     *     <li>patient100 -> Patient100 -> Quantity(36)</li>
     *     <li>patient123 -> Patient123 -> Quantity(57)</li>
     *     <li>patient456 -> Patient456 -> Quantity(3)</li>
     *     <li>patient500 -> Patient500 -> Quantity(5)</li>
     *     <li>patient567 -> Patient567 -> Quantity(57)</li>
     * </ul>
     * Then the method returns:
     * <ul>
     *     <li>Patient123 -> Quantity(57)</li>
     *     <li>Patient456 -> Quantity(3)</li>
     *     <li>Patient567 -> Quantity(57)</li>
     * </ul>
     *
     * @param measureObservationPopulationDef the population definition containing all subject resources
     * @param stratumPopulationDef the stratum population definition with subject filters
     * @return the filtered set of resources for subjects in the stratum
     */
    protected Set<Object> getResultsForStratum(
            PopulationDef measureObservationPopulationDef, StratumPopulationDef stratumPopulationDef) {

        return measureObservationPopulationDef.getSubjectResources().entrySet().stream()
                .filter(entry -> doesStratumPopDefMatchGroupPopDef(stratumPopulationDef, entry))
                .map(Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    protected boolean doesStratumPopDefMatchGroupPopDef(
            StratumPopulationDef stratumPopulationDef, Entry<String, Set<Object>> entry) {

        return stratumPopulationDef.getSubjectsUnqualified().stream()
                .collect(Collectors.toUnmodifiableSet())
                .contains(entry.getKey());
    }
}
