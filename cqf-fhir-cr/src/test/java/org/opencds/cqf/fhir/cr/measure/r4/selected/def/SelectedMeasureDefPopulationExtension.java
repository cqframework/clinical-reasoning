package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.opencds.cqf.fhir.cr.measure.common.SupportingEvidenceDef;

public class SelectedMeasureDefPopulationExtension<P>
        extends org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected<SupportingEvidenceDef, P> {
    public SelectedMeasureDefPopulationExtension(SupportingEvidenceDef value, P parent) {
        super(value, parent);
    }

    public SupportingEvidenceDef extensionDef() {
        return value();
    }

    public SelectedMeasureDefPopulationExtension<P> extensionDefHasResults() {
        assertFalse(this.extensionDef().getSubjectResources().isEmpty());
        return this;
    }
}
