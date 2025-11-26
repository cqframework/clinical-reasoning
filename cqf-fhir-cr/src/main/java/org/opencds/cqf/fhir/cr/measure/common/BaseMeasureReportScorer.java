package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

// Extracted version-agnostic patterns from R4MeasureReportScorer by Claude Sonnet 4.5
public abstract class BaseMeasureReportScorer<MeasureReportT> implements MeasureReportScorer<MeasureReportT> {

    // Version-agnostic population type constants
    protected static final String NUMERATOR = "numerator";
    protected static final String DENOMINATOR = "denominator";
    protected static final String DENOMINATOR_EXCLUSION = "denominator-exclusion";
    protected static final String DENOMINATOR_EXCEPTION = "denominator-exception";
    protected static final String NUMERATOR_EXCLUSION = "numerator-exclusion";

    protected Double calcProportionScore(Integer numeratorCount, Integer denominatorCount) {
        if (numeratorCount == null) {
            numeratorCount = 0;
        }
        if (denominatorCount != null && denominatorCount != 0) {
            return numeratorCount / (double) denominatorCount;
        }

        return null;
    }

    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic validation
    protected MeasureScoring checkMissingScoringType(MeasureDef measureDef, MeasureScoring measureScoring) {
        if (measureScoring == null) {
            throw new InvalidRequestException(
                    "Measure does not have a scoring methodology defined. Add a \"scoring\" property to the measure definition or the group definition for MeasureDef: "
                            + measureDef.url());
        }
        return measureScoring;
    }

    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic validation
    protected void groupHasValidId(MeasureDef measureDef, String id) {
        if (id == null || id.isEmpty()) {
            throw new InvalidRequestException(
                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately for MeasureDef: "
                            + measureDef.url());
        }
    }

    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic helper
    @Nullable
    protected PopulationDef getFirstMeasureObservation(GroupDef groupDef) {
        var measureObservations = getMeasureObservations(groupDef);
        if (!measureObservations.isEmpty()) {
            return getMeasureObservations(groupDef).get(0);
        } else {
            return null;
        }
    }

    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic helper
    protected List<PopulationDef> getMeasureObservations(GroupDef groupDef) {
        return groupDef.populations().stream()
                .filter(t -> t.type().equals(MeasurePopulationType.MEASUREOBSERVATION))
                .toList();
    }

    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic helper
    @Nullable
    protected PopulationDef findPopulationDef(
            GroupDef groupDef, List<PopulationDef> populationDefs, MeasurePopulationType type) {
        var groupPops = groupDef.get(type);
        if (groupPops == null || groupPops.isEmpty() || groupPops.get(0).id() == null) {
            return null;
        }

        String criteriaId = groupPops.get(0).id();

        return populationDefs.stream()
                .filter(p -> criteriaId.equals(p.getCriteriaReference()))
                .findFirst()
                .orElse(null);
    }

    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic utility
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
    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic helper
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
    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic helper
    protected Set<Object> getResultsForStratum(
            PopulationDef measureObservationPopulationDef, StratumPopulationDef stratumPopulationDef) {

        return measureObservationPopulationDef.getSubjectResources().entrySet().stream()
                .filter(entry -> doesStratumPopDefMatchGroupPopDef(stratumPopulationDef, entry))
                .map(Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    // Moved from R4MeasureReportScorer by Claude Sonnet 4.5 - version-agnostic helper
    protected boolean doesStratumPopDefMatchGroupPopDef(
            StratumPopulationDef stratumPopulationDef, Entry<String, Set<Object>> entry) {

        return stratumPopulationDef.getSubjectsUnqualified().stream()
                .collect(Collectors.toUnmodifiableSet())
                .contains(entry.getKey());
    }

    /**
     * Performs version-agnostic aggregation on a list of numeric values using the specified method.
     * Moved from R4MeasureReportScorer by Claude Sonnet 4.5 to enable reuse across FHIR versions.
     *
     * <p>Supported aggregation methods:
     * <ul>
     *   <li>SUM - Sum of all values</li>
     *   <li>MAX - Maximum value</li>
     *   <li>MIN - Minimum value</li>
     *   <li>AVG - Average (mean) of values</li>
     *   <li>COUNT - Count of values</li>
     *   <li>MEDIAN - Median value</li>
     * </ul>
     *
     * @param values the list of numeric values to aggregate
     * @param method the aggregation method to apply
     * @return the aggregated result, or null if values is null or empty
     * @throws InvalidRequestException if method is N_A
     * @throws IllegalArgumentException if method is not supported
     */
    @Nullable
    protected Double aggregateDoubles(List<Double> values, ContinuousVariableObservationAggregateMethod method) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        if (ContinuousVariableObservationAggregateMethod.N_A == method) {
            throw new InvalidRequestException(
                    "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
        }

        return switch (method) {
            case SUM -> values.stream().mapToDouble(Double::doubleValue).sum();
            case MAX -> values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
            case MIN -> values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
            case AVG -> values.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(Double.NaN);
            case COUNT -> (double) values.size();
            case MEDIAN -> {
                List<Double> sorted = values.stream().sorted().toList();
                int n = sorted.size();
                if (n % 2 == 1) {
                    yield sorted.get(n / 2);
                } else {
                    yield (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
                }
            }
            default -> throw new IllegalArgumentException("Unsupported aggregation method: " + method);
        };
    }

    /**
     * Calculates a continuous variable score by aggregating numeric values.
     * Version-agnostic method moved from R4MeasureReportScorer by Claude Sonnet 4.5.
     *
     * <p>This method performs the core continuous variable scoring calculation without
     * dealing with FHIR-specific types like Quantity. The calling code is responsible
     * for extracting numeric values from FHIR resources and wrapping the result appropriately.
     *
     * @param aggregateMethod the aggregation method to apply
     * @param values the collection of numeric values to aggregate
     * @return the calculated score, or null if values is null or empty
     * @throws InvalidRequestException if aggregate method is N_A
     */
    @Nullable
    protected Double calculateContinuousVariableScore(
            ContinuousVariableObservationAggregateMethod aggregateMethod, Collection<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return aggregateDoubles(new java.util.ArrayList<>(values), aggregateMethod);
    }
}
