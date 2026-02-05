package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.Period;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selector;
import org.opencds.cqf.fhir.cr.measure.r4.MeasureValidationUtils;

public class SelectedMeasureReportGroup
        extends Selected<MeasureReport.MeasureReportGroupComponent, SelectedMeasureReport> {

    public SelectedMeasureReportGroup(MeasureReportGroupComponent value, SelectedMeasureReport parent) {
        super(value, parent);
    }

    public SelectedMeasureReportGroup hasPopulationCount(int count) {
        assertEquals(count, this.value().getPopulation().size());
        return this;
    }

    public SelectedMeasureReportGroup hasScore(String score) {
        MeasureValidationUtils.validateGroupScore(this.value(), score);
        return this;
    }

    public SelectedMeasureReportGroup hasMeasureScore(boolean hasScore) {
        assertEquals(hasScore, this.value().hasMeasureScore());
        return this;
    }

    public SelectedMeasureReportGroup hasImprovementNotationExt(String code) {
        var improvementNotationExt = value().getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION);
        assertNotNull(improvementNotationExt);
        var codeConcept = (CodeableConcept) improvementNotationExt.getValue();
        assertTrue(codeConcept.hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, code));

        return this;
    }

    public SelectedMeasureReportGroup hasNoImprovementNotationExt() {
        var improvementNotationExt = value().getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION);
        assertNull(improvementNotationExt);
        return this;
    }

    /**
     * Assert that the group has a cqfm-scoring extension with the specified code.
     * This extension is present when the group has its own scoring type (i.e., when
     * the measure does NOT have measure-level scoring).
     * <p>
     * Extension URL: http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoring
     *
     * @param code the expected scoring code (e.g., "proportion", "ratio")
     * @return this SelectedMeasureReportGroup for chaining
     */
    public SelectedMeasureReportGroup hasGroupScoringExt(String code) {
        var scoringExt = value().getExtensionByUrl(MeasureConstants.CQFM_SCORING_EXT_URL);
        assertNotNull(scoringExt, "Expected cqfm-scoring extension but none found");
        var codeConcept = (CodeableConcept) scoringExt.getValue();
        String actualCode = codeConcept.getCodingFirstRep().getCode();
        assertTrue(
                codeConcept.hasCoding(MeasureConstants.CQFM_SCORING_SYSTEM_URL, code),
                "Expected cqfm-scoring extension code: %s, actual: %s".formatted(code, actualCode));
        return this;
    }

    /**
     * Assert that the group has a cqfm-scoring extension with the specified MeasureScoring type.
     * This is a convenience method that extracts the code from the enum.
     *
     * @param scoring the expected MeasureScoring type
     * @return this SelectedMeasureReportGroup for chaining
     */
    public SelectedMeasureReportGroup hasGroupScoringExt(MeasureScoring scoring) {
        return hasGroupScoringExt(scoring.toCode());
    }

    /**
     * Assert that the group does NOT have a cqfm-scoring extension.
     * This is the normal case when the measure has measure-level scoring.
     *
     * @return this SelectedMeasureReportGroup for chaining
     */
    public SelectedMeasureReportGroup hasNoGroupScoringExt() {
        var scoringExt = value().getExtensionByUrl(MeasureConstants.CQFM_SCORING_EXT_URL);
        String actualCode = scoringExt != null
                ? ((CodeableConcept) scoringExt.getValue()).getCodingFirstRep().getCode()
                : null;
        assertNull(
                scoringExt,
                "Expected no cqfm-scoring extension (null), but found extension with code: %s".formatted(actualCode));
        return this;
    }

    public SelectedMeasureReportGroup hasDateOfCompliance() {
        assertEquals(
                CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL,
                this.value()
                        .getExtensionsByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                        .get(0)
                        .getUrl());
        assertFalse(this.value()
                .getExtensionsByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                .get(0)
                .getValue()
                .isEmpty());
        assertInstanceOf(
                Period.class,
                this.value()
                        .getExtensionsByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                        .get(0)
                        .getValue());
        return this;
    }

    public SelectedMeasureReportPopulation population(String name) {
        return this.population(g -> g.getPopulation().stream()
                .filter(x -> x.hasCode()
                        && x.getCode().hasCoding()
                        && x.getCode().getCoding().get(0).getCode().equals(name))
                .findFirst()
                .get());
    }

    public SelectedMeasureReportPopulation populationId(String populationId) {
        SelectedMeasureReportPopulation population = this.population(g -> g.getPopulation().stream()
                .filter(x -> x.getId().equals(populationId))
                .findFirst()
                .orElse(null));

        assertNotNull(population, "Population not found: " + populationId);

        return population;
    }

    public SelectedMeasureReportPopulation population(
            Selector<MeasureReportGroupPopulationComponent, MeasureReportGroupComponent> populationSelector) {
        var p = populationSelector.select(value());
        return new SelectedMeasureReportPopulation(p, this);
    }

    public SelectedMeasureReportPopulation firstPopulation() {
        return this.population(MeasureReport.MeasureReportGroupComponent::getPopulationFirstRep);
    }

    public SelectedMeasureReportGroup hasStratifierCount(int count) {
        assertEquals(count, this.value().getStratifier().size());
        return this;
    }

    public SelectedMeasureReportStratifier firstStratifier() {
        return this.stratifier(MeasureReport.MeasureReportGroupComponent::getStratifierFirstRep);
    }

    public SelectedMeasureReportStratifier stratifierById(String stratId) {
        final SelectedMeasureReportStratifier stratifier = this.stratifier(g -> g.getStratifier().stream()
                .filter(t -> t.getId().equals(stratId))
                .findFirst()
                .orElse(null));

        assertNotNull(stratifier);

        return stratifier;
    }

    public SelectedMeasureReportStratifier stratifier(
            Selector<MeasureReportGroupStratifierComponent, MeasureReportGroupComponent> stratifierSelector) {
        var s = stratifierSelector.select(value());
        return new SelectedMeasureReportStratifier(s, this);
    }
}
