package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.EXT_SUPPORTING_EVIDENCE_URL;

import java.util.List;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
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

    public SelectedMeasureReportPopulationExt getPopulationExtension(String supportingEvidenceName) {
        // pop extensions that are for supporting evidence
        List<Extension> evidenceExts = value.getExtension().stream()
                .filter(e -> e.getUrl().equals(EXT_SUPPORTING_EVIDENCE_URL))
                .toList();
        assertNotNull(evidenceExts, "No extension found for SUPPORTING_EVIDENCE_URL");
        Extension expressionExtension = null;
        // look through all supporting evidence Extensions for population
        for (Extension extension : evidenceExts) {
            // then look for "name" field with matching name value
            for (Extension ext : extension.getExtension().stream()
                    .filter(x -> x.getUrl().toString().equals("name"))
                    .toList()) {
                if (ext.getValue() != null && ext.getValue().toString().equals(supportingEvidenceName)) {
                    expressionExtension = extension;
                    break;
                }
            }
            if (expressionExtension != null) {
                break;
            }
        }
        assertNotNull(expressionExtension, String.format("No extension found with 'name': %s", supportingEvidenceName));
        return new SelectedMeasureReportPopulationExt(expressionExtension, this);
    }

    public SelectedMeasureReportPopulation assertNoSupportingEvidence() {

        List<Extension> evidenceExts = value.getExtension().stream()
                .filter(e -> EXT_SUPPORTING_EVIDENCE_URL.equals(e.getUrl()))
                .toList();

        assertTrue(
                evidenceExts.isEmpty(), "Expected NO supportingEvidence extensions, but found " + evidenceExts.size());

        return this;
    }

    public SelectedMeasureReportPopulation hasNoAggregateMethodExtension() {
        return hasAggregateMethodExtension(null);
    }

    public SelectedMeasureReportPopulation hasAggregateMethodExtension(
            ContinuousVariableObservationAggregateMethod expectedAggregateMethod) {
        assertNotNull(value(), "PopulationDef is null");
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
                    "Population aggregate method extension value mismatch");
        } else {
            fail("Population aggregate method extension value is not a string type: "
                    + extensionValue.primitiveValue());
        }

        return this;
    }

    public SelectedMeasureReportPopulation hasNoCriteriaReferenceExtension() {
        return hasCriteriaReferenceExtension(null);
    }

    /**
     * Assert the population criteria reference extension (for MEASUREOBSERVATION populations).
     * Performs comprehensive validation:
     * - Verifies the criteria reference extension matches the expected value
     * - Validates that the referenced population exists and has code NUMERATOR or DENOMINATOR
     * - Applies heuristic validation based on population ID naming (e.g., "observation-num" must reference numerator)
     *
     * @param expectedCriteriaReference expected criteria reference value, or null for no criteria reference
     * @return this SelectedMeasureReportPopulation for chaining
     */
    public SelectedMeasureReportPopulation hasCriteriaReferenceExtension(String expectedCriteriaReference) {
        assertNotNull(value(), "PopulationDef is null");
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
                    "Population criteria reference extension value mismatch");
        } else {
            fail("Population criteria reference extension value is not a string type: "
                    + extensionValue.primitiveValue());
        }

        // Find the referenced population by ID
        var referencedPopulation = parent.value().getPopulation().stream()
                .filter(p -> expectedCriteriaReference.equalsIgnoreCase(p.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(
                referencedPopulation,
                String.format(
                        "Criteria reference '%s' does not match any population ID in the group. Available populations: %s",
                        expectedCriteriaReference,
                        parent.value().getPopulation().stream()
                                .map(Element::getId)
                                .toList()));

        // Validate that the referenced population has code NUMERATOR or DENOMINATOR
        String refCode = referencedPopulation.getCode().getCodingFirstRep().getCode();
        assertTrue(
                MeasurePopulationType.NUMERATOR.toCode().equals(refCode)
                        || MeasurePopulationType.DENOMINATOR.toCode().equals(refCode),
                String.format(
                        "Criteria reference '%s' points to population with code '%s', but must be numerator or denominator",
                        expectedCriteriaReference, refCode));

        // Apply heuristic validation based on current population ID
        String currentPopId = value().getId();
        if (currentPopId != null) {
            String lowerCaseId = currentPopId.toLowerCase();
            if (lowerCaseId.contains("num")) {
                assertEquals(
                        MeasurePopulationType.NUMERATOR.toCode(),
                        refCode,
                        String.format(
                                "Population ID '%s' contains 'num' but references '%s' which has code '%s' instead of numerator",
                                currentPopId, expectedCriteriaReference, refCode));
            } else if (lowerCaseId.contains("den")) {
                assertEquals(
                        MeasurePopulationType.DENOMINATOR.toCode(),
                        refCode,
                        String.format(
                                "Population ID '%s' contains 'den' but references '%s' which has code '%s' instead of denominator",
                                currentPopId, expectedCriteriaReference, refCode));
            }
        }

        return this;
    }
}
