package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.EXT_SUPPORTING_EVIDENCE_URL;

import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Validator;
import org.opencds.cqf.fhir.cr.measure.r4.MeasureValidationUtils;

public class SelectedMeasureReportPopulation
        extends Selected<MeasureReportGroupPopulationComponent, SelectedMeasureReportGroup> {

    public SelectedMeasureReportPopulation(
            MeasureReportGroupPopulationComponent value, SelectedMeasureReportGroup parent) {
        super(value, parent);
    }

    public SelectedMeasureReportPopulation hasCode(MeasurePopulationType expectedMeasurePopulationType) {
        assertNotNull(value(), "PopulationDef is null");

        assertTrue(value().hasCode(), "Measure population does not have a code!");
        assertEquals(
                expectedMeasurePopulationType.toCode(),
                value().getCode().getCodingFirstRep().getCode(),
                "Expected population type: %s, but got: %s"
                        .formatted(
                                expectedMeasurePopulationType.toCode(),
                                value().getCode().getCodingFirstRep().getCode()));
        return this;
    }

    public SelectedMeasureReportPopulation hasCount(int count) {
        MeasureValidationUtils.validatePopulation(value(), count);
        return this;
    }

    public SelectedMeasureReportPopulation hasSubjectResults() {
        assertNotNull(value().getSubjectResults().getReference());
        return this;
    }

    public SelectedMeasureReportPopulation hasNoAggregationResultsExtensionValue() {
        return hasAggregationResultsExtensionValue(null);
    }

    public SelectedMeasureReportPopulation hasAggregationResultsExtensionValue(
            Double expectedAggregationResultsExtensionValue) {
        assertNotNull(value(), "PopulationDef is null");
        final Extension extension = value().getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);

        if (null == expectedAggregationResultsExtensionValue) {
            assertNull(extension, "extension EXT_AGGREGATION_METHOD_RESULT is not null");
            return this;
        }

        assertNotNull(extension, "extension EXT_AGGREGATION_METHOD_RESULT is null");

        final Type extensionValue = extension.getValue();

        if (extensionValue instanceof DecimalType actualAggregationResultsExtensionValue) {
            assertEquals(
                    expectedAggregationResultsExtensionValue,
                    actualAggregationResultsExtensionValue.getValueAsNumber().doubleValue(),
                    "Population aggregation result extension value mismatch");
        } else {
            fail("Population aggregation result extension value is not a decimal type: "
                    + extensionValue.primitiveValue());
        }

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
