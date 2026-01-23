package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selector;

public class SelectedMeasureReportStratifier
        extends Selected<MeasureReport.MeasureReportGroupStratifierComponent, SelectedMeasureReportGroup> {

    public SelectedMeasureReportStratifier(
            MeasureReportGroupStratifierComponent value, SelectedMeasureReportGroup parent) {
        super(value, parent);
    }

    public SelectedMeasureReportStratum firstStratum() {
        return stratum(MeasureReport.MeasureReportGroupStratifierComponent::getStratumFirstRep);
    }

    public SelectedMeasureReportStratifier hasNoStratum() {
        assertNull(value());
        return this;
    }

    public SelectedMeasureReportStratifier hasStratumCount(int stratumCount) {
        assertNotNull(value());
        final List<StratifierGroupComponent> stratum = value().getStratum();
        assertEquals(stratumCount, stratum.size());
        return this;
    }

    // Position is the numerical position starting at 1 for the first
    public SelectedMeasureReportStratum stratumByPosition(int position) {
        assertTrue(value().getStratum().size() >= position && position > 0);

        return new SelectedMeasureReportStratum(value().getStratum().get(position - 1), this);
    }

    public SelectedMeasureReportStratum stratumByText(String stratumText) {
        final Optional<StratifierGroupComponent> optMatchingStratum = value().getStratum().stream()
                .filter(stratum -> stratumText.equals(stratum.getValue().getText()))
                .findFirst();

        assertTrue(optMatchingStratum.isPresent(), "Could not find stratum with text: %s".formatted(stratumText));

        return new SelectedMeasureReportStratum(optMatchingStratum.get(), this);
    }

    public SelectedMeasureReportStratifier hasStratum(String textValue) {
        final SelectedMeasureReportStratum stratum = stratum(textValue);
        assertNotNull(stratum.value());
        return this;
    }

    public SelectedMeasureReportStratum stratum(CodeableConcept value) {
        return stratum(s -> s.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().equalsDeep(value))
                .findFirst()
                .orElse(null));
    }

    public SelectedMeasureReportStratum stratum(String textValue) {
        return stratum(s -> s.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().hasText())
                .filter(x -> x.getValue().getText().equals(textValue))
                .findFirst()
                .orElse(null));
    }

    public SelectedMeasureReportStratum stratumByComponentValueText(String textValue) {
        return stratum(s -> s.getStratum().stream()
                .filter(x -> x.getComponent().stream()
                        .anyMatch(t -> t.getValue().getText().equals(textValue)))
                .findFirst()
                .orElse(null));
    }

    public SelectedMeasureReportStratum stratumByComponentCodeText(String textValue) {
        return stratum(s -> s.getStratum().stream()
                .filter(x -> x.getComponent().stream()
                        .anyMatch(t -> t.getCode().getText().equals(textValue)))
                .findFirst()
                .orElse(null));
    }

    public SelectedMeasureReportStratum stratum(
            Selector<MeasureReport.StratifierGroupComponent, MeasureReport.MeasureReportGroupStratifierComponent>
                    stratumSelector) {
        var s = stratumSelector.select(value());
        return new SelectedMeasureReportStratum(s, this);
    }

    public SelectedMeasureReportStratifier hasCodeText(String stratifierCodeText) {
        var firstCodeText = value().getCode().stream()
                .map(CodeableConcept::getText)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        assertEquals(
                stratifierCodeText,
                firstCodeText,
                "Stratifier does not have expected code: %s but instead has: %s"
                        .formatted(stratifierCodeText, firstCodeText));

        return this;
    }
}
