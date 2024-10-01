package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

class R4MeasureServiceTest {
    protected static Given given = Measure.given().repositoryFor("EXM108");

    @Test
    void exm108_individualPractitionerParam() {
        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .practitioner("Practitioner/exm108-practitioner-2")
                .reportType("population")
                .evaluate()
                .then()
                .hasSubjectReference("Practitioner/exm108-practitioner-2")
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
                .practitioner("Practitioner/exm108-practitioner-1")
                .reportType("population")
                .evaluate()
                .then()
                .hasSubjectReference("Practitioner/exm108-practitioner-1")
                .firstGroup()
                .population("numerator")
                .hasCount(0)
                .up()
                .population("denominator")
                .hasCount(1);
    }

    @Test
    void exm108_groupPatientsParam() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .subject("Group/patient-group-108")
                .reportType("population")
                .evaluate()
                .then()
                .hasSubjectReference("Group/patient-group-108")
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(2);
    }

    @Test
    void exm108_allPatients() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .practitioner(null)
                .reportType("population")
                .evaluate()
                .then()
                .hasSubjectReference(null)
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(2);
    }

    @Test
    void exm108_noReportTypePopulation() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(2);
    }

    @Test
    void exm108_noReportTypeIndividual() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer-EXM108")
                .evaluate()
                .then()
                .hasSubjectReference("Patient/numer-EXM108")
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1);
    }

    @Test
    void exm108_noReportTypeSummary() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(2);
    }

    @Test
    void exm108_wrongReportTypePassed1() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .reportType("individual")
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(2);
    }

    @Test
    void exm108_wrongReportTypePassed2() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart(LocalDate.of(2018, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .reportType("summary")
                .subject("Patient/numer-EXM108")
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1);
    }
}
