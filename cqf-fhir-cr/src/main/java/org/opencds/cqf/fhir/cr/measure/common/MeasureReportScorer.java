package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for scoring MeasureReports across different FHIR versions.
 * Contains version-agnostic scoring logic as default methods.
 * Refactored by Claude Sonnet 4.5 on 2025-12-03 to move logic from BaseMeasureReportScorer.
 */
public interface MeasureReportScorer<MeasureReportT> {

    Logger logger = LoggerFactory.getLogger(MeasureReportScorer.class);

    /**
     * Main scoring method - must be implemented by version-specific scorers.
     */
    void score(String measureUrl, MeasureDef measureDef, MeasureReportT measureReport);

    /**
     * Get the version-specific ContinuousVariableObservationConverter.
     * This allows the interface methods to convert QuantityDef to version-specific Quantity types.
     */
    ContinuousVariableObservationConverter<? extends ICompositeType> getContinuousVariableConverter();

    // ==================== DEFAULT METHODS - Version-Agnostic Logic ====================

    default Double calcProportionScore(Integer numeratorCount, Integer denominatorCount) {
        if (numeratorCount == null) {
            numeratorCount = 0;
        }
        if (denominatorCount != null && denominatorCount != 0) {
            return numeratorCount / (double) denominatorCount;
        }
        return null;
    }

    default MeasureScoring checkMissingScoringType(MeasureDef measureDef, MeasureScoring measureScoring) {
        if (measureScoring == null) {
            throw new InvalidRequestException(
                    "Measure does not have a scoring methodology defined. Add a \"scoring\" property to the measure definition or the group definition for MeasureDef: "
                            + measureDef.url());
        }
        return measureScoring;
    }

    default void groupHasValidId(MeasureDef measureDef, String id) {
        if (id == null || id.isEmpty()) {
            throw new InvalidRequestException(
                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately for MeasureDef: "
                            + measureDef.url());
        }
    }

    @Nullable
    default PopulationDef getFirstMeasureObservation(GroupDef groupDef) {
        var measureObservations = getMeasureObservations(groupDef);
        if (!measureObservations.isEmpty()) {
            return getMeasureObservations(groupDef).get(0);
        } else {
            return null;
        }
    }

    default List<PopulationDef> getMeasureObservations(GroupDef groupDef) {
        return groupDef.populations().stream()
                .filter(t -> t.type().equals(MeasurePopulationType.MEASUREOBSERVATION))
                .toList();
    }

