package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SUPPORTING_EVIDENCE_URL;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Validator;
import org.opencds.cqf.fhir.cr.measure.r4.MeasureValidationUtils;

public class SelectedMeasureReportPopulation
        extends Selected<MeasureReportGroupPopulationComponent, SelectedMeasureReportGroup> {

    public SelectedMeasureReportPopulation(
            MeasureReportGroupPopulationComponent value, SelectedMeasureReportGroup parent) {
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
            Validator<MeasureReportGroupPopulationComponent> populationValidator) {
        populationValidator.validate(value());
        return this;
    }

    public SelectedMeasureReportPopulationExt getPopulationExtension(String expressionName) {
        // pop extensions that are for supporting evidence
        var evidenceExt = value.getExtension().stream()
                .filter(e -> e.getUrl().equals(EXT_SUPPORTING_EVIDENCE_URL))
                .findFirst()
                .orElse(null);
        assertNotNull(evidenceExt, "No extension found for SUPPORTING_EVIDENCE_URL");
        Extension expressionExtension = null;
        for (Extension extension : evidenceExt.getExtension()) {
            if (extension.getUrl().equals(expressionName)) {
                expressionExtension = extension;
                break;
            }
        }
        assertNotNull(expressionExtension, String.format("No extension found with expressionName: %s", expressionName));
        return new SelectedMeasureReportPopulationExt(expressionExtension, this);
    }
}
