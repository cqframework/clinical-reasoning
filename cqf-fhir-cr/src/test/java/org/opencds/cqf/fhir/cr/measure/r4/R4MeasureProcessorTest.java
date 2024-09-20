package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import ca.uhn.fhir.parser.IParser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class R4MeasureProcessorTest {
    private static final String MEASURE_ID_1 = "measureId1";
    private static final String MEASURE_URL_1 = "http://example.com/measure-1";
    private static final String LIBRARY_ID = "MinimalProportionBooleanBasisSingleGroup";
    private static final String LIBRARY_URL = "http://example.com/Library/MinimalProportionBooleanBasisSingleGroup";

    private static final String CQL_FILE_PATH = "/org/opencds/cqf/fhir/cr/measure/r4/MinimalMeasureEvaluation/cql/MinimalProportionBooleanBasisSingleGroup.cql";

    private static final LocalDate LOCAL_DATE_2024_08_19 = LocalDate.of(2024, Month.AUGUST, 19);
    private static final LocalDate LOCAL_DATE_2024_08_20 = LocalDate.of(2024, Month.AUGUST, 20);

    private static final LocalDate LOCAL_DATE_2024_02_19 = LocalDate.of(2024, Month.FEBRUARY, 19);
    private static final LocalDate LOCAL_DATE_2024_02_20 = LocalDate.of(2024, Month.FEBRUARY, 20);

    private static final LocalTime LOCAL_TIME_00_00_00 = LocalTime.of(0,0,0);
    private static final LocalTime LOCAL_TIME_23_59_59 = LocalTime.of(23, 59, 59);

    private static final LocalDateTime LOCAL_DATE_TIME_2024_08_19_00_00_00 = LocalDateTime.of(LOCAL_DATE_2024_08_19, LOCAL_TIME_00_00_00);
    private static final LocalDateTime LOCAL_DATE_TIME_2024_08_20_23_59_59 = LocalDateTime.of(LOCAL_DATE_2024_08_20, LOCAL_TIME_23_59_59);

    private static final LocalDateTime LOCAL_DATE_TIME_2024_02_19_00_00_00 = LocalDateTime.of(LOCAL_DATE_2024_02_19, LOCAL_TIME_00_00_00);
    private static final LocalDateTime LOCAL_DATE_TIME_2024_02_20_23_59_59 = LocalDateTime.of(LOCAL_DATE_2024_02_20, LOCAL_TIME_23_59_59);

    // Half hour offset timezone
    private static final ZoneId TIMEZONE_NEWFOUNDLAND = ZoneId.of("America/St_Johns");
    private static final ZoneId TIMEZONE_EASTERN = ZoneId.of("America/Toronto");
    private static final ZoneId TIMEZONE_MOUNTAIN = ZoneId.of("America/Denver");

    private static final DateTimeFormatter EVALUATE_MEASURE_PERIOD_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final FhirContext ourFhirContext = FhirContext.forR4Cached();
    private static final IParser ourJsonParser = ourFhirContext.newJsonParser();

    private final InMemoryFhirRepository myRepository = new InMemoryFhirRepository( FhirContext.forR4Cached());
    private final EvaluationSettings myEvaluationSettings = EvaluationSettings.getDefault();
    private final MeasureEvaluationOptions myMeasureEvaluationOptions = new MeasureEvaluationOptions()
        .setEvaluationSettings(myEvaluationSettings);;
    private final R4RepositorySubjectProvider mySubjectProvider = new R4RepositorySubjectProvider();;
    private final R4MeasureProcessor myTestSubject = new R4MeasureProcessor(myRepository, myMeasureEvaluationOptions, mySubjectProvider);

    private static Stream<Arguments> evaluateMeasureParams() {
        return Stream.of(
            Arguments.of(
                LOCAL_DATE_2024_08_19,
                LOCAL_DATE_2024_08_20,
                null,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_00_00_00,
                    LOCAL_DATE_TIME_2024_08_20_23_59_59,
                    UTC
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_08_19,
                LOCAL_DATE_2024_08_20,
                ZoneOffset.UTC,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_00_00_00,
                    LOCAL_DATE_TIME_2024_08_20_23_59_59,
                    UTC
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_08_19,
                LOCAL_DATE_2024_08_20,
                TIMEZONE_NEWFOUNDLAND,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_00_00_00,
                    LOCAL_DATE_TIME_2024_08_20_23_59_59,
                    TIMEZONE_NEWFOUNDLAND
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_08_19,
                LOCAL_DATE_2024_08_20,
                TIMEZONE_EASTERN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_00_00_00,
                    LOCAL_DATE_TIME_2024_08_20_23_59_59,
                    TIMEZONE_EASTERN
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_08_19,
                LOCAL_DATE_2024_08_20,
                TIMEZONE_MOUNTAIN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_08_19_00_00_00,
                    LOCAL_DATE_TIME_2024_08_20_23_59_59,
                    TIMEZONE_MOUNTAIN
                )
            ),

            Arguments.of(
                LOCAL_DATE_2024_02_19,
                LOCAL_DATE_2024_02_20,
                null,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_00_00_00,
                    LOCAL_DATE_TIME_2024_02_20_23_59_59,
                    UTC
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_02_19,
                LOCAL_DATE_2024_02_20,
                ZoneOffset.UTC,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_00_00_00,
                    LOCAL_DATE_TIME_2024_02_20_23_59_59,
                    UTC
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_02_19,
                LOCAL_DATE_2024_02_20,
                TIMEZONE_NEWFOUNDLAND,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_00_00_00,
                    LOCAL_DATE_TIME_2024_02_20_23_59_59,
                    TIMEZONE_NEWFOUNDLAND
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_02_19,
                LOCAL_DATE_2024_02_20,
                TIMEZONE_EASTERN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_00_00_00,
                    LOCAL_DATE_TIME_2024_02_20_23_59_59,
                    TIMEZONE_EASTERN
                )
            ),
            Arguments.of(
                LOCAL_DATE_2024_02_19,
                LOCAL_DATE_2024_02_20,
                TIMEZONE_MOUNTAIN,
                buildPeriod(
                    LOCAL_DATE_TIME_2024_02_19_00_00_00,
                    LOCAL_DATE_TIME_2024_02_20_23_59_59,
                    TIMEZONE_MOUNTAIN
                )
            )
        );
    }

    @BeforeEach
    void beforeEach() {
        myRepository.update(buildMeasure(MEASURE_ID_1, MEASURE_URL_1));

        myRepository.update(buildLibrary());
    }

    @ParameterizedTest
    @MethodSource("evaluateMeasureParams")
    void testme(LocalDate periodStart, LocalDate periodEnd, @Nullable ZoneId clientTimezone, Period expectedPeriod) {
        Optional.ofNullable(clientTimezone)
            .ifPresent(zoneIdNonNull -> myEvaluationSettings.setClientTimezone( clientTimezone));

        final MeasureReport actualMeasureReport = myTestSubject.evaluateMeasure(
            Eithers.forMiddle3(new IdType("Measure", MEASURE_ID_1)),
            toYyyyMmdd(periodStart),
            toYyyyMmdd(periodEnd),
            "",
            List.of(),
            null,
            new Parameters()
        );

        assertNotNull(actualMeasureReport.getPeriod());

        final ZoneId expectedClientTimezone =
            Optional.ofNullable(clientTimezone)
                .orElse(ZoneOffset.UTC);

        assertEquals(expectedClientTimezone, myEvaluationSettings.getClientTimezone());

        assertPeriodEquals(expectedPeriod, actualMeasureReport.getPeriod());
    }

    private void assertPeriodEquals(Period expectedPeriod, Period actualPeriod) {
        assertDatesEqualNoMillis(expectedPeriod.getStart(), actualPeriod.getStart());
        assertDatesEqualNoMillis(expectedPeriod.getEnd(), actualPeriod.getEnd());
    }

    public static Library buildLibrary() {
        return
            ((Library)new Library()
                .setId(LIBRARY_ID))
                .setUrl(LIBRARY_URL)
                .setName(LIBRARY_ID)
                .setStatus(Enumerations.PublicationStatus.ACTIVE)
                .addContent(new Attachment()
                    .setContentType("text/cql")
                    .setData(getFileBytes(CQL_FILE_PATH))
                );
    }

    private static byte[] getFileBytes(String theCqlFilePath) {
        final InputStream inputStream = getInputStreamForFilePath(theCqlFilePath);

        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException theE) {
            fail("Could not read InputStream for path: " + theCqlFilePath);
            // Need the below so this will compile:
            return new byte[]{};
        }
    }

    private static Measure buildMeasure(String theId, String theMeasureUrl) {
        return (Measure)new Measure()
            .setUrl(theMeasureUrl)
            .addLibrary(LIBRARY_URL)
            .setId(new IdType("Measure", theId));
    }

    @Nonnull
    private static InputStream getInputStreamForFilePath(String theFilePath) {
        final InputStream inputStream = R4MeasureProcessor.class.getResourceAsStream(theFilePath);
        assertNotNull(inputStream);
        return inputStream;
    }

    @Nullable
    private static Period buildPeriod(ZonedDateTime thePeriodStart, ZonedDateTime thePeriodEnd) {
        if (thePeriodStart == null || thePeriodEnd == null) {
            return null;
        }
        return new Period()
            .setStart(toJavaUtilDate(thePeriodStart))
            .setEnd(toJavaUtilDate(thePeriodEnd));
    }

    @Nullable
    private static Period buildPeriod(LocalDateTime periodStart, LocalDateTime periodEnd, ZoneId zoneId) {
        if (periodStart == null || periodEnd == null) {
            return null;
        }
        return new Period()
            .setStart(toJavaUtilDate(periodStart, zoneId))
            .setEnd(toJavaUtilDate(periodEnd, zoneId));
    }

    private static Date toJavaUtilDate(LocalDateTime localDateTime, ZoneId zoneId) {
        return Date.from(localDateTime
            .atZone(zoneId)
            .toInstant());
    }

    private static String toYyyyMmdd(LocalDate localDate) {
        return EVALUATE_MEASURE_PERIOD_DATE_TIME_FORMATTER.format(localDate);
    }

    private static Date toJavaUtilDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    private static void assertDatesEqualNoMillis(@Nullable Date theExpectedDate, @Nullable Date theActualDate) {
        assertEquals(stripMillisOrNull(theExpectedDate), stripMillisOrNull(theActualDate));
    }

    @Nullable
    private static Date stripMillisOrNull(@Nullable Date theDateWithMillis) {
        return Optional.ofNullable(theDateWithMillis)
            .map(nonNullDate -> Date.from(nonNullDate.toInstant().truncatedTo(ChronoUnit.SECONDS)))
            .orElse(null);
    }
}