    @Nullable
    default PopulationDef findPopulationDef(
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

    @Nullable
    default StratumPopulationDef getStratumPopDefFromPopDef(StratumDef stratumDef, PopulationDef populationDef) {
        return stratumDef.stratumPopulations().stream()
                .filter(t -> t.id().equals(populationDef.id()))
                .findFirst()
                .orElse(null);
    }

    default Set<Object> getResultsForStratum(
            PopulationDef measureObservationPopulationDef, StratumPopulationDef stratumPopulationDef) {

        return measureObservationPopulationDef.getSubjectResources().entrySet().stream()
                .filter(entry -> doesStratumPopDefMatchGroupPopDef(stratumPopulationDef, entry))
                .map(Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    default boolean doesStratumPopDefMatchGroupPopDef(
            StratumPopulationDef stratumPopulationDef, Entry<String, Set<Object>> entry) {

        return stratumPopulationDef.getSubjectsUnqualified().stream()
                .collect(Collectors.toUnmodifiableSet())
                .contains(entry.getKey());
    }

    // ==================== STATIC HELPER METHODS ====================

    default List<QuantityDef> collectQuantities(Collection<Object> resources) {
        var mapValues = resources.stream()
                .filter(x -> x instanceof java.util.Map<?, ?>)
                .map(x -> (java.util.Map<?, ?>) x)
                .map(java.util.Map::values)
                .flatMap(Collection::stream)
                .toList();

        return mapValues.stream()
                .filter(QuantityDef.class::isInstance)
                .map(QuantityDef.class::cast)
                .toList();
    }

    default QuantityDef aggregate(List<QuantityDef> quantities, ContinuousVariableObservationAggregateMethod method) {
        if (quantities == null || quantities.isEmpty()) {
            return null;
        }

        if (ContinuousVariableObservationAggregateMethod.N_A == method) {
            throw new InvalidRequestException(
                    "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
        }

        double result;

        switch (method) {
            case SUM:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .sum();
                break;
            case MAX:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .max()
                        .orElse(Double.NaN);
                break;
            case MIN:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .min()
                        .orElse(Double.NaN);
                break;
            case AVG:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .average()
                        .orElse(Double.NaN);
                break;
            case COUNT:
                result = quantities.size();
                break;
            case MEDIAN:
                List<Double> sorted = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();
                int n = sorted.size();
                if (n % 2 == 1) {
                    result = sorted.get(n / 2);
                } else {
                    result = (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported aggregation method: " + method);
        }

        return new QuantityDef(result);
    }

    @Nullable
    default QuantityDef calculateContinuousVariableAggregateQuantity(
            String measureUrl,
            PopulationDef populationDef,
            Function<PopulationDef, Collection<Object>> popDefToResources) {

        if (populationDef == null) {
            logger.warn("Measure population group has no measure population defined for measure: {}", measureUrl);
            return null;
        }

        return calculateContinuousVariableAggregateQuantity(
                populationDef.getAggregateMethod(), popDefToResources.apply(populationDef));
    }

    @Nullable
    default QuantityDef calculateContinuousVariableAggregateQuantity(
            ContinuousVariableObservationAggregateMethod aggregateMethod, Collection<Object> qualifyingResources) {
        var observationQuantity = collectQuantities(qualifyingResources);
        return aggregate(observationQuantity, aggregateMethod);
    }

    default int getCountFromStratifierPopulation(
            GroupDef groupDef, StratumDef stratumDef, MeasurePopulationType populationType) {
        if (stratumDef == null) {
            return 0;
        }

        PopulationDef populationDef = groupDef.getSingle(populationType);
        return stratumDef.getPopulationCount(populationDef);
    }

    @Nullable
    default Double scoreRatioContVariable(String measureUrl, GroupDef groupDef, List<PopulationDef> populationDefs) {

        if (groupDef == null || populationDefs == null || populationDefs.isEmpty()) {
            return null;
        }

        PopulationDef numPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.NUMERATOR);
        PopulationDef denPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.DENOMINATOR);

        if (numPopDef == null || denPopDef == null) {
            return null;
        }

        QuantityDef aggregateNumQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, numPopDef, PopulationDef::getAllSubjectResources);
        QuantityDef aggregateDenQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, denPopDef, PopulationDef::getAllSubjectResources);

        if (aggregateNumQuantityDef == null || aggregateDenQuantityDef == null) {
            return null;
        }

        Double num = aggregateNumQuantityDef.value();
        Double den = aggregateDenQuantityDef.value();

        if (den == null || den == 0.0) {
            return null;
        }

        if (num == null || num == 0.0) {
            return den > 0.0 ? 0.0 : null;
        }

        return num / den;
    }

    @Nullable
    default Double scoreRatioContVariableStratum(
            String measureUrl,
            StratumPopulationDef measureObsNumStratum,
            StratumPopulationDef measureObsDenStratum,
            PopulationDef numPopDef,
            PopulationDef denPopDef) {

        QuantityDef aggregateNumQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, numPopDef, populationDef -> getResultsForStratum(populationDef, measureObsNumStratum));
        QuantityDef aggregateDenQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, denPopDef, populationDef -> getResultsForStratum(populationDef, measureObsDenStratum));

        if (aggregateNumQuantityDef == null || aggregateDenQuantityDef == null) {
            return null;
        }

        Double num = aggregateNumQuantityDef.value();
        Double den = aggregateDenQuantityDef.value();

        if (den == null || den == 0.0) {
            return null;
        }

        if (num == null || num == 0.0) {
            return den > 0.0 ? 0.0 : null;
        }

        return num / den;
    }

