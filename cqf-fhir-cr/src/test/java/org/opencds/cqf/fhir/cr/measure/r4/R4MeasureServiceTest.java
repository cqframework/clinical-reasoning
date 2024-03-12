package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class R4MeasureServiceTest {
    protected static Given given = Measure.given().repositoryFor("EXM108");

    @Test
    public void exm108_individualPractitionerParam() {
        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
    public void exm108_groupPatientsParam() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
    public void exm108_allPatients() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
    public void exm108_noReportTypePopulation() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
    public void exm108_noReportTypeIndividual() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
    public void exm108_noReportTypeSummary() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
    public void exm108_wrongReportTypePassed1() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
    public void exm108_wrongReportTypePassed2() {

        given.when()
                .measureId("measure-EXM108-8.3.000")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
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
