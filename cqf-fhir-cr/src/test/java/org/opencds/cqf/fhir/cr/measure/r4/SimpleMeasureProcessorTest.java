package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

@SuppressWarnings("java:S2699")
class SimpleMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("EXM108");

    @Nested
    class BasisEncounter {

        private static final String MEASURE_ID = "measure-EXM108-8.3.000-basis-Encounter";

        @Test
        void exm108_partialSubjectId() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("numer-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .population("numerator")
                    .hasCount(1)
                    .up()
                    .population("denominator")
                    .hasCount(1);

            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("denom-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .population("numerator")
                    .hasCount(0)
                    .up()
                    .population("denominator")
                    .hasCount(1);
        }

        @Test
        void exm108_fullSubjectId() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/numer-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .hasScore("1.0")
                    .population("numerator")
                    .hasCount(1)
                    .up()
                    .population("denominator")
                    .hasCount(1);

            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/denom-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .hasScore("0.0")
                    .population("numerator")
                    .hasCount(0)
                    .up()
                    .population("denominator")
                    .hasCount(1);
        }

        @Test
        void exm108_population() {
            // This bundle has two patients, a numerator and denominator
            var report = given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .reportType("population")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .hasScore("0.5")
                    .population("numerator")
                    .hasCount(1)
                    .up()
                    .population("denominator")
                    .hasCount(2)
                    .up()
                    .population("initial-population")
                    .hasCount(2)
                    .up()
                    .up()
                    .report();

            assertEquals(MeasureReportType.SUMMARY, report.getType());
        }

        @Test
        void exm108_noReportType_noSubject_runsPopulation() {
            // This default behavior if no type or subject is specified is "population"
            var report = given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .hasScore("0.5")
                    .population("numerator")
                    .hasCount(1)
                    .up()
                    .population("denominator")
                    .hasCount(2)
                    .up()
                    .population("initial-population")
                    .hasCount(2)
                    .up()
                    .up()
                    .report();

            assertEquals(MeasureReportType.SUMMARY, report.getType());
        }

        @Test
        void exm108_noType_hasSubject_runsIndividual() {
            // This default behavior if no type is specified is "individual"
            var report = given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/numer-EXM108")
                    .evaluate()
                    .then()
                    .firstGroup()
                    .hasScore("1.0")
                    .population("numerator")
                    .hasCount(1)
                    .up()
                    .population("denominator")
                    .hasCount(1)
                    .up()
                    .up()
                    .report();

            assertEquals(MeasureReportType.INDIVIDUAL, report.getType());
        }

        @Test
        void exm108_singlePatient_hasMetadata() {
            var report = given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/numer-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .report();

            assertEquals(MeasureReportType.INDIVIDUAL, report.getType());
            assertEquals(
                    "http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108-basis-Encounter|8.3.000", report.getMeasure());
            assertEquals("Patient/numer-EXM108", report.getSubject().getReference());

            assertEquals(report.getPeriod().getStartElement().getYear(), (Integer) 2018);
            assertEquals(report.getPeriod().getStartElement().getDay(), (Integer) 31);

            assertEquals(report.getPeriod().getEndElement().getYear(), (Integer) 2019);
            assertEquals(report.getPeriod().getEndElement().getDay(), (Integer) 31);

            assertNotNull(report.getDate());
            assertEquals(
                    LocalDateTime.now().withSecond(0).withNano(0),
                    report.getDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .withSecond(0)
                            .withNano(0));
        }
    }

    @Nested
    class BasisBoolean {

        private static final String MEASURE_ID = "measure-EXM108-8.3.000-basis-boolean";

        @Test
        void exm108_partialSubjectId_1() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("numer-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg("Patient/numer-EXM108")
                    .report();
        }

        @Test
        void exm108_partialSubjectId_2() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("denom-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg("Patient/denom-EXM108")
                    .report();
        }

        @Test
        void exm108_fullSubjectId_1() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/numer-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg("Patient/numer-EXM108")
                    .report();
        }

        @Test
        void exm108_fullSubjectId_2() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/denom-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg("Patient/denom-EXM108")
                    .report();
        }

        @Test
        void exm108_population() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .reportType("population")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "group expression criteria results for expression: [Initial Population] and scoring: [PROPORTION] must fall within accepted types for population basis: [boolean] for Measure: [http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108-basis-boolean] due to mismatch between total result classes: [Encounter] and matching result classes: []")
                    .report();
        }

        @Test
        void exm108_noReportType_noSubject_runsPopulation() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg(
                            "Message: group expression criteria results for expression: [Initial Population] and scoring: [PROPORTION] must fall within accepted types for population basis: [boolean] for Measure: [http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108-basis-boolean] due to mismatch between total result classes: [Encounter] and matching result classes: []")
                    .report();
        }

        @Test
        void exm108_noType_hasSubject_runsIndividual() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/numer-EXM108")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg("Patient/numer-EXM108")
                    .report();
        }

        @Test
        void exm108_singlePatient_hasMetadata() {
            given.when()
                    .measureId(MEASURE_ID)
                    .periodStart("2018-12-31")
                    .periodEnd("2019-12-31")
                    .subject("Patient/numer-EXM108")
                    .reportType("subject")
                    .evaluate()
                    .then()
                    .hasStatus(MeasureReportStatus.ERROR)
                    .hasContainedOperationOutcome()
                    .hasContainedOperationOutcomeMsg("Patient/numer-EXM108")
                    .report();
        }
    }
}