    @Nullable
    default Double calculateGroupScore(String measureUrl, MeasureScoring measureScoring, GroupDef groupDef) {

        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                if (measureScoring.equals(MeasureScoring.RATIO)
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    return scoreRatioContVariable(measureUrl, groupDef, getMeasureObservations(groupDef));
                } else {
                    return calcProportionScore(
                            groupDef.getPopulationCount(MeasurePopulationType.NUMERATOR)
                                    - groupDef.getPopulationCount(MeasurePopulationType.NUMERATOREXCLUSION),
                            groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOR)
                                    - groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCLUSION)
                                    - groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCEPTION));
                }
            case CONTINUOUSVARIABLE:
                return null;
            default:
                return null;
        }
    }

    @Nullable
    default Double calculateStratumScore(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureScoring measureScoring) {

        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                if (measureScoring.equals(MeasureScoring.RATIO)
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    if (stratumDef != null) {
                        var populationDefs = getMeasureObservations(groupDef);
                        PopulationDef numPopDef =
                                findPopulationDef(groupDef, populationDefs, MeasurePopulationType.NUMERATOR);
                        PopulationDef denPopDef =
                                findPopulationDef(groupDef, populationDefs, MeasurePopulationType.DENOMINATOR);
                        StratumPopulationDef stratumPopulationDefDen =
                                getStratumPopDefFromPopDef(stratumDef, denPopDef);
                        StratumPopulationDef stratumPopulationDefNum =
                                getStratumPopDefFromPopDef(stratumDef, numPopDef);
                        return scoreRatioContVariableStratum(
                                measureUrl, stratumPopulationDefNum, stratumPopulationDefDen, numPopDef, denPopDef);
                    }
                    return null;
                } else {
                    return calcProportionScore(
                            getCountFromStratifierPopulation(groupDef, stratumDef, MeasurePopulationType.NUMERATOR),
                            getCountFromStratifierPopulation(groupDef, stratumDef, MeasurePopulationType.DENOMINATOR));
                }
            case CONTINUOUSVARIABLE:
                return null;
            default:
                return null;
        }
    }

    default GroupDef getGroupDefById(MeasureDef measureDef, String groupId) {
        var groupDefs = measureDef.groups();
        if (groupDefs.size() == 1) {
            return groupDefs.get(0);
        }
        return groupDefs.stream()
                .filter(t -> t.id().equals(groupId))
                .findFirst()
                .orElse(null);
    }

    default MeasureScoring getGroupMeasureScoringById(MeasureDef measureDef, String groupId) {
        MeasureScoring groupScoringType = null;
        if (measureDef.groups().size() == 1) {
            groupScoringType = measureDef.groups().get(0).measureScoring();
        } else {
            for (GroupDef groupDef : measureDef.groups()) {
                var groupDefMeasureScoring = groupDef.measureScoring();
                groupHasValidId(measureDef, groupId);
                groupHasValidId(measureDef, groupDef.id());
                if (groupDef.id().equals(groupId)) {
                    groupScoringType = groupDefMeasureScoring;
                }
            }
        }
        return checkMissingScoringType(measureDef, groupScoringType);
    }

    /**
     * Calculate stratum quantity in a version-agnostic way.
     * Returns QuantityDef that can be converted to version-specific Quantity types.
     */
    @Nullable
    default QuantityDef calculateStratumQuantity(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureScoring measureScoring) {

        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                Double score = calculateStratumScore(measureUrl, groupDef, stratumDef, measureScoring);
                return score != null ? new QuantityDef(score) : null;
            case CONTINUOUSVARIABLE:
                final StratumPopulationDef stratumPopulationDef;
                if (stratumDef != null) {
                    stratumPopulationDef = stratumDef.stratumPopulations().stream()
                            .filter(stratumPopDef ->
                                    stratumPopDef.id().startsWith(MeasurePopulationType.MEASUREOBSERVATION.toCode()))
                            .findFirst()
                            .orElse(null);
                } else {
                    stratumPopulationDef = null;
                }
                return calculateContinuousVariableAggregateQuantity(
                        measureUrl,
                        getFirstMeasureObservation(groupDef),
                        populationDef -> getResultsForStratum(populationDef, stratumPopulationDef));
            default:
                return null;
        }
    }

    /**
     * Get stratum score as a version-specific Quantity type, or null if not calculable.
     * Uses the subclass's ContinuousVariableObservationConverter to convert to the appropriate FHIR version.
     */
    @Nullable
    default ICompositeType getStratumScoreOrNull(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureScoring measureScoring) {
        QuantityDef quantityDef = calculateStratumQuantity(measureUrl, groupDef, stratumDef, measureScoring);
        return quantityDef != null ? getContinuousVariableConverter().convertToFhirQuantity(quantityDef) : null;
    }
}
