package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.opencds.cqf.fhir.cr.measure.common.ExtensionDef;

public class SelectedMeasureDefPopulationExtension<P>
        extends org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected<ExtensionDef, P> {
    public SelectedMeasureDefPopulationExtension(ExtensionDef value, P parent) {
        super(value, parent);
    }

    public ExtensionDef extensionDef() {
        return value();
    }

    public SelectedMeasureDefPopulationExtension<P> extensionDefHasResults() {
        assertFalse(this.extensionDef().getSubjectResources().isEmpty());
        return this;
    }
}
