package org.opencds.cqf.fhir.cr.measure.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hamcrest.Matchers;
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
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Stream;

@TestInstance(Lifecycle.PER_CLASS)
class R4MeasureReportBuilderTest {

    private static final ZoneId TIMEZONE_NEWFOUNDLAND = ZoneId.of("America/St_Johns");
    private static final ZoneId TIMEZONE_EASTERN = ZoneId.of("America/Toronto");
    private static final ZoneId TIMEZONE_MOUNTAIN = ZoneId.of("America/Denver");

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
                "2024-08-19",
                "2024-08-20",
                TIMEZONE_NEWFOUNDLAND,
                buildPeriod(
                    LocalDate.of(2024, Month.AUGUST, 19),
                    LocalDate.of(2024, Month.AUGUST, 20),
                    TIMEZONE_NEWFOUNDLAND)
            ),
            Arguments.of(
                "2024-08-19",
                "2024-08-20",
                TIMEZONE_EASTERN,
                buildPeriod(
                    LocalDate.of(2024, Month.AUGUST, 19),
                    LocalDate.of(2024, Month.AUGUST, 20),
                    TIMEZONE_EASTERN)
            ),
            Arguments.of(
                "2024-08-19",
                "2024-08-20",
                TIMEZONE_MOUNTAIN,
                buildPeriod(
                    LocalDate.of(2024, Month.AUGUST, 19),
                    LocalDate.of(2024, Month.AUGUST, 20),
                    TIMEZONE_MOUNTAIN)
            )
        );
    }

    // LUKETODO:  complete this test
    @ParameterizedTest
    @MethodSource("periodParams")
    void getPeriod(String periodStart, String periodEnd, ZoneId zoneId, Period expectedPeriod) {
        final Interval interval = IntervalHelper.buildMeasurementPeriod(periodStart, periodEnd, zoneId);
        final Period actualPeriod = measureReportBuilder.getPeriod(interval);

        assertThat(actualPeriod.getStart(), equalTo(expectedPeriod.getStart()));
        assertThat(actualPeriod.getEnd(), equalTo(expectedPeriod.getEnd()));
    }

    private static Period buildPeriod(LocalDate localDateStart, LocalDate localDateEnd, ZoneId zoneId) {
        return new Period()
            .setStart(toJavaUtilDate(localDateStart, zoneId))
            .setEnd(toJavaUtilDate(localDateEnd, zoneId));
    }

    private static Date toJavaUtilDate(LocalDate localDate, ZoneId zoneId) {
        return Date.from(localDate.atStartOfDay()
                            .atZone(zoneId)
                            .toInstant());
    }
}
