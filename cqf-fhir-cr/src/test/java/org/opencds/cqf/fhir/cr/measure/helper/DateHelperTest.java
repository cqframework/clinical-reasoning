package org.opencds.cqf.fhir.cr.measure.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DateHelperTest {
    private static final Logger logger = LoggerFactory.getLogger(DateHelperTest.class);

    private static final boolean IS_START = true;
    private static final boolean IS_END = false;

    // LUKETODO:  consider more test cases
    private static final ZoneId TIMEZONE_EASTERN = ZoneId.of("America/Toronto");
    private static final ZoneId TIMEZONE_MOUNTAIN = ZoneId.of("America/Denver");

    private static final ZoneOffset OFFSET_MINUS_5 = ZoneOffset.ofHours(-5);
    private static final ZoneOffset OFFSET_MINUS_6 = ZoneOffset.ofHours(-6);
    private static final ZoneOffset OFFSET_MINUS_7 = ZoneOffset.ofHours(-7);

    public static Stream<Arguments> dateHelperTestParams() {
        return Stream.of(
            Arguments.of(
                "2019-01-17T12:30:00",
                IS_START,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    OFFSET_MINUS_5
                )
            ),
            Arguments.of(
                "2019-01-01T22:00:00.0-06:00",
                IS_START,
                TIMEZONE_EASTERN,
                    toDateTime(LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    OFFSET_MINUS_6
                )
            ),
            Arguments.of(
                "2017-01-01T00:00:00.000Z",
                IS_START,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    ZoneOffset.UTC
                )
            ),
            Arguments.of(
                "2017-01-01",
                IS_START,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    OFFSET_MINUS_5
                )
            ),
            Arguments.of(
                "2017",
                IS_START,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    OFFSET_MINUS_5
                )
            ),
            Arguments.of(
                "2019-01-17T12:30:00",
                IS_END,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    OFFSET_MINUS_5
                )
            ),
            Arguments.of(
                "2019-01-01T22:00:00.0-06:00",
                IS_END,
                TIMEZONE_EASTERN,
                toDateTime(LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    OFFSET_MINUS_6
                )
            ),
            Arguments.of(
                "2017-01-01T00:00:00.000Z",
                IS_END,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    ZoneOffset.UTC
                )
            ),
            Arguments.of(
                "2017-01-01",
                IS_END,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 23, 59, 59, 999000000),
                    OFFSET_MINUS_5
                )
            ),
            Arguments.of(
                "2017",
                IS_END,
                TIMEZONE_EASTERN,
                toDateTime(
                    LocalDateTime.of(2017, Month.DECEMBER, 31, 23, 59, 59, 999000000),
                    OFFSET_MINUS_5
                )
            ),
            Arguments.of(
                "2019-01-17T12:30:00",
                IS_START,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    OFFSET_MINUS_7
                )
            ),
            Arguments.of(
                "2019-01-01T22:00:00.0-06:00",
                IS_START,
                TIMEZONE_MOUNTAIN,
                toDateTime(LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    OFFSET_MINUS_6
                )
            ),
            Arguments.of(
                "2017-01-01T00:00:00.000Z",
                IS_START,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    ZoneOffset.UTC
                )
            ),
            Arguments.of(
                "2017-01-01",
                IS_START,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    OFFSET_MINUS_7
                )
            ),
            Arguments.of(
                "2017",
                IS_START,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    OFFSET_MINUS_7
                )
            ),
            Arguments.of(
                "2019-01-17T12:30:00",
                IS_END,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    OFFSET_MINUS_7
                )
            ),
            Arguments.of(
                "2019-01-01T22:00:00.0-06:00",
                IS_END,
                TIMEZONE_MOUNTAIN,
                toDateTime(LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    OFFSET_MINUS_6
                )
            ),
            Arguments.of(
                "2017-01-01T00:00:00.000Z",
                IS_END,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    ZoneOffset.UTC
                )
            ),
            Arguments.of(
                "2017-01-01",
                IS_END,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2017, Month.JANUARY, 1, 23, 59, 59, 999000000),
                    OFFSET_MINUS_7
                )
            ),
            Arguments.of(
                "2017",
                IS_END,
                TIMEZONE_MOUNTAIN,
                toDateTime(
                    LocalDateTime.of(2017, Month.DECEMBER, 31, 23, 59, 59, 999000000),
                    OFFSET_MINUS_7
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("dateHelperTestParams")
    void testIntervals(String inputDate, boolean isStart, ZoneId clientTimezone, DateTime expectedResult) {
        final DateTime actualDateTime = DateHelper.resolveRequestDate(inputDate, isStart, clientTimezone);
        assertNotNull(actualDateTime);

        assertEquals(expectedResult.getZoneOffset(), actualDateTime.getZoneOffset());
        assertEquals(expectedResult, actualDateTime);
    }

    private static DateTime toDateTime(LocalDateTime localDateTime, ZoneOffset zoneOffset) {
        return new DateTime(localDateTime.atOffset(zoneOffset));
    }
}
