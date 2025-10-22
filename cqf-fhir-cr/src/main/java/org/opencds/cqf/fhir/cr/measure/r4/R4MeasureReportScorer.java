package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
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
 * <p> (v3.18.0 and below) Previous calculation of measure score from MeasureReport only interpreted Numerator, Denominator membership since exclusions and exceptions were already applied. Now exclusions and exceptions are present in Denominator and Numerator populations, the measure scorer calculation has to take into account additional population membership to determine Final-Numerator and Final-Denominator values</p>
 */
@SuppressWarnings("squid:S1135")
public class R4MeasureReportScorer extends BaseMeasureReportScorer<MeasureReport> {

    private static final Logger logger = LoggerFactory.getLogger(R4MeasureReportScorer.class);

    private static final String NUMERATOR = "numerator";
    private static final String DENOMINATOR = "denominator";
    private static final String DENOMINATOR_EXCLUSION = "denominator-exclusion";
    private static final String DENOMINATOR_EXCEPTION = "denominator-exception";
    private static final String NUMERATOR_EXCLUSION = "numerator-exclusion";

    @Override
    public void score(String measureUrl, MeasureDef measureDef, MeasureReport measureReport) {
        // Measure Def Check
        if (measureDef == null) {
            throw new InvalidRequestException(
                    "MeasureDef is required in order to score a Measure for Measure: " + measureUrl);
        }
        // No groups to score, nothing to do.
        if (measureReport.getGroup().isEmpty()) {
            return;
        }

        for (MeasureReportGroupComponent mrgc : measureReport.getGroup()) {
            scoreGroup(
                    measureUrl,
                    getGroupMeasureScoring(mrgc, measureDef),
                    mrgc,
                    getGroupDef(measureDef, mrgc).isIncreaseImprovementNotation(),
                    getGroupDef(measureDef, mrgc));
        }
    }

