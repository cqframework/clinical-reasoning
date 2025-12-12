package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Validator;
import org.opencds.cqf.fhir.cr.measure.r4.MeasureValidationUtils;

public class SelectedMeasureReportPopulation
        extends Selected<MeasureReport.MeasureReportGroupPopulationComponent, SelectedMeasureReportGroup> {

    public SelectedMeasureReportPopulation(
            MeasureReport.MeasureReportGroupPopulationComponent value, SelectedMeasureReportGroup parent) {
        super(value, parent);
    }

    public SelectedMeasureReportPopulation hasCount(int count) {
        MeasureValidationUtils.validatePopulation(value(), count);
        return this;
    }

    public SelectedMeasureReportPopulation hasSubjectResults() {
        assertNotNull(value().getSubjectResults().getReference());
        return this;
    }

    public SelectedMeasureReportPopulation passes(
            Validator<MeasureReport.MeasureReportGroupPopulationComponent> populationValidator) {
        populationValidator.validate(value());
        return this;
    }
}
