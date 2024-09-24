package org.opencds.cqf.fhir.cr.measure.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.runtime.DateTime;

class IntervalHelperTest {
    private static final ZoneId TIMEZONE_EASTERN = ZoneId.of("America/Toronto");
    private static final ZoneId TIMEZONE_MOUNTAIN = ZoneId.of("America/Denver");

    private static final ZoneOffset OFFSET_MINUS_4 = ZoneOffset.ofHours(-4);
    private static final ZoneOffset OFFSET_MINUS_5 = ZoneOffset.ofHours(-5);
    private static final ZoneOffset OFFSET_MINUS_6 = ZoneOffset.ofHours(-6);
    private static final ZoneOffset OFFSET_MINUS_7 = ZoneOffset.ofHours(-7);

    public static Stream<Arguments> dateHelperTestParams() {
        return Stream.of(
                Arguments.of(
                        "2019-01-17T12:30:00",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0), OFFSET_MINUS_5)),
                Arguments.of(
                        "2019-01-01T22:00:00.0-06:00",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0), OFFSET_MINUS_6)),
                Arguments.of(
                        "2017-01-01T00:00:00.000Z",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0), ZoneOffset.UTC)),
                Arguments.of(
                        "2017-01-01",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(
                                LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                                OFFSET_MINUS_5,
                                LocalDateTime.of(2017, Month.JANUARY, 1, 23, 59, 59, 999000000),
                                OFFSET_MINUS_5)),
                Arguments.of(
                        "2019-07-17T12:30:00",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JULY, 17, 12, 30, 0), OFFSET_MINUS_4)),
                Arguments.of(
                        "2019-07-01T22:00:00.0-06:00",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JULY, 1, 22, 0, 0), OFFSET_MINUS_6)),
                Arguments.of(
                        "2017-07-01T00:00:00.000Z",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(LocalDateTime.of(2017, Month.JULY, 1, 0, 0, 0), ZoneOffset.UTC)),
                Arguments.of(
                        "2017-07-01",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(
                                LocalDateTime.of(2017, Month.JULY, 1, 0, 0, 0),
                                OFFSET_MINUS_4,
                                LocalDateTime.of(2017, Month.JULY, 1, 23, 59, 59, 999000000),
                                OFFSET_MINUS_4)),
                Arguments.of(
                        "2017",
                        TIMEZONE_EASTERN,
                        toDateTimeRange(
                                LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                                OFFSET_MINUS_5,
                                LocalDateTime.of(2017, Month.DECEMBER, 31, 23, 59, 59, 999000000),
                                OFFSET_MINUS_5)),
                Arguments.of(
                        "2019-01-17T12:30:00",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0), OFFSET_MINUS_7)),
                Arguments.of(
                        "2019-01-01T22:00:00.0-06:00",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0), OFFSET_MINUS_6)),
                Arguments.of(
                        "2017-01-01T00:00:00.000Z",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0), ZoneOffset.UTC)),
                Arguments.of(
                        "2017-01-01",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(
                                LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                                OFFSET_MINUS_7,
                                LocalDateTime.of(2017, Month.JANUARY, 1, 23, 59, 59, 999000000),
                                OFFSET_MINUS_7)),
                Arguments.of(
                        "2019-07-17T12:30:00",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JULY, 17, 12, 30, 0), OFFSET_MINUS_6)),
                Arguments.of(
                        "2019-07-01T22:00:00.0-06:00",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(LocalDateTime.of(2019, Month.JULY, 1, 22, 0, 0), OFFSET_MINUS_6)),
                Arguments.of(
                        "2017-07-01T00:00:00.000Z",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(LocalDateTime.of(2017, Month.JULY, 1, 0, 0, 0), ZoneOffset.UTC)),
                Arguments.of(
                        "2017-07-01",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(
                                LocalDateTime.of(2017, Month.JULY, 1, 0, 0, 0),
                                OFFSET_MINUS_6,
                                LocalDateTime.of(2017, Month.JULY, 1, 23, 59, 59, 999000000),
                                OFFSET_MINUS_6)),
                Arguments.of(
                        "2017",
                        TIMEZONE_MOUNTAIN,
                        toDateTimeRange(
                                LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                                OFFSET_MINUS_7,
                                LocalDateTime.of(2017, Month.DECEMBER, 31, 23, 59, 59, 999000000),
                                OFFSET_MINUS_7)));
    }

    @ParameterizedTest
    @MethodSource("dateHelperTestParams")
    void testIntervals(String inputDate, ZoneId clientTimezone, DateTimeRange expectedResult) {
        final DateTime resolvedDateStart = DateHelper.resolveRequestDate(inputDate, true, clientTimezone);
        assertNotNull(resolvedDateStart);

        final DateTime resolvedDateEnd = DateHelper.resolveRequestDate(inputDate, false, clientTimezone);
        assertNotNull(resolvedDateEnd);

        if (expectedResult.areStartAndEndEqual()) {
            assertEquals(resolvedDateStart, resolvedDateEnd);
        } else {
            assertNotEquals(resolvedDateStart, resolvedDateEnd);
        }

        final DateTime expectedStart = expectedResult.getStart();
        final DateTime expectedEnd = expectedResult.getEnd();

        assertEquals(expectedStart.getZoneOffset(), resolvedDateStart.getZoneOffset());
        assertEquals(expectedEnd.getZoneOffset(), resolvedDateEnd.getZoneOffset());

        assertEquals(expectedStart, resolvedDateStart);
        assertEquals(expectedEnd, resolvedDateEnd);
    }

    private static DateTimeRange toDateTimeRange(LocalDateTime localDateTime, ZoneOffset zoneOffset) {
        return new DateTimeRange(toDateTime(localDateTime, zoneOffset), toDateTime(localDateTime, zoneOffset));
    }

    private static DateTimeRange toDateTimeRange(
            LocalDateTime startLocalDateTime,
            ZoneOffset startZoneOffset,
            LocalDateTime endLocalDateTime,
            ZoneOffset endZoneOffset) {
        return new DateTimeRange(
                toDateTime(startLocalDateTime, startZoneOffset), toDateTime(endLocalDateTime, endZoneOffset));
    }

    private static DateTime toDateTime(LocalDateTime localDateTime, ZoneOffset zoneOffset) {
        return new DateTime(localDateTime.atOffset(zoneOffset));
    }

    private static class DateTimeRange {
        private final DateTime start;
        private final DateTime end;

        public DateTimeRange(DateTime theStart, DateTime theEnd) {
            start = theStart;
            end = theEnd;
        }

        public DateTime getStart() {
            return start;
        }

        public DateTime getEnd() {
            return end;
        }

        public boolean areStartAndEndEqual() {
            return start.equals(end);
        }
    }
}
