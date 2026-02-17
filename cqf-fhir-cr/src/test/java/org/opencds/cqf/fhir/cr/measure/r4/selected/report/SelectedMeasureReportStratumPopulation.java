package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
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

    // ==================== Extension Assertions ====================

    public SelectedMeasureReportStratumPopulation hasNoAggregationResultsExtensionValue() {
        return hasAggregationResultsExtensionValue(null);
    }

    public SelectedMeasureReportStratumPopulation hasAggregationResultsExtensionValue(
            Double expectedAggregationResultsExtensionValue) {
        assertNotNull(value(), "StratifierGroupPopulationComponent is null");
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
                    "Stratum population aggregation result extension value mismatch");
        } else {
            fail("Stratum population aggregation result extension value is not a decimal type: "
                    + extensionValue.primitiveValue());
        }

        return this;
    }

    public SelectedMeasureReportStratumPopulation hasNoAggregateMethodExtension() {
        return hasAggregateMethodExtension(null);
    }

    public SelectedMeasureReportStratumPopulation hasAggregateMethodExtension(
            ContinuousVariableObservationAggregateMethod expectedAggregateMethod) {
        assertNotNull(value(), "StratifierGroupPopulationComponent is null");
        final Extension extension = value().getExtensionByUrl(MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL);

        if (null == expectedAggregateMethod) {
            assertNull(extension, "extension EXT_CQFM_AGGREGATE_METHOD_URL is not null");
            return this;
        }

        assertNotNull(extension, "extension EXT_CQFM_AGGREGATE_METHOD_URL is null");

        final Type extensionValue = extension.getValue();

        if (extensionValue instanceof StringType actualAggregateMethodExtension) {
            assertEquals(
                    expectedAggregateMethod.getText(),
                    actualAggregateMethodExtension.getValue(),
                    "Stratum population aggregate method extension value mismatch");
        } else {
            fail("Stratum population aggregate method extension value is not a string type: "
                    + extensionValue.primitiveValue());
        }

        return this;
    }

    public SelectedMeasureReportStratumPopulation hasNoCriteriaReferenceExtension() {
        return hasCriteriaReferenceExtension(null);
    }

    public SelectedMeasureReportStratumPopulation hasCriteriaReferenceExtension(String expectedCriteriaReference) {
        assertNotNull(value(), "StratifierGroupPopulationComponent is null");
        final Extension extension = value().getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);

        if (null == expectedCriteriaReference) {
            assertNull(extension, "extension EXT_CQFM_CRITERIA_REFERENCE is not null");
            return this;
        }

        assertNotNull(extension, "extension EXT_CQFM_CRITERIA_REFERENCE is null");

        final Type extensionValue = extension.getValue();

        if (extensionValue instanceof StringType actualCriteriaReferenceExtension) {
            assertEquals(
                    expectedCriteriaReference,
                    actualCriteriaReferenceExtension.getValue(),
                    "Stratum population criteria reference extension value mismatch");
        } else {
            fail("Stratum population criteria reference extension value is not a string type: "
                    + extensionValue.primitiveValue());
        }

        return this;
    }
}
