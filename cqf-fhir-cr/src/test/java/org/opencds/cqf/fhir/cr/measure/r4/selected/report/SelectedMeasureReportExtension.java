package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CRITERIA_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_REFERENCE_EXT_URL;

import org.hl7.fhir.r4.model.Extension;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportExtension extends Selected<Extension, SelectedMeasureReport> {

    public SelectedMeasureReportExtension(Extension value, SelectedMeasureReport parent) {
        super(value, parent);
    }

    public SelectedMeasureReportExtension extensionHasSDEUrl() {
        assertEquals(SDE_REFERENCE_EXT_URL, value().getUrl());
        return this;
    }

    public SelectedMeasureReportExtension extensionHasSDEId(String id) {
        assertEquals(
                id,
                value().getValue()
                        .getExtensionByUrl(EXT_CRITERIA_REFERENCE_URL)
                        .getValue()
                        .toString());
        return this;
    }
}
