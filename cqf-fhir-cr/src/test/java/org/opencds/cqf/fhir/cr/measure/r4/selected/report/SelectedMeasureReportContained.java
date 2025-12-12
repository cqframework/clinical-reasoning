package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.cr.measure.common.MeasureInfo.EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_SYSTEM_URL;

import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportContained extends Selected<Resource, SelectedMeasureReport> {

    public SelectedMeasureReportContained(Resource value, SelectedMeasureReport parent) {
        super(value, parent);
    }

    public SelectedMeasureReportContained observationHasExtensionUrl() {
        var obs = (Observation) value();
        assertEquals(EXT_URL, obs.getExtension().get(0).getUrl());
        return this;
    }

    /**
     * only applicable to individual reports
     * @return
     */
    public SelectedMeasureReportContained observationHasSDECoding() {
        assert value() instanceof Observation;
        var obs = (Observation) value();
        assertEquals(SDE_SYSTEM_URL, obs.getCode().getCodingFirstRep().getSystem());
        assertEquals("supplemental-data", obs.getCode().getCodingFirstRep().getCode());
        return this;
    }

    public SelectedMeasureReportContained observationHasCode(String code) {
        var obs = (Observation) value();
        assertEquals(code, obs.getCode().getCoding().get(0).getCode());
        return this;
    }

    public SelectedMeasureReportContained observationCount(int count) {
        var obs = (Observation) value();
        assertEquals(count, obs.getValueIntegerType().getValue());
        return this;
    }
}
