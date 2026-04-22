package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * This test is to verify and confirm that created Individual report has required format and output
 *      subject field has reference
 *      report has date
 *      report type summary
 *      has measure url
 *      has correct status
 *      has period
 */
@SuppressWarnings("squid:S2699")
class MeasureReportTypeIndividualTest {
    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    @Test
    void proportionResourceWithReportTypeParameter() {

        given.when()
                .measureId("ProportionResourceAllPopulations")
                .subject("Patient/patient-9")
                .reportType("subject")
                .evaluate()
                .then()
                .hasSubjectReference("Patient/patient-9")
                .hasReportType("Individual")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .hasEvaluatedResourceCount(2)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithNoReportType() {
        // this should default to 'Individual' based on subject parameter
        given.when()
                .measureId("ProportionResourceAllPopulations")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .hasSubjectReference("Patient/patient-9")
                .hasReportType("Individual")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .hasEvaluatedResourceCount(2)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .report();
    }
}
