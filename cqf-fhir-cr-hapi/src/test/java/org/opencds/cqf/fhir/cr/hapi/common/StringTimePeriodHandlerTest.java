package org.opencds.cqf.fhir.cr.hapi.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StringTimePeriodHandlerTest {

    record GetStartZonedDateTimeHappyPathParams(
            @Nullable String timezone,
            @Nullable String theInputPeriodStart,
            @Nullable ZonedDateTime expectedResult) {}

    record GetEndZonedDateTimeHappyPathParams(
            @Nullable String timezone,
            @Nullable String theInputPeriodEnd,
            @Nullable ZonedDateTime expectedResult) {}

    record ErrorParams(
            @Nullable String timezone, @Nullable String theInputPeriod, InvalidRequestException expectedResult) {}

    record SerializeDeserializeRoundTripParams(ZonedDateTime theInputDateTime, String expectedResult) {}

    record DeSerializeRoundTripParams(String theInputString, ZonedDateTime expectedResult) {}

    private static final String ZONE_ID_Z = "Z";
    private static final String TIMEZONE_UTC = ZoneOffset.UTC.getId();

    private static final ZoneId TIMEZONE_AMERICA_ST_JOHNS = ZoneId.of("America/St_Johns");
    private static final ZoneId TIMEZONE_AMERICA_TORONTO = ZoneId.of("America/Toronto");
    private static final ZoneId TIMEZONE_AMERICA_DENVER = ZoneId.of("America/Denver");

    private static final String TIMEZONE_AMERICA_ST_JOHNS_ID = TIMEZONE_AMERICA_ST_JOHNS.getId();
    private static final String TIMEZONE_AMERICA_TORONTO_ID = TIMEZONE_AMERICA_TORONTO.getId();
    private static final String TIMEZONE_AMERICA_DENVER_ID = TIMEZONE_AMERICA_DENVER.getId();

    private static final LocalDate LOCAL_DATE_2020_01_01 = LocalDate.of(2020, Month.JANUARY, 1);
    private static final LocalDate LOCAL_DATE_2021_12_31 = LocalDate.of(2021, Month.DECEMBER, 31);

    private static final LocalDate LOCAL_DATE_2022_02_01 = LocalDate.of(2022, Month.FEBRUARY, 1);
    private static final LocalDate LOCAL_DATE_2022_02_28 = LocalDate.of(2022, Month.FEBRUARY, 28);
    private static final LocalDate LOCAL_DATE_2022_08_31 = LocalDate.of(2022, Month.AUGUST, 31);

    private static final LocalDate LOCAL_DATE_2024_01_01 = LocalDate.of(2024, Month.JANUARY, 1);
    private static final LocalDate LOCAL_DATE_2024_01_02 = LocalDate.of(2024, Month.JANUARY, 2);
    private static final LocalDate LOCAL_DATE_2024_02_25 = LocalDate.of(2024, Month.FEBRUARY, 25);
    private static final LocalDate LOCAL_DATE_2024_02_26 = LocalDate.of(2024, Month.FEBRUARY, 26);
    private static final LocalDate LOCAL_DATE_2024_02_29 = LocalDate.of(2024, Month.FEBRUARY, 29);
    private static final LocalDate LOCAL_DATE_2024_09_25 = LocalDate.of(2024, Month.SEPTEMBER, 25);
    private static final LocalDate LOCAL_DATE_2024_09_26 = LocalDate.of(2024, Month.SEPTEMBER, 26);

    private static final LocalTime LOCAL_TIME_00_00_00 = LocalTime.of(0, 0, 0);
    private static final LocalTime LOCAL_TIME_12_00_00 = LocalTime.of(12, 0, 0);
    private static final LocalTime LOCAL_TIME_23_59_59 = LocalTime.of(23, 59, 59);

    private static final LocalDateTime _2020_01_01_00_00_00 = LOCAL_DATE_2020_01_01.atTime(LOCAL_TIME_00_00_00);

    private static final LocalDateTime _2021_12_31_23_59_59 = LOCAL_DATE_2021_12_31.atTime(LOCAL_TIME_23_59_59);

    private static final LocalDateTime _2022_02_01_00_00_00 = LOCAL_DATE_2022_02_01.atTime(LOCAL_TIME_00_00_00);
    private static final LocalDateTime _2022_02_28_23_59_59 = LOCAL_DATE_2022_02_28.atTime(LOCAL_TIME_23_59_59);
    private static final LocalDateTime _2022_08_31_23_59_59 = LOCAL_DATE_2022_08_31.atTime(LOCAL_TIME_23_59_59);

    private static final LocalDateTime _2024_01_01_12_00_00 = LOCAL_DATE_2024_01_01.atTime(LOCAL_TIME_12_00_00);
    private static final LocalDateTime _2024_01_02_12_00_00 = LOCAL_DATE_2024_01_02.atTime(LOCAL_TIME_12_00_00);

    private static final LocalDateTime _2024_02_25_00_00_00 = LOCAL_DATE_2024_02_25.atTime(LOCAL_TIME_00_00_00);
    private static final LocalDateTime _2024_02_26_23_59_59 = LOCAL_DATE_2024_02_26.atTime(LOCAL_TIME_23_59_59);

    private static final LocalDateTime _2024_02_29_23_59_59 = LOCAL_DATE_2024_02_29.atTime(LOCAL_TIME_23_59_59);
    private static final LocalDateTime _2024_09_25_00_00_00 = LOCAL_DATE_2024_09_25.atTime(LOCAL_TIME_00_00_00);
    private static final LocalDateTime _2024_09_25_12_00_00 = LOCAL_DATE_2024_09_25.atTime(LOCAL_TIME_12_00_00);
    private static final LocalDateTime _2024_09_26_12_00_00 = LOCAL_DATE_2024_09_26.atTime(LOCAL_TIME_12_00_00);
    private static final LocalDateTime _2024_09_26_23_59_59 = LOCAL_DATE_2024_09_26.atTime(LOCAL_TIME_23_59_59);

    private final StringTimePeriodHandler myTestSubject = new StringTimePeriodHandler(ZoneOffset.UTC);

    private static Stream<GetStartZonedDateTimeHappyPathParams> getStartZonedDateTime_happyPath_params() {
        return Stream.of(
                new GetStartZonedDateTimeHappyPathParams(null, null, null),
                new GetStartZonedDateTimeHappyPathParams(ZONE_ID_Z, null, null),
                new GetStartZonedDateTimeHappyPathParams(TIMEZONE_UTC, null, null),
                new GetStartZonedDateTimeHappyPathParams(TIMEZONE_AMERICA_ST_JOHNS_ID, null, null),
                new GetStartZonedDateTimeHappyPathParams(TIMEZONE_AMERICA_TORONTO_ID, null, null),
                new GetStartZonedDateTimeHappyPathParams(TIMEZONE_AMERICA_DENVER_ID, null, null),
                new GetStartZonedDateTimeHappyPathParams(null, "2020", _2020_01_01_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2020", _2020_01_01_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2020", _2020_01_01_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID, "2020", _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID, "2020", _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2020", _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetStartZonedDateTimeHappyPathParams(null, "2022-02", _2022_02_01_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2022-02", _2022_02_01_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2022-02", _2022_02_01_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2022-02",
                        _2022_02_01_00_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID, "2022-02", _2022_02_01_00_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2022-02", _2022_02_01_00_00_00.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetStartZonedDateTimeHappyPathParams(
                        null, "2024-02-25", _2024_02_25_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-02-25", _2024_02_25_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-02-25", _2024_02_25_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-02-25",
                        _2024_02_25_00_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-02-25",
                        _2024_02_25_00_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2024-02-25", _2024_02_25_00_00_00.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetStartZonedDateTimeHappyPathParams(
                        null, "2024-09-25", _2024_09_25_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-09-25", _2024_09_25_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-09-25", _2024_09_25_00_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-09-25",
                        _2024_09_25_00_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-09-25",
                        _2024_09_25_00_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2024-09-25", _2024_09_25_00_00_00.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetStartZonedDateTimeHappyPathParams(
                        null, "2024-01-01T12:00:00", _2024_01_01_12_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-01-01T12:00:00", _2024_01_01_12_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-01-01T12:00:00", _2024_01_01_12_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-01-01T12:00:00",
                        _2024_01_01_12_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-01-01T12:00:00",
                        _2024_01_01_12_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID,
                        "2024-01-01T12:00:00",
                        _2024_01_01_12_00_00.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetStartZonedDateTimeHappyPathParams(
                        null, "2024-09-25T12:00:00", _2024_09_25_12_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-09-25T12:00:00", _2024_09_25_12_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-09-25T12:00:00", _2024_09_25_12_00_00.atZone(ZoneOffset.UTC)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-09-25T12:00:00",
                        _2024_09_25_12_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-09-25T12:00:00",
                        _2024_09_25_12_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetStartZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID,
                        "2024-09-25T12:00:00",
                        _2024_09_25_12_00_00.atZone(TIMEZONE_AMERICA_DENVER)));
    }

    private static Stream<GetEndZonedDateTimeHappyPathParams> getEndZonedDateTime_happyPath_params() {
        return Stream.of(
                new GetEndZonedDateTimeHappyPathParams(null, null, null),
                new GetEndZonedDateTimeHappyPathParams(ZONE_ID_Z, null, null),
                new GetEndZonedDateTimeHappyPathParams(TIMEZONE_UTC, null, null),
                new GetEndZonedDateTimeHappyPathParams(TIMEZONE_AMERICA_ST_JOHNS_ID, null, null),
                new GetEndZonedDateTimeHappyPathParams(TIMEZONE_AMERICA_TORONTO_ID, null, null),
                new GetEndZonedDateTimeHappyPathParams(TIMEZONE_AMERICA_DENVER_ID, null, null),
                new GetEndZonedDateTimeHappyPathParams(null, "2021", _2021_12_31_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(ZONE_ID_Z, "2021", _2021_12_31_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2021", _2021_12_31_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID, "2021", _2021_12_31_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID, "2021", _2021_12_31_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2021", _2021_12_31_23_59_59.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetEndZonedDateTimeHappyPathParams(null, "2022-08", _2022_08_31_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2022-08", _2022_08_31_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2022-08", _2022_08_31_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2022-08",
                        _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID, "2022-08", _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2022-08", _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetEndZonedDateTimeHappyPathParams(null, "2022-02", _2022_02_28_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2022-02", _2022_02_28_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2022-02", _2022_02_28_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2022-02",
                        _2022_02_28_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID, "2022-02", _2022_02_28_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2022-02", _2022_02_28_23_59_59.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetEndZonedDateTimeHappyPathParams(null, "2024-02", _2024_02_29_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-02", _2024_02_29_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-02", _2024_02_29_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-02",
                        _2024_02_29_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID, "2024-02", _2024_02_29_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2024-02", _2024_02_29_23_59_59.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetEndZonedDateTimeHappyPathParams(null, "2024-02-26", _2024_02_26_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-02-26", _2024_02_26_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-02-26", _2024_02_26_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-02-26",
                        _2024_02_26_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-02-26",
                        _2024_02_26_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2024-02-26", _2024_02_26_23_59_59.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetEndZonedDateTimeHappyPathParams(null, "2024-09-26", _2024_09_26_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-09-26", _2024_09_26_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-09-26", _2024_09_26_23_59_59.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-09-26",
                        _2024_09_26_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-09-26",
                        _2024_09_26_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID, "2024-09-26", _2024_09_26_23_59_59.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetEndZonedDateTimeHappyPathParams(
                        null, "2024-01-02T12:00:00", _2024_01_02_12_00_00.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-01-02T12:00:00", _2024_01_02_12_00_00.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-01-02T12:00:00", _2024_01_02_12_00_00.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-01-02T12:00:00",
                        _2024_01_02_12_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-01-02T12:00:00",
                        _2024_01_02_12_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID,
                        "2024-01-02T12:00:00",
                        _2024_01_02_12_00_00.atZone(TIMEZONE_AMERICA_DENVER)),
                new GetEndZonedDateTimeHappyPathParams(
                        null, "2024-09-26T12:00:00", _2024_09_26_12_00_00.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        ZONE_ID_Z, "2024-09-26T12:00:00", _2024_09_26_12_00_00.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_UTC, "2024-09-26T12:00:00", _2024_09_26_12_00_00.atZone(ZoneOffset.UTC)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-09-26T12:00:00",
                        _2024_09_26_12_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-09-26T12:00:00",
                        _2024_09_26_12_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new GetEndZonedDateTimeHappyPathParams(
                        TIMEZONE_AMERICA_DENVER_ID,
                        "2024-09-26T12:00:00",
                        _2024_09_26_12_00_00.atZone(TIMEZONE_AMERICA_DENVER)));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("getStartZonedDateTime_happyPath_params")
    void getStartZonedDateTime_happyPath(GetStartZonedDateTimeHappyPathParams testCase) {

        final ZonedDateTime actualResult = myTestSubject.getStartZonedDateTime(
                testCase.theInputPeriodStart(), getRequestDetails(testCase.timezone()));

        assertThat(actualResult).isEqualTo(testCase.expectedResult());
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("getEndZonedDateTime_happyPath_params")
    void getEndZonedDateTime_happyPath(GetEndZonedDateTimeHappyPathParams testCase) {

        final ZonedDateTime actualResult =
                myTestSubject.getEndZonedDateTime(testCase.theInputPeriodEnd(), getRequestDetails(testCase.timezone()));

        assertThat(actualResult).isEqualTo(testCase.expectedResult());
    }

    private static Stream<ErrorParams> errorParams() {
        return Stream.of(
                new ErrorParams(
                        null,
                        "2024-01-01T12",
                        new InvalidRequestException("Unsupported Date/Time format for input: 2024-01-01T12")),
                new ErrorParams(
                        "Middle-Earth/Combe",
                        "2024-01-02",
                        new InvalidRequestException("Invalid value for Timezone header: Middle-Earth/Combe")),
                new ErrorParams(
                        null,
                        "2024-01-01T12:00:00-02:30",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-01-01T12:00:00-02:30")),
                new ErrorParams(
                        ZONE_ID_Z,
                        "2024-01-01T12:00:00-02:30",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-01-01T12:00:00-02:30")),
                new ErrorParams(
                        "UTC",
                        "2024-01-01T12:00:00-02:30",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-01-01T12:00:00-02:30")),
                new ErrorParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-01-01T12:00:00-02:30",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-01-01T12:00:00-02:30")),
                new ErrorParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-01-01T12:00:00-02:30",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-01-01T12:00:00-02:30")),
                new ErrorParams(
                        TIMEZONE_AMERICA_DENVER_ID,
                        "2024-01-01T12:00:00-02:30",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-01-01T12:00:00-02:30")),
                new ErrorParams(
                        null,
                        "2024-09-25T12:00:00-06:00",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-09-25T12:00:00-06:00")),
                new ErrorParams(
                        ZONE_ID_Z,
                        "2024-09-25T12:00:00-06:00",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-09-25T12:00:00-06:00")),
                new ErrorParams(
                        "UTC",
                        "2024-09-25T12:00:00-06:00",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-09-25T12:00:00-06:00")),
                new ErrorParams(
                        TIMEZONE_AMERICA_ST_JOHNS_ID,
                        "2024-09-25T12:00:00-06:00",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-09-25T12:00:00-06:00")),
                new ErrorParams(
                        TIMEZONE_AMERICA_TORONTO_ID,
                        "2024-09-25T12:00:00-06:00",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-09-25T12:00:00-06:00")),
                new ErrorParams(
                        TIMEZONE_AMERICA_DENVER_ID,
                        "2024-09-25T12:00:00-06:00",
                        new InvalidRequestException(
                                "Unsupported Date/Time format for input: 2024-09-25T12:00:00-06:00")));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("errorParams")
    void getStartZonedDateTime_errorPaths(ErrorParams testCase) {
        assertThatThrownBy(() -> myTestSubject.getStartZonedDateTime(
                        testCase.theInputPeriod(), getRequestDetails(testCase.timezone())))
                .hasMessage(testCase.expectedResult().getMessage())
                .isInstanceOf(testCase.expectedResult().getClass());
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("errorParams")
    void getEndZonedDateTime_errorPaths(ErrorParams testCase) {
        assertThatThrownBy(() -> myTestSubject.getEndZonedDateTime(
                        testCase.theInputPeriod(), getRequestDetails(testCase.timezone())))
                .hasMessage(testCase.expectedResult().getMessage())
                .isInstanceOf(testCase.expectedResult().getClass());
    }

    private static Stream<SerializeDeserializeRoundTripParams> serializeDeserializeRoundTripParams() {
        return Stream.of(
                new SerializeDeserializeRoundTripParams(
                        _2020_01_01_00_00_00.atZone(ZoneOffset.UTC), "2020-01-01T00:00:00Z"),
                new SerializeDeserializeRoundTripParams(
                        _2022_08_31_23_59_59.atZone(ZoneOffset.UTC), "2022-08-31T23:59:59Z"),
                new SerializeDeserializeRoundTripParams(
                        _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS), "2020-01-01T00:00:00-03:30"),
                new SerializeDeserializeRoundTripParams(
                        _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS), "2022-08-31T23:59:59-02:30"),
                new SerializeDeserializeRoundTripParams(
                        _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_TORONTO), "2020-01-01T00:00:00-05:00"),
                new SerializeDeserializeRoundTripParams(
                        _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO), "2022-08-31T23:59:59-04:00"),
                new SerializeDeserializeRoundTripParams(
                        _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_DENVER), "2020-01-01T00:00:00-07:00"),
                new SerializeDeserializeRoundTripParams(
                        _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_DENVER), "2022-08-31T23:59:59-06:00"));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("serializeDeserializeRoundTripParams")
    void serializeDeserializeRoundTrip(SerializeDeserializeRoundTripParams testCase) {
        final String actualResult = myTestSubject.serialize(testCase.theInputDateTime());

        assertThat(actualResult).isEqualTo(testCase.expectedResult());

        final ZonedDateTime deSerialized = myTestSubject.deSerialize(actualResult);

        assertThat(deSerialized).isEqualTo(testCase.theInputDateTime());
    }

    private static Stream<DeSerializeRoundTripParams> deSerializeRoundTripParams() {
        return Stream.of(
                new DeSerializeRoundTripParams("2020-01-01T00:00:00Z", _2020_01_01_00_00_00.atZone(ZoneOffset.UTC)),
                new DeSerializeRoundTripParams("2022-08-31T23:59:59Z", _2022_08_31_23_59_59.atZone(ZoneOffset.UTC)),
                new DeSerializeRoundTripParams(
                        "2020-01-01T00:00:00-03:30", _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new DeSerializeRoundTripParams(
                        "2022-08-31T23:59:59-02:30", _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_ST_JOHNS)),
                new DeSerializeRoundTripParams(
                        "2020-01-01T00:00:00-05:00", _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_TORONTO)),
                new DeSerializeRoundTripParams(
                        "2022-08-31T23:59:59-04:00", _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_TORONTO)),
                new DeSerializeRoundTripParams(
                        "2020-01-01T00:00:00-07:00", _2020_01_01_00_00_00.atZone(TIMEZONE_AMERICA_DENVER)),
                new DeSerializeRoundTripParams(
                        "2022-08-31T23:59:59-06:00", _2022_08_31_23_59_59.atZone(TIMEZONE_AMERICA_DENVER)));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("deSerializeRoundTripParams")
    void deSerializeRoundTrip(DeSerializeRoundTripParams testCase) {
        final ZonedDateTime actualResult = myTestSubject.deSerialize(testCase.theInputString());

        assertThat(actualResult).isEqualTo(testCase.expectedResult());

        final String serialized = myTestSubject.serialize(actualResult);

        assertThat(serialized).isEqualTo(testCase.theInputString());
    }

    private static RequestDetails getRequestDetails(@Nullable String timezone) {
        final SystemRequestDetails systemRequestDetails = new SystemRequestDetails();
        Optional.ofNullable(timezone)
                .ifPresent(nonNullTimezone ->
                        systemRequestDetails.addHeader(Constants.HEADER_CLIENT_TIMEZONE, nonNullTimezone));
        return systemRequestDetails;
    }
}
