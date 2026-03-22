package org.opencds.cqf.fhir.cr.measure.r4.selected.def;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;
import org.opencds.cqf.fhir.cr.measure.common.SupportingEvidenceDef;

public class SelectedMeasureDefPopulationExtension<P>
        extends org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected<SupportingEvidenceDef, P> {

    private final MeasureEvaluationState state;

    public SelectedMeasureDefPopulationExtension(SupportingEvidenceDef value, MeasureEvaluationState state, P parent) {
        super(value, parent);
        this.state = state;
    }

    public SupportingEvidenceDef extensionDef() {
        return value();
    }

    public SelectedMeasureDefPopulationExtension<P> extensionDefHasResults() {
        assertFalse(state.supportingEvidence(this.extensionDef())
                .getSubjectResources()
                .isEmpty());
        return this;
    }
}
