package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class SimpleMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("EXM108");

    @Test
    void exm108_partialSubjectId() {
        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
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
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
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
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
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
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
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
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
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
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
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
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
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
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer-EXM108")
                .reportType("subject")
                .evaluate()
                .then()
                .report();

        assertEquals(MeasureReportType.INDIVIDUAL, report.getType());
        assertEquals("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108|8.3.000", report.getMeasure());
        assertEquals("Patient/numer-EXM108", report.getSubject().getReference());

        // TODO: The MeasureProcessor assumes local timezone if none is specified.
        // Need to make the test smart enough to handle that.
        assertEquals(report.getPeriod().getStartElement().getYear(), (Integer) 2018);
        // assertEquals(report.getPeriod().getStartElement().getMonth(), (Integer)12);
        assertEquals(report.getPeriod().getStartElement().getDay(), (Integer) 31);

        assertEquals(report.getPeriod().getEndElement().getYear(), (Integer) 2019);
        // assertEquals(report.getPeriod().getEndElement().getMonth(), (Integer)12);
        assertEquals(report.getPeriod().getEndElement().getDay(), (Integer) 31);

        // TODO: Should be the evaluation date. Or approximately "now"
        assertNotNull(report.getDate());
    }
}
