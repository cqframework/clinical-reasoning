package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.selected.report.SelectedMeasureReportPopulation;

/**
 * Verifies the declared supporting-evidence path encodes every CQL System type, including the
 * types that previously failed the whole $evaluate-measure operation (Long, Quantity, Ratio,
 * Concept, non-temporal Interval). Each previously-failing type is declared on its own measure so
 * a single regression cannot mask the others.
 */
class MeasureSupportingEvidenceTypeEncodingTest {

    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    private static SelectedMeasureReportPopulation initialPopulation(String measureId) {
        return given.when()
                .measureId(measureId)
                .subject("patient-9")
                .evaluate()
                .then()
                .report()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION);
    }

    @Test
    void longEncodesAsString() {
        initialPopulation("SupportingEvidenceTypeLong")
                .getPopulationExtension("LongValue")
                .hasStringValue("31");
    }

    @Test
    void quantityEncodesAsQuantity() {
        initialPopulation("SupportingEvidenceTypeQuantity")
                .getPopulationExtension("QuantityValue")
                .hasQuantityValue(31.5, "mg");
    }

    @Test
    void ratioEncodesAsRatio() {
        initialPopulation("SupportingEvidenceTypeRatio")
                .getPopulationExtension("RatioValue")
                .hasRatioValue(1, 2);
    }

    @Test
    void conceptEncodesAsCodeableConcept() {
        initialPopulation("SupportingEvidenceTypeConcept")
                .getPopulationExtension("ConceptValue")
                .hasCodeableConceptValue("http://hl7.org/fhir/v3/AdministrativeGender", "M");
    }

    @Test
    void integerIntervalEncodesAsCqlText() {
        initialPopulation("SupportingEvidenceTypeIntervalInteger")
                .getPopulationExtension("IntervalIntegerValue")
                .hasStringValue("Interval[1, 10]");
    }

    @Test
    void quantityIntervalEncodesAsRange() {
        initialPopulation("SupportingEvidenceTypeIntervalQuantity")
                .getPopulationExtension("IntervalQuantityValue")
                .hasRangeValue(1, 10);
    }

    /**
     * A measure declaring the full type domain evaluates to a COMPLETE report, and the encodings
     * with shipped behaviour keep their shape: Time is valid FHIR (not the CQL literal), Code
     * keeps its string form.
     */
    @Test
    void allTypesDeclaredTogetherProduceACompleteReport() {
        var population = initialPopulation("SupportingEvidenceAllTypes");

        population.getPopulationExtension("TimeValue").hasTimeValue("14:30:00");
        population
                .getPopulationExtension("CodeValue")
                .hasStringValue(
                        "Code { code: M, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Male }");
        population.getPopulationExtension("DateValue").hasStringValue("2026-01-01");
        population.getPopulationExtension("NullValue").hasNullResult();
        population.getPopulationExtension("EmptyListValue").hasEmptyListResult();
        population.getPopulationExtension("SingleResourceValue").hasResourceIdValue("Patient/patient-9");
    }
}
