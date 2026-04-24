package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

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
@SuppressWarnings("squid:S2699")
class MeasureReportTypeSubjectListTest {
    private static final Given given = Measure.given().repositoryFor("MeasureTest");

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
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(11)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(11)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(3) // because subject was also in Numerator
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
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
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
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
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .firstStratifier()
                .hasStratumCount(2)
                .stratum("M")
                .hasScore("0.2") // make sure stratum are scored
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasStratumPopulationSubjectResults()
                .hasCount(5)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasStratumPopulationSubjectResults()
                .hasCount(5)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
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
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(11)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(11)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .up()
                .report();
    }
}
