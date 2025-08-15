package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Date;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

// TODO: LD :  introduce an R5 version of this test once R5 services/etc become available
class MeasureSingleWithNpmForR4Test extends BaseMeasureWithNpmForR4Test {

    private static final String WITH_DERIVED_LIBRARY = "WithDerivedLibrary";
    private static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES = "WithTwoLayersDerivedLibraries";

    private static final String DERIVED_URL = "http://with-derived-library.npm.opencds.org";
    private static final String DERIVED_TWO_LAYERS_URL = "http://with-two-layers-derived-libraries.npm.opencds.org";


    private static final String MEASURE_URL_WITH_DERIVED_LIBRARY =
            DERIVED_URL + SLASH_MEASURE_SLASH + WITH_DERIVED_LIBRARY;
    private static final String MEASURE_URL_WITH_DERIVED_LIBRARY_WITH_VERSION =
            MEASURE_URL_WITH_DERIVED_LIBRARY + PIPE + VERSION_0_2;
    private static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES =
            DERIVED_TWO_LAYERS_URL + SLASH_MEASURE_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES;
    private static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION =
            MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES + PIPE + VERSION_0_1;

    private static final String CROSS_PACKAGE_SOURCE = "CrossPackageSource";
    private static final String CROSS_PACKAGE_TARGET = "CrossPackageTarget";

    private static final String CROSS_PACKAGE_SOURCE_URL = "http://cross.package.source.npm.opencds.org";

    private static final String MEASURE_URL_CROSS_PACKAGE_SOURCE =
            CROSS_PACKAGE_SOURCE_URL + SLASH_MEASURE_SLASH + CROSS_PACKAGE_SOURCE;
    private static final String MEASURE_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION =
            MEASURE_URL_CROSS_PACKAGE_SOURCE + PIPE + VERSION_0_2;

    private static final Given NPM_REPO_SINGLE_MEASURE = Measure.given().repositoryFor("BasicNpmPackages");

    private static final String PATIENT_MALE_1988 = "Patient/male-1988";

    // LUKETODO:  set up a multilib eval with NPM

    @Test
    void evaluateSucceedsWithMinimalMeasureAndSingleSubject() {

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_ALPHA)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_FEMALE_1944)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_FEMALE_1944)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .evaluatedResource("Encounter/female-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                // We match the patient and the single finished encounter, which matches Alpha's where
                .hasCount(1);

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_MALE_1944)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_MALE_1944)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .evaluatedResource("Encounter/male-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                // We match the patient and the single finished encounter, which matches Alpha's where
                .hasCount(1);
    }

    @Test
    void evaluateSucceedsWithBasicPatientAndSingleSubject() {

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_BRAVO)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_MALE_1944)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_BRAVO_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2024_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_MALE_1944)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .evaluatedResource("Encounter/male-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                // there are 0 planned encounters corresponding to Bravo's where and the patient
                .hasCount(0);
    }

    @Test
    void evaluateSucceedsWithBasicPatientAllSubjects() {

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_BRAVO)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_BRAVO_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2024_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND))
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(11)
                .evaluatedResource("Encounter/female-1914-planned-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/female-1931-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/female-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/female-1988-2-finished-encounter-invalid-period")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/female-1988-planned-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/female-1988-finished-encounter-2")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/female-2021-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/male-1931-planned-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/male-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource(ENCOUNTER_MALE_1988_FINISHED_ENCOUNTER_1)
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .evaluatedResource("Encounter/male-2022-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                .hasCode("initial-population")
                .hasCount(3); // there are 3 planned encounters which corresponds to Bravo's where
    }

    @Test
    void evaluateWithDerivedLibraryOneLayerAndSingleSubject() {

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_ALPHA)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_FEMALE_1944)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_FEMALE_1944)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .evaluatedResource("Encounter/female-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                .hasCount(1);

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_WITH_DERIVED_LIBRARY)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_FEMALE_1944)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_WITH_DERIVED_LIBRARY_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_FEMALE_1944)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population(INITIAL_POPULATION)
                .hasCount(1);
    }

    @Test
    void evaluateWithDerivedLibraryOneLayerAndAllSubjects() {
        fail();
    }

    @Test
    void evaluateWithSingleMeasureDerivedLibraryTwoLayersOneSubject() {

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_BRAVO)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_MALE_1988)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_BRAVO_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2024_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_MALE_1988)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .evaluatedResource(ENCOUNTER_MALE_1988_FINISHED_ENCOUNTER_1)
                .hasEvaluatedResourceReferenceCount(1);

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_MALE_1988)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2023_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_MALE_1988)
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population(INITIAL_POPULATION)
                .hasCount(1)
                .up()
                .population(DENOMINATOR)
                // LUKETODO:  investigate to see if this is correct for male 1988
                .hasCount(1)
                .up()
                .population(NUMERATOR)
                .hasCount(1);
    }

    @Test
    void evaluateWithDerivedLibraryTwoLayersAllSubjects() {
        fail();
    }

    @Test
    void evaluateWithDerivedLibraryCrossPackageSingleSubject() {

        NPM_REPO_SINGLE_MEASURE.when()
                .measureUrl(MEASURE_URL_CROSS_PACKAGE_SOURCE)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_FEMALE_1944)
                .evaluate()
                .then()
                .hasMeasureUrl(MEASURE_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION)
                .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2020_01_01))
                .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_FEMALE_1944)
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
}
