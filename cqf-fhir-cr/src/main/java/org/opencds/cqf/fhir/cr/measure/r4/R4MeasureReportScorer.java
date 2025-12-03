package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.QuantityDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueDef;
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
public class R4MeasureReportScorer implements MeasureReportScorer<MeasureReport> {

    private static final Logger logger = LoggerFactory.getLogger(R4MeasureReportScorer.class);

    @Override
    public ContinuousVariableObservationConverter<Quantity> getContinuousVariableConverter() {
        return R4ContinuousVariableObservationConverter.INSTANCE;
    }

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
        // Delegate to base class version-agnostic method
        return getGroupDefById(measureDef, mrgc.getId());
    }

    protected MeasureScoring getGroupMeasureScoring(MeasureReportGroupComponent mrgc, MeasureDef measureDef) {
        // Delegate to base class version-agnostic method
        return getGroupMeasureScoringById(measureDef, mrgc.getId());
    }

    protected void scoreGroup(Double score, boolean isIncreaseImprovementNotation, MeasureReportGroupComponent mrgc) {
        // When applySetMembership=false, this value can receive strange values
        // This should prevent scoring in certain scenarios like <0
        if (score != null && score >= 0) {
            if (isIncreaseImprovementNotation) {
                mrgc.setMeasureScore(new Quantity(score));
            } else {
                mrgc.setMeasureScore(new Quantity(1 - score));
            }
        }
    }

    protected void scoreGroup(
            String measureUrl,
            MeasureScoring measureScoring,
            MeasureReportGroupComponent mrgc,
            boolean isIncreaseImprovementNotation,
            GroupDef groupDef) {

        switch (measureScoring) {
            case PROPORTION, RATIO:
                // Calculate score using base class method (version-agnostic)
                Double score = calculateGroupScore(measureUrl, measureScoring, groupDef);
                // Assign score to R4 component (version-specific)
                scoreGroup(score, isIncreaseImprovementNotation, mrgc);
                break;

            case CONTINUOUSVARIABLE:
                // increase notation cannot be applied to ContVariable
                scoreContinuousVariable(measureUrl, mrgc, getFirstMeasureObservation(groupDef));
                break;
            default:
                break;
        }

        for (MeasureReportGroupStratifierComponent stratifierComponent : mrgc.getStratifier()) {
            scoreStratifier(measureUrl, groupDef, measureScoring, stratifierComponent);
        }
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to convert QuantityDef at the end
    protected void scoreContinuousVariable(
            String measureUrl, MeasureReportGroupComponent mrgc, PopulationDef populationDef) {
        final QuantityDef aggregateQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, populationDef, PopulationDef::getAllSubjectResources);

        // Convert QuantityDef to R4 Quantity at the last moment before setting on report
        Quantity aggregateQuantity = getContinuousVariableConverter().convertToFhirQuantity(aggregateQuantityDef);
        mrgc.setMeasureScore(aggregateQuantity);
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

            final StratifierDef stratifierDef = optStratifierDef.get();

            final StratumDef stratumDef = stratifierDef.getStratum().stream()
                    .filter(stratumDefInner -> matchesStratumTextForR4(
                            stratumDefInner, sgc.getValue().getText(), stratifierDef))
                    .findFirst()
                    .orElse(null);

            // TODO: LD: should we always expect these to match up?
            if (stratumDef == null) {
                logger.warn("stratumDef is null");
            }

            scoreStratum(measureUrl, groupDef, stratumDef, measureScoring, sgc);
        }
    }

    /**
     * Get the text representation of a stratum for R4.
     * This method handles R4-specific CodeableConcept instances.
     *
     * @param stratumDef the StratumDef
     * @param stratifierDef the StratifierDef for context
     * @return the text representation, or null if no text can be determined
     */
    protected String getStratumTextForR4(StratumDef stratumDef, StratifierDef stratifierDef) {
        String stratumText = null;

        for (StratumValueDef valuePair : stratumDef.valueDefs()) {
            var value = valuePair.value();
            var componentDef = valuePair.def();
            // Set Stratum value to indicate which value is displaying results
            // ex. for Gender stratifier, code 'Male'
            if (value.getValue() instanceof CodeableConcept) {
                if (stratumDef.isComponent()) {
                    // component stratifier example: code: "gender", value: 'M'
                    // value being stratified: 'M'
                    stratumText = componentDef.code().text();
                } else {
                    // non-component stratifiers only set stratified value, code is set on stratifier object
                    // value being stratified: 'M'
                    if (value.getValue() instanceof CodeableConcept codeableConcept) {
                        stratumText = codeableConcept.getText();
                    }
                }
            } else if (stratumDef.isComponent()) {
                stratumText = value.getValueAsString();
            } else if (MeasureStratifierType.VALUE == stratifierDef.getStratifierType()) {
                // non-component stratifiers only set stratified value, code is set on stratifier object
                // value being stratified: 'M'
                stratumText = value.getValueAsString();
            } else if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
                // Handle CRITERIA-type stratifiers with non-CodeableConcept values (e.g., String, Boolean)
                stratumText = value.getValueAsString();
            }
        }

        return stratumText;
    }

    /**
     * Check if a stratum matches a given text value for R4.
     *
     * @param stratumDef the StratumDef
     * @param text the text to match against
     * @param stratifierDef the StratifierDef for context
     * @return true if the stratum text matches the given text
     */
    protected boolean matchesStratumTextForR4(StratumDef stratumDef, String text, StratifierDef stratifierDef) {
        return java.util.Objects.equals(getStratumTextForR4(stratumDef, stratifierDef), text);
    }

    protected void scoreStratum(
            String measureUrl,
            GroupDef groupDef,
            StratumDef stratumDef,
            MeasureScoring measureScoring,
            StratifierGroupComponent stratum) {
        final Quantity quantity = (Quantity) getStratumScoreOrNull(measureUrl, groupDef, stratumDef, measureScoring);

        if (quantity != null) {
            stratum.setMeasureScore(quantity);
        }
    }
}
