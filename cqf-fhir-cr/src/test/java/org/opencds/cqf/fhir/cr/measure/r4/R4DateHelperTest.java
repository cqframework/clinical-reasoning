package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Precision;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;

public class R4DateHelperTest {

    @Test
    public void checkDate() {
        var date = new Interval(new Date("2019-01-01"), true, new Date("2019-12-31"), true);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var now = OffsetDateTime.now();
        // The CQL engine sets times with an unspecified offset to the _current_ system offset,
        // using the rules for the system default timezone. A given timezone may have variable
        // offsets from UTC (e.g. daylight savings time), and the current offset may be different
        // than the expected offset for a given date.
        var helper = new R4DateHelper();
        var period = helper.buildMeasurementPeriod(date);

        assertEquals(
                "2019-01-01", formatter.format(period.getStart().toInstant().atOffset(now.getOffset())));
        assertEquals("2019-12-31", formatter.format(period.getEnd().toInstant().atOffset(now.getOffset())));
    }

    @Test
    public void checkDateTime() {
        ZoneOffset offset = ZonedDateTime.now().getOffset();

        DateTime start = new DateTime("2019-01-01", offset);
        DateTime end = new DateTime("2019-12-31", offset);
        var date = new Interval(start, true, end, true);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // The CQL engine sets times with an unspecified offset to the _current_ system offset,
        // using the rules for the system default timezone. A given timezone may have variable
        // offsets from UTC (e.g. daylight savings time), and the current offset may be different
        // than the expected offset for a given date.
        var helper = new R4DateHelper();
        var period = helper.buildMeasurementPeriod(date);

        assertEquals(
                "2019-01-01", formatter.format(period.getStart().toInstant().atOffset(offset)));
        assertEquals("2019-12-31", formatter.format(period.getEnd().toInstant().atOffset(offset)));
    }

    @Test
    public void checkNull() {
        var helper = new R4DateHelper();
        try {
            helper.buildMeasurementPeriod(new Interval(new java.util.Date(), true, new java.util.Date(), true));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Measurement period should be an interval of CQL DateTime or Date"));
        }
    }

    public static Stream<Arguments> zonedDateTimesParams() {
        return Stream.of(
            Arguments.of(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()), Precision.DAY),
            Arguments.of(LocalDateTime.of(2020, Month.JANUARY, 1, 12, 0, 0).atZone(ZoneId.systemDefault()), Precision.HOUR),
            Arguments.of(LocalDateTime.of(2020, Month.JANUARY, 1, 12, 23, 0).atZone(ZoneId.systemDefault()), Precision.MINUTE),
            Arguments.of(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 23, 0).atZone(ZoneId.systemDefault()), Precision.MINUTE),
            Arguments.of(LocalDateTime.of(2020, Month.JANUARY, 1, 12, 23, 47).atZone(ZoneId.systemDefault()), Precision.SECOND),
            Arguments.of(LocalDateTime.of(2020, Month.JANUARY, 1, 12, 0, 47).atZone(ZoneId.systemDefault()), Precision.SECOND),
            Arguments.of(LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0, 47).atZone(ZoneId.systemDefault()), Precision.SECOND)
        );
    }

    @ParameterizedTest
    @MethodSource("zonedDateTimesParams")
    public void zonedDateTimes(ZonedDateTime theZonedDateTime, Precision theExpectedPrecision) {
        final Interval interval = new R4DateHelper()
            .buildMeasurementPeriodInterval(theZonedDateTime, theZonedDateTime);
        final Object start = interval.getStart();
        final Object end = interval.getEnd();
        assertInstanceOf(DateTime.class, start);
        assertInstanceOf(DateTime.class, end);
        assertEquals(theExpectedPrecision, ((DateTime)start).getPrecision());
        assertEquals(theExpectedPrecision, ((DateTime)end).getPrecision());
    }
}
