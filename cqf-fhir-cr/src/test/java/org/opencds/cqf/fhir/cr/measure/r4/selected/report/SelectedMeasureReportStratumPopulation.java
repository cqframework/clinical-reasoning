package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportStratumPopulation
        extends Selected<MeasureReport.StratifierGroupPopulationComponent, SelectedMeasureReportStratum> {

    public SelectedMeasureReportStratumPopulation(
            MeasureReport.StratifierGroupPopulationComponent value, SelectedMeasureReportStratum parent) {
        super(value, parent);
    }

    public SelectedMeasureReportStratumPopulation hasCount(int count) {
        assertEquals(count, this.value().getCount());
        return this;
    }

    /**
     * if population has a count>0 and mode= subject-list, then population should have a subjectResult reference
     * @return assertNotNull
     */
    public SelectedMeasureReportStratumPopulation hasStratumPopulationSubjectResults() {
        assertNotNull(value().getSubjectResults().getReference());
        return this;
    }
    /**
     * if population has a count=0 and mode= subject-list, then population should NOT have a subjectResult reference
     * @return assertNull
     */
    public SelectedMeasureReportStratumPopulation hasNoStratumPopulationSubjectResults() {
        assertNull(value().getSubjectResults().getReference());
        return this;
    }

    public SelectedMeasureReportStratumPopulation hasCode(MeasurePopulationType expectedMeasurePopulationType) {
        assertNotNull(value());
        assertEquals(
                expectedMeasurePopulationType.toCode(),
                value().getCode().getCodingFirstRep().getCode());
        return this;
    }
}
