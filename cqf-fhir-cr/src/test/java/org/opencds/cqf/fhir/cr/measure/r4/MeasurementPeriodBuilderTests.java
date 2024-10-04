package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class MeasurementPeriodBuilderTests {
    private static final Given GIVEN_REPO = Measure.given().repositoryFor("MinimalMeasureEvaluation");

    @Test
    void uberSimple_UsesMeasurementPeriodToIncludeResource() {
        // Targeted Encounter "period": {
        //    "start": "2020-01-16T20:00:00Z",
        //    "end": "2020-01-16T21:00:00Z"
        //  }
        var when = GIVEN_REPO
                .when()
                .measureId("UberSimple")
                .periodStart(ZonedDateTime.of(LocalDateTime.of(2020, Month.JANUARY, 16, 20, 0, 0), ZoneOffset.UTC))
                .periodEnd(ZonedDateTime.of(LocalDateTime.of(2020, Month.JANUARY, 16, 21, 0, 0), ZoneOffset.UTC))
                .reportType("subject")
                .subject("Patient/female-1914")
                .evaluate()
                .then();

        when.hasReportType("Individual")
                .hasPeriodStart(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 20, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasPeriodEnd(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 21, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasSubjectReference("Patient/female-1914")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(1);
    }

    @Test
    void uberSimple_UsesMeasurementPeriodToExcludeResource() {
        // Targeted Encounter "period": {
        //    "start": "2020-01-16T20:00:00Z",
        //    "end": "2020-01-16T21:00:00Z"
        //  }
        // test is one hour after resource period
        var when = GIVEN_REPO
                .when()
                .measureId("UberSimple")
                .periodStart(ZonedDateTime.of(LocalDateTime.of(2020, Month.JANUARY, 16, 22, 0, 0), ZoneOffset.UTC))
                .periodEnd(ZonedDateTime.of(LocalDateTime.of(2020, Month.JANUARY, 16, 23, 0, 0), ZoneOffset.UTC))
                .reportType("subject")
                .subject("Patient/female-1914")
                .evaluate()
                .then();

        when.hasReportType("Individual")
                .hasPeriodStart(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 22, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasPeriodEnd(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 23, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasSubjectReference("Patient/female-1914")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(0);
    }

    @Test
    void uberSimple_UsesMeasurementPeriodToExcludeResource2() {
        // Targeted Encounter "period": {
        //    "start": "2020-01-16T20:00:00Z",
        //    "end": "2020-01-16T21:00:00Z"
        //  }
        // test is one hour before resource period
        var when = GIVEN_REPO
                .when()
                .measureId("UberSimple")
                .periodStart(ZonedDateTime.of(LocalDateTime.of(2020, Month.JANUARY, 16, 19, 0, 0), ZoneOffset.UTC))
                .periodEnd(ZonedDateTime.of(LocalDateTime.of(2020, Month.JANUARY, 16, 20, 0, 0), ZoneOffset.UTC))
                .reportType("subject")
                .subject("Patient/female-1914")
                .evaluate()
                .then();

        when.hasReportType("Individual")
                .hasPeriodStart(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 19, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasPeriodEnd(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 20, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasSubjectReference("Patient/female-1914")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(0);
    }

    @Test
    void uberSimple_DefaultMeasurementPeriodToIncludeResource() {
        // Targeted Encounter "period": {
        //    "start": "2020-01-16T20:00:00Z",
        //    "end": "2020-01-16T21:00:00Z"
        //  }
        // default period should default to UTC of parameter default
        // parameter "Measurement Period" Interval<DateTime> default Interval[@2020-01-16T20:00:00,
        // @2020-01-16T21:00:00)
        var when = GIVEN_REPO
                .when()
                .measureId("UberSimple")
                // No explicit period start and end
                .reportType("subject")
                .subject("Patient/female-1914")
                .evaluate()
                .then();

        when.hasReportType("Individual")
                // These assertions reflect the new passing CQL Interval with one extra minute at each end (minus and
                // plus, respectively):
                .hasPeriodStart(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 20, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasPeriodEnd(Date.from(
                        LocalDateTime.of(2020, Month.JANUARY, 16, 21, 0, 0).toInstant(ZoneOffset.UTC)))
                .hasSubjectReference("Patient/female-1914")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(1);
    }
}
