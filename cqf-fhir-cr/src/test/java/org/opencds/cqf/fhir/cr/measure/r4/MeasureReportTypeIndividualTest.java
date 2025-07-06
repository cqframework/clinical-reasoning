package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

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
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final IRepository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Path.of(getResourcePath(MeasureReportTypeIndividualTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }

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
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
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
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(2)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .report();
    }
}
