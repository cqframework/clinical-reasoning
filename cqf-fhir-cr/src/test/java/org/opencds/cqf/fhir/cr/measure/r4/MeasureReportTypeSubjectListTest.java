package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * This test is to verify and confirm that created Subject-List report has required format and output
 *    subject field has reference
 *    has date
 *    has type subject-list
 *    has contained list
 *    has matching number of references in list to population count
 *    resource based is reference to resource
 *    boolean based is reference to patient
 *    stratifier has subject result too
 *    has measure url
 *    has status
 *    has period
 */
public class MeasureReportTypeSubjectListTest {
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(
                    getResourcePath(MeasureReportTypeSubjectListTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
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
    void proportionResourceWithReportTypeParameterPatientGroupSubject() {
        // All subjects
        // Encounter results
        given.when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("subject-list")
                .subject("Group/group-patients-1")
                .evaluate()
                .then()
                .hasSubjectReference("Group/group-patients-1")
                .hasReportType("Subject List")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .subjectResultsValidation()
                .subjectResultsHaveResourceType("Encounter")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(6)
                .hasSubjectResults()
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .hasSubjectResults()
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("numerator")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionBooleanWithReportTypeParameterEmptySubject() {
        // All subjects
        // Boolean results
        given.when()
                .measureId("ProportionBooleanAllPopulations")
                .reportType("subject-list")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportType("Subject List")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanAllPopulations")
                .subjectResultsValidation()
                .subjectResultsHaveResourceType("Patient")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(6)
                .hasSubjectResults()
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("denominator-exception")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("numerator")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    /**
     * This test validates Boolean Stratifier subjectResults
     * All contained lists have matching number of item entries as population count
     * Results are correct resourceType
     */
    @Test
    void ratioBooleanValueStrat() {

        given.when()
                .measureId("RatioBooleanStratValue")
                .reportType("subject-list")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportType("Subject List")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/RatioBooleanStratValue")
                .subjectResultsValidation()
                .subjectResultsHaveResourceType("Patient")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(10)
                .hasSubjectResults()
                .up()
                .population("numerator")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .firstStratifier()
                .stratumCount(2)
                .stratum("M")
                .hasScore("0.2") // make sure stratum are scored
                .population("initial-population")
                .hasStratumPopulationSubjectResults()
                .hasCount(5)
                .up()
                .population("denominator")
                .hasStratumPopulationSubjectResults()
                .hasCount(5)
                .up()
                .population("numerator")
                .hasStratumPopulationSubjectResults()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * This test validates Boolean Stratifier subjectResults
     * All contained lists have matching number of item entries as population count
     * Results are correct resourceType
     */
    @Test
    void ratioResourceValueStrat() {

        given.when()
                .measureId("RatioResourceStratValue")
                .reportType("subject-list")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportType("Subject List")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/RatioResourceStratValue")
                .subjectResultsValidation()
                .subjectResultsHaveResourceType("Encounter")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(11)
                .hasSubjectResults()
                .up()
                .population("numerator")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .firstStratifier()
                .stratumCount(9)
                .stratum("Encounter/patient-1-encounter-1")
                .population("initial-population")
                .hasCount(1)
                .hasStratumPopulationSubjectResults()
                .up()
                .population("denominator")
                .hasCount(1)
                .hasStratumPopulationSubjectResults()
                .up()
                .population("numerator")
                .hasCount(0) // no subject results
                .hasNoStratumPopulationSubjectResults() // validates nothing is there
                .up()
                .hasScore("0.0") // make sure stratum are scored
                .up()
                .up()
                .up()
                .report();
    }
}