    protected GroupDef getGroupDef(MeasureDef measureDef, MeasureReportGroupComponent mrgc) {
        var groupDefs = measureDef.groups();
        if (groupDefs.size() == 1) {
            return groupDefs.get(0);
        }
        return groupDefs.stream()
                .filter(t -> t.id().equals(mrgc.getId()))
                .findFirst()
                .orElse(null);
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

    protected MeasureScoring getGroupMeasureScoring(MeasureReportGroupComponent mrgc, MeasureDef measureDef) {
        MeasureScoring groupScoringType = null;
        // if not multi-rate, get first groupDef scoringType
        if (measureDef.groups().size() == 1) {
            groupScoringType = measureDef.groups().get(0).measureScoring();
        } else {
            // multi rate measure, match groupComponent to groupDef to extract scoringType
            for (GroupDef groupDef : measureDef.groups()) {
                var groupDefMeasureScoring = groupDef.measureScoring();
                // groups must have id populated
                groupHasValidId(measureDef, mrgc.getId());
                groupHasValidId(measureDef, groupDef.id());
                // Match by group id if available
                // Note: Match by group's population id, was removed per cqf-measures conformance change FHIR-45423
                // multi-rate measures must have a group.id defined to be conformant
                if (groupDef.id().equals(mrgc.getId())) {
                    groupScoringType = groupDefMeasureScoring;
                }
            }
        }
        return checkMissingScoringType(measureDef, groupScoringType);
    }

    protected void scoreGroup(
            String measureUrl,
            MeasureScoring measureScoring,
            MeasureReportGroupComponent mrgc,
            boolean isIncreaseImprovementNotation,
            GroupDef groupDef) {

        switch (measureScoring) {
            case PROPORTION, RATIO:
                var score = calcProportionScore(
                        getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR)
                                - getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR_EXCLUSION),
                        getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR)
                                - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCLUSION)
                                - getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCEPTION));
                // When applySetMembership=false, this value can receive strange values
                // This should prevent scoring in certain scenarios like <0
                if (score != null && score >= 0) {
                    if (isIncreaseImprovementNotation) {
                        mrgc.setMeasureScore(new Quantity(score));
                    } else {
                        mrgc.setMeasureScore(new Quantity(1 - score));
                    }
                }
                break;

            case CONTINUOUSVARIABLE:
                scoreContinuousVariable(measureUrl, mrgc, groupDef);
                break;
            default:
                break;
        }

        for (MeasureReportGroupStratifierComponent stratifierComponent : mrgc.getStratifier()) {
            scoreStratifier(measureUrl, groupDef, measureScoring, stratifierComponent);
        }
    }

    protected void scoreContinuousVariable(String measureUrl, MeasureReportGroupComponent mrgc, GroupDef groupDef) {
        logger.info("1234: scoreContinuousVariable");
        final Quantity aggregateQuantity =
                calculateContinuousVariableAggregateQuantity(measureUrl, groupDef, PopulationDef::getResources);

        mrgc.setMeasureScore(aggregateQuantity);
    }

    @Nullable
    private static Quantity calculateContinuousVariableAggregateQuantity(
            String measureUrl, GroupDef groupDef, Function<PopulationDef, Set<Object>> popDefToResources) {

        var popDef = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);
        if (popDef == null) {
            // In the case where we're missing a measure population definition, we don't want to
            // throw an Exception, but we want the existing error handling to include this
            // error in the MeasureReport output.
            logger.warn("Measure population group has no measure population defined for measure: {}", measureUrl);
            return null;
        }

        return calculateContinuousVariableAggregateQuantity(
                groupDef.getAggregateMethod(), popDefToResources.apply(popDef));
    }

    @Nullable
    private static Quantity calculateContinuousVariableAggregateQuantity(
            ContinuousVariableObservationAggregateMethod aggregateMethod, Set<Object> qualifyingResources) {
        var observationQuantity = collectQuantities(qualifyingResources);
        return aggregate(observationQuantity, aggregateMethod);
    }

    private static Quantity aggregate(List<Quantity> quantities, ContinuousVariableObservationAggregateMethod method) {
        if (quantities == null || quantities.isEmpty()) {
            return null;
        }

        if (ContinuousVariableObservationAggregateMethod.N_A == method) {
            throw new InvalidRequestException(
                    "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
        }

        // assume all quantities share the same unit/system/code
        Quantity base = quantities.get(0);
        String unit = base.getUnit();
        String system = base.getSystem();
        String code = base.getCode();

        double result;

        switch (method) {
            case SUM:
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .sum();
                break;
            case MAX:
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .max()
                        .orElse(Double.NaN);
                break;
            case MIN:
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .min()
                        .orElse(Double.NaN);
                break;
            case AVG:
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .average()
                        .orElse(Double.NaN);
                break;
            case COUNT:
                result = quantities.size();
                break;
            case MEDIAN:
                List<Double> sorted = quantities.stream()
                        .map(q -> q.getValue().doubleValue())
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

        return new Quantity().setValue(result).setUnit(unit).setSystem(system).setCode(code);
    }

    private static List<Quantity> collectQuantities(Set<Object> resources) {
        List<Quantity> quantities = new ArrayList<>();

        for (Object resource : resources) {
            if (resource instanceof Map<?, ?> map) {
                for (Object value : map.values()) {
                    if (value instanceof Observation obs && obs.hasValueQuantity()) {
                        quantities.add(obs.getValueQuantity());
                    }
                }
            }
        }

        return quantities;
    }

    protected void scoreStratifier(
            String measureUrl,
            GroupDef groupDef,
            MeasureScoring measureScoring,
            MeasureReportGroupStratifierComponent stratifierComponent) {

        for (StratifierGroupComponent sgc : stratifierComponent.getStratum()) {

            // This isn't fantastic, but it seems to work
            final Optional<StratifierDef> optStratifierDef = groupDef.stratifiers().stream()
                    .filter(stratifierDef -> stratifierComponent.getId().equals(stratifierDef.id()))
                    .findFirst();

            if (optStratifierDef.isEmpty()) {
                throw new InternalErrorException("Stratifier component " + sgc.getId() + " does not exist.");
            }

            scoreStratum(measureUrl, groupDef, optStratifierDef.get(), measureScoring, sgc);
        }
    }

    protected void scoreStratum(
            String measureUrl,
            GroupDef groupDef,
            StratifierDef stratifierDef,
            MeasureScoring measureScoring,
            StratifierGroupComponent stratum) {
        final Quantity quantity = getStratumScoreOrNull(measureUrl, groupDef, stratifierDef, measureScoring, stratum);

        if (quantity != null) {
            stratum.setMeasureScore(quantity);
        }
    }

    @Nullable
    private Quantity getStratumScoreOrNull(
            String measureUrl,
            GroupDef groupDef,
            StratifierDef stratifierDef,
            MeasureScoring measureScoring,
            StratifierGroupComponent stratum) {

        switch (measureScoring) {
            case PROPORTION, RATIO -> {
                var score = calcProportionScore(
                        getCountFromStratifierPopulation(stratum.getPopulation(), NUMERATOR),
                        getCountFromStratifierPopulation(stratum.getPopulation(), DENOMINATOR));

                if (score != null) {
                    return new Quantity(score);
                }
                return null;
            }
            case CONTINUOUSVARIABLE -> {
                return calculateContinuousVariableAggregateQuantity(
                        measureUrl,
                        groupDef,
                        populationDef -> getResultsForStratum(populationDef, stratifierDef, stratum));
            }
            default -> {
                return null;
            }
        }
    }

    /*
    the existing algo takes the measure-observation population from the group definition and goes through all resources to get the quantities
    MeasurePopulationType.MEASUREOBSERVATION

    but we don't want that:  we want to filter only resources that belong to the patients captured by each stratum
    so we want to do some sort of wizardry that involves getting the stratum values, and using those to retrieve the associated resources

    so it's basically a hack to go from StratifierGroupComponent stratum value -> subject -> populationDef.subjectResources.get(subject)
    to get Set of resources on which to do measure scoring
     */

    // TODO:  LD: Integrate this algorithm with a new StratumDef that will be populated in R4StratifierBuilder
    private Set<Object> getResultsForStratum(
            PopulationDef measureObservationPopulationDef,
            StratifierDef stratifierDef,
            StratifierGroupComponent stratum) {

        final String stratumValue = stratum.getValue().getText();

        final Set<String> subjectsWithStratumValue = stratifierDef.getResults().entrySet().stream()
                .filter(entry -> doesStratumMatch(stratumValue, entry.getValue().rawValue()))
                .map(Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());

        return measureObservationPopulationDef.getSubjectResources().entrySet().stream()
                .filter(entry -> subjectsWithStratumValue.contains(entry.getKey()))
                .map(Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    // TODO: LD: we may need to match more types of stratum here:  The below logic deals with
    // currently anticipated use cases
    private boolean doesStratumMatch(String stratumValueAsString, Object rawValueFromStratifier) {
        if (rawValueFromStratifier == null || stratumValueAsString == null) {
            return false;
        }

        if (rawValueFromStratifier instanceof Integer rawValueFromStratifierAsInt) {
            final int stratumValueAsInt = Integer.parseInt(stratumValueAsString);

            return stratumValueAsInt == rawValueFromStratifierAsInt;
        }

        if (rawValueFromStratifier instanceof Enumeration<?> rawValueFromStratifierAsEnumeration) {
            return stratumValueAsString.equals(rawValueFromStratifierAsEnumeration.asStringValue());
        }

        if (rawValueFromStratifier instanceof String rawValueFromStratifierAsString) {
            return stratumValueAsString.equals(rawValueFromStratifierAsString);
        }

        return false;
    }

    private int getCountFromGroupPopulation(
            List<MeasureReportGroupPopulationComponent> populations, String populationName) {
        return populations.stream()
                .filter(population -> populationName.equals(
                        population.getCode().getCodingFirstRep().getCode()))
                .map(MeasureReportGroupPopulationComponent::getCount)
                .findAny()
                .orElse(0);
    }

    private int getCountFromStratifierPopulation(
            List<StratifierGroupPopulationComponent> populations, String populationName) {
        return populations.stream()
                .filter(population -> populationName.equals(
                        population.getCode().getCodingFirstRep().getCode()))
                .map(StratifierGroupPopulationComponent::getCount)
                .findAny()
                .orElse(0);
    }
}
