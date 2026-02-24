package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponentComponent;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportStratumComponent
        extends Selected<StratifierGroupComponentComponent, SelectedMeasureReportStratum> {

    public SelectedMeasureReportStratumComponent(
            StratifierGroupComponentComponent value, SelectedMeasureReportStratum parent) {
        super(value, parent);
    }

    public SelectedMeasureReportStratumComponent hasCodeText(String expectedCodeText) {
        assertTrue(
                value().hasCode() && value().getCode().hasText(),
                "Expected component code text: %s, but component has no code text".formatted(expectedCodeText));

        assertEquals(
                expectedCodeText,
                value().getCode().getText(),
                "Expected component code text: %s, but got: %s"
                        .formatted(expectedCodeText, value().getCode().getText()));

        return this;
    }

    public SelectedMeasureReportStratumComponent hasValueText(String expectedValueText) {
        assertTrue(
                value().hasValue() && value().getValue().hasText(),
                "Expected component value text: %s, but component has no value text".formatted(expectedValueText));

        assertEquals(
                expectedValueText,
                value().getValue().getText(),
                "Expected component value text: %s, but got: %s"
                        .formatted(expectedValueText, value().getValue().getText()));

        return this;
    }
}
