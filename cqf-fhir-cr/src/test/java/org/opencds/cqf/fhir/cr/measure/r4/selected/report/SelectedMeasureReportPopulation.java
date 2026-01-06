package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Validator;
import org.opencds.cqf.fhir.cr.measure.r4.MeasureValidationUtils;

public class SelectedMeasureReportPopulation
        extends Selected<MeasureReport.MeasureReportGroupPopulationComponent, SelectedMeasureReportGroup> {

    public SelectedMeasureReportPopulation(
            MeasureReport.MeasureReportGroupPopulationComponent value, SelectedMeasureReportGroup parent) {
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
            Validator<MeasureReport.MeasureReportGroupPopulationComponent> populationValidator) {
        populationValidator.validate(value());
        return this;
    }
}
