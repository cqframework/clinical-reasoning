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
import org.opencds.cqf.fhir.utility.repository.ig.IgRepositoryForTests;

/**
 * This test is to verify and confirm that created Summary report has required format and output
 *     subject field has reference
 *     report has date
 *     report type summary
 *     has measure url
 *     has correct status
 *     has period
 */
@SuppressWarnings("squid:S2699")
class MeasureReportTypeSummaryTest {
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final IRepository repository = new IgRepositoryForTests(
            FhirContext.forR4Cached(),
            Path.of(getResourcePath(MeasureReportTypeSummaryTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient("practitioner-1", "organization-1", period);
    }

    @Test
    void proportionResourceWithReportTypeParameterEmptySubject() {
        // All subjects
        given.when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithReportTypeParameterPatientGroup() {
        // Patients in Group
        given.when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .subject("Group/group-patients-1")
                .evaluate()
                .then()
                .hasSubjectReference("Group/group-patients-1")
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithReportTypeParameterPractitionerGroup() {
        // Patients with generalPractitioner.reference matching member of group
        given.when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .subject("Group/group-practitioners-1")
                .evaluate()
                .then()
                .hasSubjectReference("Group/group-practitioners-1")
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithReportTypeParameterPractitioner() {
        // Patients with generalPractitioner.reference matching member of group
        given.when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .subject("Practitioner/practitioner-1")
                .evaluate()
                .then()
                .hasSubjectReference("Practitioner/practitioner-1")
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithNoReportType() {
        // this should default to 'Summary' for empty subject
        given.when()
                .measureId("ProportionResourceAllPopulations")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }
}
