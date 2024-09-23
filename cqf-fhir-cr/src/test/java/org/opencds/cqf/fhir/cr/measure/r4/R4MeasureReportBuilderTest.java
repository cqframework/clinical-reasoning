package org.opencds.cqf.fhir.cr.measure.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.helper.IntervalHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

@TestInstance(Lifecycle.PER_CLASS)
class R4MeasureReportBuilderTest {

    private static final ZoneId TIMEZONE_NEWFOUNDLAND = ZoneId.of("America/St_Johns");
    private static final ZoneId TIMEZONE_EASTERN = ZoneId.of("America/Toronto");
    private static final ZoneId TIMEZONE_MOUNTAIN = ZoneId.of("America/Denver");
    private static final String _2024_08_19 = "2024-08-19";
    private static final String _2024_08_20 = "2024-08-20";
    private static final String _2024_02_19 = "2024-02-19";
    private static final String _2024_02_20 = "2024-02-20";
    private static final LocalDateTime LOCAL_DATE_TIME_2024_08_19_START = LocalDate.of(2024, Month.AUGUST, 19).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND = LocalDate.of(2024, Month.AUGUST, 21).atStartOfDay().minusSeconds(1);
    private static final LocalDateTime LOCAL_DATE_TIME_2024_02_19_START = LocalDate.of(2024, Month.FEBRUARY, 19).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND = LocalDate.of(2024, Month.FEBRUARY, 21).atStartOfDay().minusSeconds(1);

    protected R4MeasureReportBuilder measureReportBuilder;
    protected FhirContext fhirContext;

    @BeforeAll
    void setup() {
        this.measureReportBuilder = new R4MeasureReportBuilder();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    }

    @Test
    void checkIfNotBooleanBasedMeasure() {
        IParser parser = fhirContext.newJsonParser();
        Measure measureEncounterBias =
                (Measure) parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream(
                        "MeasureBuilderSampleWithPopulationBiasEncounter.json"));
        Measure measureWithoutExtension = (Measure) parser.parseResource(
                R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithoutExtension.json"));
        Measure measureBooleanBias =
                (Measure) parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream(
                        "MeasureBuilderSampleWithPopulationBiasBoolean.json"));
        Measure measureWithEmptyExtension = (Measure) parser.parseResource(
                R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithEmptyExtension.json"));

        R4MeasureBasisDef measureBasisDef = new R4MeasureBasisDef();

        assertNotNull(measureEncounterBias);
        assertFalse(measureBasisDef.isBooleanBasis(measureEncounterBias));

        assertNotNull(measureWithoutExtension);
        assertFalse(measureBasisDef.isBooleanBasis(measureWithoutExtension));

        assertNotNull(measureBooleanBias);
        assertTrue(measureBasisDef.isBooleanBasis(measureBooleanBias));

        assertNotNull(measureWithEmptyExtension);
        assertFalse(measureBasisDef.isBooleanBasis(measureWithEmptyExtension));
    }

    public Stream<Arguments> periodParams() {
        return Stream.of(
            Arguments.of(
                _2024_08_19,
                _2024_08_20,
                ZoneOffset.UTC,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_START,
                    LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                    ZoneOffset.UTC)
            ),
            Arguments.of(
                _2024_08_19,
                _2024_08_20,
                TIMEZONE_NEWFOUNDLAND,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_START,
                    LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                    TIMEZONE_NEWFOUNDLAND)
            ),
            Arguments.of(
                _2024_08_19,
                _2024_08_20,
                TIMEZONE_EASTERN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_START,
                    LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                    TIMEZONE_EASTERN)
            ),
            Arguments.of(
                _2024_08_19,
                _2024_08_20,
                TIMEZONE_MOUNTAIN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_START,
                    LOCAL_DATE_TIME_2024_08_21_MINUS_ONE_SECOND,
                    TIMEZONE_MOUNTAIN)
            ),
            Arguments.of(
                _2024_02_19,
                _2024_02_20,
                ZoneOffset.UTC,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_START,
                    LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                    ZoneOffset.UTC)
            ),
            Arguments.of(
                _2024_02_19,
                _2024_02_20,
                TIMEZONE_NEWFOUNDLAND,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_START,
                    LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                    TIMEZONE_NEWFOUNDLAND)
            ),
            Arguments.of(
                _2024_02_19,
                _2024_02_20,
                TIMEZONE_EASTERN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_START,
                    LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                    TIMEZONE_EASTERN)
            ),
            Arguments.of(
                _2024_02_19,
                _2024_02_20,
                TIMEZONE_MOUNTAIN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_START,
                    LOCAL_DATE_TIME_2024_02_21_MINUS_ONE_SECOND,
                    TIMEZONE_MOUNTAIN)
            )
        );
    }

    @ParameterizedTest
    @MethodSource("periodParams")
    void getPeriod(String periodStart, String periodEnd, ZoneId zoneId, Period expectedPeriod) {
        final Interval interval = IntervalHelper.buildMeasurementPeriod(periodStart, periodEnd, zoneId);
        final Period actualPeriod = measureReportBuilder.getPeriod(interval);

        assertDatesEqualNoMillis(expectedPeriod.getStart(), actualPeriod.getStart());
        assertDatesEqualNoMillis(expectedPeriod.getEnd(), actualPeriod.getEnd());
    }

    private static Period buildPeriod(LocalDateTime localDateStart, LocalDateTime localDateEnd, ZoneId zoneId) {
        return new Period()
            .setStart(toJavaUtilDate(localDateStart, zoneId))
            .setEnd(toJavaUtilDate(localDateEnd, zoneId));
    }

    private static Date toJavaUtilDate(LocalDateTime localDate, ZoneId zoneId) {
        return Date.from(localDate.atZone(zoneId).toInstant());
    }

    public static void assertDatesEqualNoMillis(@Nullable Date theExpectedDate, @Nullable Date theActualDate) {
        assertThat(stripMillisOrNull(theActualDate), equalTo(stripMillisOrNull(theExpectedDate)));
    }

    @Nullable
    private static Date stripMillisOrNull(@Nullable Date theDateWithMillis) {
        return Optional.ofNullable(theDateWithMillis)
            .map(nonNullDate -> Date.from(nonNullDate.toInstant().truncatedTo(ChronoUnit.SECONDS)))
            .orElse(null);
    }
}
