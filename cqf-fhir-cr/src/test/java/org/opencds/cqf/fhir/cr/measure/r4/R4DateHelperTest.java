package org.opencds.cqf.fhir.cr.measure.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Period;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.helper.IntervalHelper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;

public class R4DateHelperTest {

    private static final ZoneId TIMEZONE_NEWFOUNDLAND = ZoneId.of("America/St_Johns");
    private static final ZoneId TIMEZONE_EASTERN = ZoneId.of("America/Toronto");
    private static final ZoneId TIMEZONE_MOUNTAIN = ZoneId.of("America/Denver");
    private static final String _2024_08_19 = "2024-08-19";
    private static final String _2024_08_20 = "2024-08-20";
    private static final String _2024_02_19 = "2024-02-19";
    private static final String _2024_02_20 = "2024-02-20";
    private static final LocalDateTime LOCAL_DATE_TIME_2024_08_19_START =
            LocalDate.of(2024, Month.AUGUST, 19).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND =
            LocalDate.of(2024, Month.AUGUST, 21).atStartOfDay().minusSeconds(1);
    private static final LocalDateTime LOCAL_DATE_TIME_2024_02_19_START =
            LocalDate.of(2024, Month.FEBRUARY, 19).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND =
            LocalDate.of(2024, Month.FEBRUARY, 21).atStartOfDay().minusSeconds(1);

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

    private static Stream<Arguments> periodParams() {
        return Stream.of(
                Arguments.of(
                        _2024_08_19,
                        _2024_08_20,
                        ZoneOffset.UTC,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_08_19_START,
                                LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                                ZoneOffset.UTC)),
                Arguments.of(
                        _2024_08_19,
                        _2024_08_20,
                        TIMEZONE_NEWFOUNDLAND,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_08_19_START,
                                LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                                TIMEZONE_NEWFOUNDLAND)),
                Arguments.of(
                        _2024_08_19,
                        _2024_08_20,
                        TIMEZONE_EASTERN,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_08_19_START,
                                LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                                TIMEZONE_EASTERN)),
                Arguments.of(
                        _2024_08_19,
                        _2024_08_20,
                        TIMEZONE_MOUNTAIN,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_08_19_START,
                                LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                                TIMEZONE_MOUNTAIN)),
                Arguments.of(
                        _2024_02_19,
                        _2024_02_20,
                        ZoneOffset.UTC,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_02_19_START,
                                LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                                ZoneOffset.UTC)),
                Arguments.of(
                        _2024_02_19,
                        _2024_02_20,
                        TIMEZONE_NEWFOUNDLAND,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_02_19_START,
                                LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                                TIMEZONE_NEWFOUNDLAND)),
                Arguments.of(
                        _2024_02_19,
                        _2024_02_20,
                        TIMEZONE_EASTERN,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_02_19_START,
                                LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                                TIMEZONE_EASTERN)),
                Arguments.of(
                        _2024_02_19,
                        _2024_02_20,
                        TIMEZONE_MOUNTAIN,
                        buildPeriod(
                                LOCAL_DATE_TIME_2024_02_19_START,
                                LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                                TIMEZONE_MOUNTAIN)));
    }

    @ParameterizedTest
    @MethodSource("periodParams")
    void getPeriod(String periodStart, String periodEnd, ZoneId zoneId, Period expectedPeriod) {
        final R4DateHelper helper = new R4DateHelper();

        final Interval interval = IntervalHelper.buildMeasurementPeriod(periodStart, periodEnd, zoneId);
        final Period actualPeriod = helper.buildMeasurementPeriod(interval);

        assertDatesEqualNoMillis(expectedPeriod.getStart(), actualPeriod.getStart());
        assertDatesEqualNoMillis(expectedPeriod.getEnd(), actualPeriod.getEnd());
    }

    private static Period buildPeriod(LocalDateTime localDateStart, LocalDateTime localDateEnd, ZoneId zoneId) {
        return new Period()
                .setStart(toJavaUtilDate(localDateStart, zoneId))
                .setEnd(toJavaUtilDate(localDateEnd, zoneId));
    }

    private static java.util.Date toJavaUtilDate(LocalDateTime localDate, ZoneId zoneId) {
        return java.util.Date.from(localDate.atZone(zoneId).toInstant());
    }

    private static void assertDatesEqualNoMillis(
            @Nullable java.util.Date theExpectedDate, @Nullable java.util.Date theActualDate) {
        assertThat(stripMillisOrNull(theActualDate), equalTo(stripMillisOrNull(theExpectedDate)));
    }

    @Nullable
    private static java.util.Date stripMillisOrNull(@Nullable java.util.Date theDateWithMillis) {
        return Optional.ofNullable(theDateWithMillis)
                .map(nonNullDate -> java.util.Date.from(nonNullDate.toInstant().truncatedTo(ChronoUnit.SECONDS)))
                .orElse(null);
    }
}
