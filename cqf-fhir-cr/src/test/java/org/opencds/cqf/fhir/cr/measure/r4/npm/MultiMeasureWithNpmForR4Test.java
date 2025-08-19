package org.opencds.cqf.fhir.cr.measure.r4.npm;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.SelectedReport;

import static org.junit.jupiter.api.Assertions.fail;

// TODO: LD :  introduce an R5 version of this test once R5 services/etc become available
class MultiMeasureWithNpmForR4Test extends BaseMeasureWithNpmForR4Test {
    private static final Given NPM_REPO_MULTI_MEASURE = MultiMeasure.given().repositoryPlusNpmFor("BasicNpmPackages");

    // LUKETODO:  multi-lib cross-package, common targets, similar to complex deps test

    @Test
    void evaluateSucceedsWithMinimalMeasureAndSingleSubject() {

        final SelectedReport then = NPM_REPO_MULTI_MEASURE
                .when()
                .measureUrl(MEASURE_URL_ALPHA)
                .measureUrl(MEASURE_URL_BRAVO)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_FEMALE_1944)
                .evaluate()
                .then();
        // LUKETODO:  figure out what mutli-meassure assertions we can make
        then.hasMeasureReportCount(2)
                .hasMeasureReportCountPerUrl(1, MEASURE_URL_ALPHA_WITH_VERSION)
                .hasMeasureReportCountPerUrl(1, MEASURE_URL_BRAVO_WITH_VERSION)
                .measureReport(MEASURE_URL_ALPHA_WITH_VERSION)
                //            .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
                //            .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_FEMALE_1944)
                .hasMeasureReportStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .evaluatedResource("Encounter/female-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                // We match the patient and the single finished encounter, which matches Alpha's where
                .hasCount(1)
                .up()
                .up()
                .up()
                .measureReport(MEASURE_URL_BRAVO_WITH_VERSION)
                //            .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2024_01_01))
                //            .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2025_01_01_MINUS_ONE_SECOND))
                .hasSubjectReference(PATIENT_FEMALE_1944)
                .hasMeasureReportStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(1)
                .evaluatedResource("Encounter/female-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                // No match for the second library, so zero
                .hasCount(0);

        final SelectedReport then1 = NPM_REPO_MULTI_MEASURE
                .when()
                .measureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
                .measureUrl(MEASURE_URL_BRAVO_WITH_VERSION)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_MALE_1944)
                .evaluate()
                .then();

        // LUKETODO:  figure out what mutli-meassure assertions we can make
        //            .hasMeasureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
        //            .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
        //            .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
        //            .hasSubjectReference(PATIENT_MALE_1944)
        //            .hasStatus(MeasureReportStatus.COMPLETE)
        //            .hasEvaluatedResourceCount(1)
        //            .evaluatedResource("Encounter/male-1944-finished-encounter-1")
        //            .hasEvaluatedResourceReferenceCount(1)
        //            .up()
        //            .firstGroup()
        //            .population(INITIAL_POPULATION)
        //            // We match the patient and the single finished encounter, which matches Alpha's where
        //            .hasCount(1);
    }

    @Test
    void evaluateSucceedsWithMinimalMeasureAndAllSubjects() {

        // LUKETODO:  figure out what the deal is with measure periods and why they're different from single measure
        NPM_REPO_MULTI_MEASURE
                .when()
                .measureUrl(MEASURE_URL_ALPHA)
                .measureUrl(MEASURE_URL_BRAVO)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .evaluate()
                .then()
                .hasMeasureReportCount(20)
                .hasMeasureReportCountPerUrl(10, MEASURE_URL_ALPHA_WITH_VERSION)
                .hasMeasureReportCountPerUrl(10, MEASURE_URL_BRAVO_WITH_VERSION)
                .measureReport(MEASURE_URL_ALPHA_WITH_VERSION, PATIENT_FEMALE_1944)
                .hasSubjectReference(PATIENT_FEMALE_1944)
                .hasMeasureReportStatus(MeasureReportStatus.COMPLETE)
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
                .hasCount(8)
                .up()
                .up()
                .up()
                .measureReport(MEASURE_URL_BRAVO_WITH_VERSION, PATIENT_FEMALE_1944)
                .hasSubjectReference(PATIENT_FEMALE_1944)
                .hasMeasureReportStatus(MeasureReportStatus.COMPLETE)
                .hasEvaluatedResourceCount(11)
                .evaluatedResource("Encounter/female-1944-finished-encounter-1")
                .hasEvaluatedResourceReferenceCount(1)
                .up()
                .firstGroup()
                .population(INITIAL_POPULATION)
                // No match for the second library, so zero
                .hasCount(3);

        final SelectedReport then1 = NPM_REPO_MULTI_MEASURE
                .when()
                .measureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
                .measureUrl(MEASURE_URL_BRAVO_WITH_VERSION)
                .reportType(MeasureEvalType.SUBJECT.toCode())
                .subject(PATIENT_MALE_1944)
                .evaluate()
                .then();

        // LUKETODO:  figure out what mutli-meassure assertions we can make
        //            .hasMeasureUrl(MEASURE_URL_ALPHA_WITH_VERSION)
        //            .hasPeriodStart(toJavaUtilDate(LOCAL_DATE_TIME_2021_01_01))
        //            .hasPeriodEnd(toJavaUtilDate(LOCAL_DATE_TIME_2022_01_01_MINUS_ONE_SECOND))
        //            .hasSubjectReference(PATIENT_MALE_1944)
        //            .hasStatus(MeasureReportStatus.COMPLETE)
        //            .hasEvaluatedResourceCount(1)
        //            .evaluatedResource("Encounter/male-1944-finished-encounter-1")
        //            .hasEvaluatedResourceReferenceCount(1)
        //            .up()
        //            .firstGroup()
        //            .population(INITIAL_POPULATION)
        //            // We match the patient and the single finished encounter, which matches Alpha's where
        //            .hasCount(1);
    }

    @Test
    void evaluateSucceedsWithMultiLibCrossPackageSingleSubject() {
        fail();
    }


    @Test
    void evaluateSucceedsWithMultiLibCrossPackageAllSubject() {
        NPM_REPO_MULTI_MEASURE
            .when()
            .measureUrl(MEASURE_URL_CROSSPACKAGE_SOURCE_1)
            .measureUrl(MEASURE_URL_CROSSPACKAGE_SOURCE_2)
            .reportType(MeasureEvalType.SUBJECT.toCode())
            .evaluate()
            .then()
            .hasMeasureReportCount(20)
            .hasMeasureReportCountPerUrl(10, MEASURE_URL_CROSSPACKAGE_SOURCE_1_VERSION)
            .hasMeasureReportCountPerUrl(10, MEASURE_URL_CROSSPACKAGE_SOURCE_2_VERSION);
    }
}
