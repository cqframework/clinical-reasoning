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
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.utility.npm.R4NpmPackageLoaderInMemory;

class MeasureWithNpmTest {

    static final LocalDateTime LOCAL_DATE_TIME_2021_01_01 =
            LocalDate.of(2021, Month.JANUARY, 1).atStartOfDay();
    static final LocalDateTime LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    static final LocalDateTime LOCAL_DATE_TIME_2024_01_01 =
            LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay();
    static final LocalDateTime LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND =
            LocalDate.of(2025, Month.JANUARY, 1).atStartOfDay().minusNanos(1);

    // LUKETODO:  either add a patient to the InMemoryRepository or the NPM package so we can get results in the
    // MeasureReport
    // LUKETODO:  maybe a Bundle to the InMemoryRepository?
    @Test
    void evaluateSucceedsWithMinimalMeasure() {
        final Given npmRepo = initNpmRepos("simple-alpha");

        npmRepo.when()
                .measureUrl("http://example.com/Measure/simple-alpha")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureUrl("http://example.com/Measure/simple-alpha")
                .hasPeriodStart(Date.from(
                        LOCAL_DATE_TIME_2021_01_01.atZone(ZoneOffset.UTC).toInstant()))
                .hasPeriodEnd(Date.from(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND
                        .atZone(ZoneOffset.UTC)
                        .toInstant()))
                .hasEmptySubject()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(0);
    }

    @Test
    void evaluateSucceedsWithMeasureAndBasicPatient() {
        final Given npmRepo = initNpmRepos("simple-bravo");

        npmRepo.getRepository().update(new Patient().setId(new IdType("Patient", "pat1")));

        npmRepo.when()
                .measureUrl("http://example.com/Measure/simple-bravo")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureUrl("http://example.com/Measure/simple-bravo")
                .hasPeriodStart(Date.from(
                        LOCAL_DATE_TIME_2024_01_01.atZone(ZoneOffset.UTC).toInstant()))
                .hasPeriodEnd(Date.from(LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND
                        .atZone(ZoneOffset.UTC)
                        .toInstant()))
                .hasSubjectReference("Patient/pat1")
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(0);
    }

    // LUKETODO:  test Measure with groups/populations so we can assert the initial population and other counts
    // LUKETODO:  test one layer of derived libraries
    // LUKETODO:  test two layers of derived libraries

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
}
