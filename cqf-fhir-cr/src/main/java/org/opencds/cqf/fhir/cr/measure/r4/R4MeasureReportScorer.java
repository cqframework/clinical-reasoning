package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
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
public class R4MeasureReportScorer extends BaseMeasureReportScorer<MeasureReport> {

    private static final Logger logger = LoggerFactory.getLogger(MeasureProcessorUtils.class);

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
            case PROPORTION:
            case RATIO:
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
            scoreStratifier(measureScoring, stratifierComponent);
        }
    }

    protected void scoreContinuousVariable(String measureUrl, MeasureReportGroupComponent mrgc, GroupDef groupDef) {
        // LUKETODO:  this could be null:  is it possible that any test that fails here is badly formed?
        var popDef = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);
        if (popDef == null) {
            // In the case where we're missing a measure population definition, we don't want to
            // throw an Exception, but we want the existing error handling to include this
            // error in the MeasureReport output.
            logger.warn("Measure population group has no measure population defined for measure: {}", measureUrl);
            return;
        }
        var observationQuantity = collectQuantities(popDef.getResources());
        var aggregateMethod = groupDef.getAggregateMethod();
        mrgc.setMeasureScore(aggregate(observationQuantity, aggregateMethod));
    }

    public static Quantity aggregate(List<Quantity> quantities, String method) {
        if (quantities == null || quantities.isEmpty()) {
            return null;
        }

        // assume all quantities share the same unit/system/code
        Quantity base = quantities.get(0);
        String unit = base.getUnit();
        String system = base.getSystem();
        String code = base.getCode();

        double result;

        switch (method.toLowerCase()) {
            case "sum":
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .sum();
                break;
            case "max":
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .max()
                        .orElse(Double.NaN);
                break;
            case "min":
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .min()
                        .orElse(Double.NaN);
                break;
            case "avg":
                result = quantities.stream()
                        .mapToDouble(q -> q.getValue().doubleValue())
                        .average()
                        .orElse(Double.NaN);
                break;
            case "count":
                result = quantities.size();
                break;
            case "median":
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

    public List<Quantity> collectQuantities(Set<Object> resources) {
        List<Quantity> quantities = new ArrayList<>();

        for (Object resource : resources) {
            if (resource instanceof Map<?, ?> map) {
                for (Object value : map.values()) {
                    if (value instanceof Observation obs) {
                        if (obs.hasValueQuantity()) {
                            // LUKETODO:  get rid of this during final cleanup
                            logger.info(
                                    "1234: observation value quantity: {}",
                                    obs.getValueQuantity().getValue().doubleValue());
                            quantities.add(obs.getValueQuantity());
                        }
                    }
                }
            }
        }

        return quantities;
    }

    protected void scoreStratum(MeasureScoring measureScoring, StratifierGroupComponent stratum) {
        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                var score = calcProportionScore(
                        getCountFromStratifierPopulation(stratum.getPopulation(), NUMERATOR),
                        getCountFromStratifierPopulation(stratum.getPopulation(), DENOMINATOR));
                if (score != null) {
                    stratum.setMeasureScore(new Quantity(score));
                }
                break;
            default:
                break;
        }
    }

    protected void scoreStratifier(
            MeasureScoring measureScoring, MeasureReportGroupStratifierComponent stratifierComponent) {
        for (StratifierGroupComponent sgc : stratifierComponent.getStratum()) {
            scoreStratum(measureScoring, sgc);
        }
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
