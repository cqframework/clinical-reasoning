package org.opencds.cqf.fhir.cr.measure.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.runtime.DateTime;

class DateHelperTest {

    private static Stream<Arguments> resolveRequestDateWithTimeParams() {
        return Stream.of(
            Arguments.of(
                "2019-01-17T12:30:00",
                LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                ZoneId.systemDefault()),
            Arguments.of(
                "2019-01-01T22:00:00.0-06:00",
                LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                ZoneId.of("America/Chicago")),
            Arguments.of(
                "2017-01-01T00:00:00.000Z",
                LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                ZoneOffset.UTC));
    }

    @ParameterizedTest
    @MethodSource("resolveRequestDateWithTimeParams")
    void resolveRequestDateWithTime(
        String date, LocalDateTime expectedStartTime, LocalDateTime expectedEndTime, ZoneId zoneId) {
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertNotNull(resolvedDateStart);
        final DateTime expectedDateStart = getDateTimeForZoneId(expectedStartTime, zoneId);
        assertDateTimesEqual(expectedDateStart, resolvedDateStart);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertNotNull(resolvedDateEnd);
        assertEquals(resolvedDateStart, resolvedDateEnd);
        final DateTime expectedDateEnd = getDateTimeForZoneId(expectedEndTime, zoneId);
        assertDateTimesEqual(expectedDateEnd, resolvedDateEnd);
    }

    @Test
    void resolveRequestOnlyDate() {
        String date = "2017-07-01";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertNotNull(resolvedDateStart);
        assertDateTimesEqual(
            new DateTime(getOffsetDateTimeForDefaultOffset(LocalDateTime.of(2017, Month.JULY, 1, 0, 0, 0))),
            resolvedDateStart);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertNotNull(resolvedDateEnd);
        assertDateTimesEqual(
            getDateTimeForDefaultOffset(LocalDateTime.of(2017, Month.JULY, 1, 23, 59, 59, 999000000)),
            resolvedDateEnd);
    }

    @Test
    void resolveRequestOnlyYear() {
        String date = "2017";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertNotNull(resolvedDateStart);
        final DateTime expectedDateStart =
            getDateTimeForDefaultOffset(LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0));
        assertDateTimesEqual(expectedDateStart, resolvedDateStart);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertNotNull(resolvedDateEnd);
        final DateTime expectedDateEnd =
            getDateTimeForDefaultOffset(LocalDateTime.of(2017, Month.DECEMBER, 31, 23, 59, 59, 999000000));
        assertDateTimesEqual(expectedDateEnd, resolvedDateEnd);
    }

    private void assertDateTimesEqual(DateTime expectedDateTime, DateTime actualDateTime) {
        assertEquals(
            expectedDateTime.getDateTime().toInstant(),
            actualDateTime.getDateTime().toInstant());
    }

    @Nonnull
    private static DateTime getDateTimeForDefaultOffset(LocalDateTime localDateTime) {
        return new DateTime(getOffsetDateTimeForDefaultOffset(localDateTime));
    }

    @Nonnull
    private static DateTime getDateTimeForZoneId(LocalDateTime localDateTime, ZoneId zoneId) {
        return new DateTime(getOffsetDateTimeForZoneId(localDateTime, zoneId));
    }

    @Nonnull
    private static OffsetDateTime getOffsetDateTimeForDefaultOffset(LocalDateTime localDateTime) {
        return OffsetDateTime.of(localDateTime, getDefaultOffset(localDateTime));
    }

    @Nonnull
    private static OffsetDateTime getOffsetDateTimeForZoneId(LocalDateTime localDateTime, ZoneId zoneId) {
        return OffsetDateTime.of(localDateTime, getOffsetForZoneId(localDateTime, zoneId));
    }

    @Nonnull
    private static ZoneOffset getDefaultOffset(LocalDateTime localDateTime) {
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).getOffset();
    }

    @Nonnull
    private static ZoneOffset getOffsetForZoneId(LocalDateTime localDateTime, ZoneId zoneId) {
        return ZonedDateTime.of(localDateTime, zoneId).getOffset();
    }
}
