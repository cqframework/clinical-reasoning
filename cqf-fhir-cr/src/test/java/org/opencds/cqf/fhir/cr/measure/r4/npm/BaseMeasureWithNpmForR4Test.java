package org.opencds.cqf.fhir.cr.measure.r4.npm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Date;

public abstract class BaseMeasureWithNpmForR4Test {

    static final LocalDateTime LOCAL_DATE_TIME_2020_01_01 =
            LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay();
    static final LocalDateTime LOCAL_DATE_TIME_2021_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2021, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    static final LocalDateTime LOCAL_DATE_TIME_2021_01_01 =
            LocalDate.of(2021, Month.JANUARY, 1).atStartOfDay();
    static final LocalDateTime LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    static final LocalDateTime LOCAL_DATE_TIME_2022_01_01 =
            LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay();
    static final LocalDateTime LOCAL_DATE_TIME_2023_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2023, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    static final LocalDateTime LOCAL_DATE_TIME_2024_01_01 =
            LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay();
    static final LocalDateTime LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2025, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    static final String PIPE = "|";
    static final String VERSION_0_1 = "0.1";
    static final String VERSION_0_2 = "0.2";
    static final String VERSION_0_5 = "0.5";

    static final String PATIENT_FEMALE_1944 = "Patient/female-1944";
    static final String PATIENT_MALE_1944 = "Patient/male-1944";

    static final String ENCOUNTER_MALE_1988_FINISHED_ENCOUNTER_1 = "Encounter/male-1988-finished-encounter-1";
    static final String ENCOUNTER_FEMALE_1944_FINISHED_ENCOUNTER_1 = "Encounter/female-1944-finished-encounter-1";

    static final String SIMPLE_ALPHA = "SimpleAlpha";
    static final String SIMPLE_BRAVO = "SimpleBravo";

    static final String MULTILIB_CROSSPACKAGE_SOURCE_1 = "MultiLibCrossPackageSource1";
    static final String MULTILIB_CROSSPACKAGE_SOURCE_2 = "MultiLibCrossPackageSource2";

    static final String SIMPLE_URL = "http://example.com";
    static final String MULTILIB_CROSSPACKAGE_SOURCE_URL = "http://multilib.cross.package.source.npm.opencds.org";

    static final String SLASH_MEASURE_SLASH = "/Measure/";

    static final String MEASURE_URL_ALPHA = SIMPLE_URL + SLASH_MEASURE_SLASH + SIMPLE_ALPHA;
    static final String MEASURE_URL_ALPHA_WITH_VERSION = MEASURE_URL_ALPHA + PIPE + VERSION_0_2;
    static final String MEASURE_URL_BRAVO = SIMPLE_URL + SLASH_MEASURE_SLASH + SIMPLE_BRAVO;
    static final String MEASURE_URL_BRAVO_WITH_VERSION = MEASURE_URL_BRAVO + PIPE + VERSION_0_5;

    static final String MEASURE_URL_CROSSPACKAGE_SOURCE_1 =
            MULTILIB_CROSSPACKAGE_SOURCE_URL + SLASH_MEASURE_SLASH + MULTILIB_CROSSPACKAGE_SOURCE_1;
    static final String MEASURE_URL_CROSSPACKAGE_SOURCE_1_WITH_VERSION =
            MEASURE_URL_CROSSPACKAGE_SOURCE_1 + PIPE + VERSION_0_1;
    static final String MEASURE_URL_CROSSPACKAGE_SOURCE_2 =
            MULTILIB_CROSSPACKAGE_SOURCE_URL + SLASH_MEASURE_SLASH + MULTILIB_CROSSPACKAGE_SOURCE_2;
    static final String MEASURE_URL_CROSSPACKAGE_SOURCE_2_WITH_VERSION =
            MEASURE_URL_CROSSPACKAGE_SOURCE_2 + PIPE + VERSION_0_1;

    static final String INITIAL_POPULATION = "initial-population";
    static final String DENOMINATOR = "denominator";
    static final String NUMERATOR = "numerator";

    static Date toJavaUtilDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
    }
}
