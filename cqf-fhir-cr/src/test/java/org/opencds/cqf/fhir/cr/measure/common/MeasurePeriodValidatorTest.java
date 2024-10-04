package org.opencds.cqf.fhir.cr.measure.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.exception.InvalidInterval;

class MeasurePeriodValidatorTest {

    private static final ZonedDateTime _2024_09_01_UTC =
            LocalDate.of(2024, Month.SEPTEMBER, 1).atStartOfDay(ZoneOffset.UTC);
    private static final ZonedDateTime _2024_10_01_UTC =
            LocalDate.of(2024, Month.OCTOBER, 1).atStartOfDay(ZoneOffset.UTC);

    private final MeasurePeriodValidator testSubject = new MeasurePeriodValidator();

    private static Stream<Arguments> validatePeriodStartAndEndParams() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(_2024_09_01_UTC, _2024_10_01_UTC, null),
                Arguments.of(
                        null,
                        _2024_09_01_UTC,
                        new InvalidInterval(
                                "Invalid Period - Either both or neither should be null: start date: null and end date: 2024-09-01T00:00Z")),
                Arguments.of(
                        _2024_09_01_UTC,
                        null,
                        new InvalidInterval(
                                "Invalid Period - Either both or neither should be null: start date: 2024-09-01T00:00Z and end date: null")),
                Arguments.of(
                        _2024_10_01_UTC,
                        _2024_10_01_UTC,
                        new InvalidInterval(
                                "Invalid Period - Start date: 2024-10-01T00:00Z is the same as end date: 2024-10-01T00:00Z")),
                Arguments.of(
                        _2024_10_01_UTC,
                        _2024_09_01_UTC,
                        new InvalidInterval(
                                "Invalid Period - the ending boundary: 2024-09-01T00:00Z must be greater than or equal to the starting boundary: 2024-10-01T00:00Z")));
    }

    @ParameterizedTest
    @MethodSource("validatePeriodStartAndEndParams")
    void validatePeriodStartAndEnd(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable Exception expectedException) {
        try {
            testSubject.validatePeriodStartAndEnd(periodStart, periodEnd);
            if (expectedException != null) {
                fail(String.format(
                        "Expected the following exception: %s but the method did not throw one", expectedException));
            }
        } catch (Exception actualException) {
            if (expectedException == null) {
                fail("Expected no Exception, but the following was thrown: " + actualException);
                return;
            }

            assertThat(actualException.getClass(), equalTo(expectedException.getClass()));
            assertThat(actualException.getMessage(), equalTo(expectedException.getMessage()));
        }
    }
}
