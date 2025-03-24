package org.opencds.cqf.fhir.cr.measure.r4.npm;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.utility.npm.R4NpmPackageLoaderInMemory;

class MeasureWithNpmTest {

    private static final String SIMPLE_ALPHA = "simple-alpha";
    private static final String SIMPLE_BRAVO = "simple-bravo";
    private static final String WITH_DERIVED_LIBRARY = "with-derived-library";
    private static final String WITH_DERIVED_LIBRARY_UPPER = "WithDerivedLibrary";
    private static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES = "with-two-layers-derived-libraries";
    private static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER = "WithTwoLayersDerivedLibraries";

    private static final String SIMPLE_URL = "http://example.com";
    private static final String DERIVED_URL = "http://with-derived-library.npm.opencds.org";
    private static final String DERIVED_TWO_LAYERS_URL = "http://with-two-layers-derived-libraries.npm.opencds.org";

    private static final String SLASH_MEASURE_SLASH = "/Measure/";

    private static final String MEASURE_URL_ALPHA = SIMPLE_URL + SLASH_MEASURE_SLASH + SIMPLE_ALPHA;
    private static final String MEASURE_URL_BRAVO = SIMPLE_URL + SLASH_MEASURE_SLASH + SIMPLE_BRAVO;
    private static final String MEASURE_URL_WITH_DERIVED_LIBRARY =
            DERIVED_URL + SLASH_MEASURE_SLASH + WITH_DERIVED_LIBRARY_UPPER;
    private static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES =
            DERIVED_TWO_LAYERS_URL + SLASH_MEASURE_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER;

    private static final String CROSS_PACKAGE_SOURCE = "cross-package-source";
    private static final String CROSS_PACKAGE_SOURCE_UPPER = "CrossPackageSource";
    private static final String CROSS_PACKAGE_TARGET = "cross-package-target";

    private static final String CROSS_PACKAGE_SOURCE_URL = "http://cross.package.source.npm.opencds.org";

    private static final String MEASURE_URL_CROSS_PACKAGE_SOURCE =
            CROSS_PACKAGE_SOURCE_URL + SLASH_MEASURE_SLASH + CROSS_PACKAGE_SOURCE_UPPER;

    private static final LocalDateTime LOCAL_DATE_TIME_2020_01_01 =
            LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2021_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2021, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    private static final LocalDateTime LOCAL_DATE_TIME_2021_01_01 =
            LocalDate.of(2021, Month.JANUARY, 1).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    private static final LocalDateTime LOCAL_DATE_TIME_2022_01_01 =
            LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2023_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2023, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    private static final LocalDateTime LOCAL_DATE_TIME_2024_01_01 =
            LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay();
    private static final LocalDateTime LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2025, Month.JANUARY, 1).atStartOfDay().minusNanos(1);
    private static final String PATIENT_ID = "pat1";
    private static final String PATIENT_REFERENCE = ResourceType.Patient + "/pat1";
    private static final String INITIAL_POPULATION = "initial-population";
    private static final String DENOMINATOR = "denominator";
    private static final String NUMERATOR = "numerator";

    @Test
    void evaluateSucceedsWithMinimalMeasure() {
        final Given npmRepo = initNpmRepos(SIMPLE_ALPHA);

        npmRepo.when()
                .measureUrl(MEASURE_URL_ALPHA)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_ALPHA)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasEmptySubject()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(0);
    }

    @Test
    void evaluateSucceedsWithMeasureAndBasicPatient() {
        final Given npmRepo = initNpmRepos(SIMPLE_BRAVO);

        setupPatient(npmRepo);

        npmRepo.when()
                .measureUrl(MEASURE_URL_BRAVO)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_BRAVO)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2024_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_REFERENCE)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(0);
    }

    @Test
    void evaluateWithDerivedLibraryOneLayer() {
        final Given npmRepo = initNpmRepos(SIMPLE_ALPHA, WITH_DERIVED_LIBRARY);

        setupPatient(npmRepo);

        npmRepo.when()
                .measureUrl(MEASURE_URL_ALPHA)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_ALPHA)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_REFERENCE)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(0);

        npmRepo.when()
                .measureUrl(MEASURE_URL_WITH_DERIVED_LIBRARY)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_WITH_DERIVED_LIBRARY)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_REFERENCE)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population(INITIAL_POPULATION)
                .hasCount(1)
                .up()
                .population(DENOMINATOR)
                .hasCount(1)
                .up()
                .population(NUMERATOR)
                .hasCount(1);
    }

    @Test
    void evaluateWithDerivedLibraryTwoLayers() {
        final Given npmRepo = initNpmRepos(SIMPLE_BRAVO, WITH_TWO_LAYERS_DERIVED_LIBRARIES);

        setupPatient(npmRepo);

        npmRepo.when()
                .measureUrl(MEASURE_URL_BRAVO)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_BRAVO)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2024_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_REFERENCE)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(0);

        npmRepo.when()
                .measureUrl(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2023_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_REFERENCE)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population(INITIAL_POPULATION)
                .hasCount(1)
                .up()
                .population(DENOMINATOR)
                .hasCount(1)
                .up()
                .population(NUMERATOR)
                .hasCount(1);
    }

    @Test
    void evaluateWithDerivedLibraryCrossPackage() {
        final Given npmRepo = initNpmRepos(CROSS_PACKAGE_SOURCE, CROSS_PACKAGE_TARGET);

        setupPatient(npmRepo);

        npmRepo.when()
                .measureUrl(MEASURE_URL_CROSS_PACKAGE_SOURCE)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_CROSS_PACKAGE_SOURCE)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2020_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_REFERENCE)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population(INITIAL_POPULATION)
                .hasCount(1)
                .up()
                .population(DENOMINATOR)
                .hasCount(1)
                .up()
                .population(NUMERATOR)
                .hasCount(1);
    }

    private void setupPatient(Given npmRepo) {
        npmRepo.getRepository().update(new Patient().setId(new IdType(ResourceType.Patient.toString(), PATIENT_ID)));
    }

    private Given initNpmRepos(String... tgzFileNames) {
        return Measure.given()
                .r4NpmPackageLoader(
                        R4NpmPackageLoaderInMemory.fromNpmPackageTgzPath(getClass(), getPaths(tgzFileNames)));
    }

    private Path[] getPaths(String[] tgzFileNames) {
        return Arrays.stream(tgzFileNames)
                .map(tgzFileName -> Paths.get("BasicNpmPackages/%s.tgz".formatted(tgzFileName)))
                .toArray(Path[]::new);
    }

    private Date toJavaUtilDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
    }
}
