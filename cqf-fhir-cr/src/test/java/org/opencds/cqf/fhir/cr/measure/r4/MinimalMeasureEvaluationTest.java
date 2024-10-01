package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class MinimalMeasureEvaluationTest {
    private static final Given GIVEN_REPO = Measure.given().repositoryFor("MinimalMeasureEvaluation");

    @Test
    void ProportionNoBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(8)
                .up()
                .population("denominator")
                .hasCount(5)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .hasScore("0.6");
    }

    @Test
    void ProportionNoBasisSingleGroup_InvalidEvalType() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("summary")
                .evaluate();

        when.then()
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(8)
                .up()
                .population("denominator")
                .hasCount(5)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .hasScore("0.6");
    }

    @Test
    void ProportionNoBasisSingleGroup_NullEvalType() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();

        when.then()
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(8)
                .up()
                .population("denominator")
                .hasCount(5)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .hasScore("0.6");
    }

    @Test
    void ProportionBooleanBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();
        // Denominator Exception that meets Numerator and does not meet Numerator
        // ** When subject meets criteria of Denominator Exception, but not Numerator, it shows count for Denominator
        // Exception, removes count from Denominator
        // ** When subject meets criteria of Denominator Exception, and also Numerator, it shows count for
        // Denominator/Numerator
        // Denominator Exclusion properly removed from Denominator
        // Numerator Exclusion removed from Numerator
        when.then()
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(8)
                .up()
                .population("denominator")
                .hasCount(5)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .hasScore("0.6");
    }

    @Test
    void ProportionBooleanBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0");
    }

    @Test
    void ProportionBooleanBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Patient")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("1.0");
    }

    @Test
    void ProportionResourceBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        // Denominator Exception that meets Numerator and does not meet Numerator
        // ** When subject meets criteria of Denominator Exception, but not Numerator, it shows count for Denominator
        // Exception, removes count from Denominator
        // ** When subject meets criteria of Denominator Exception, and also Numerator, it shows count for
        // Denominator/Numerator
        // Denominator Exclusion properly removed from Denominator
        // Numerator Exclusion removed from Numerator
        when.then()
                .firstGroup()
                .population("initial-population")
                .hasCount(9)
                .up()
                .population("denominator")
                .hasCount(6)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .hasScore("0.5");
    }

    @Test
    void ProportionResourceBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0");
    }

    @Test
    void ProportionResourceBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Encounter")
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("1.0");
    }

    @Test
    void RatioBooleanBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        when.then()
                .firstGroup()
                .population("initial-population")
                .hasCount(8)
                .up()
                .population("denominator")
                .hasCount(6)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .hasScore("0.5");
    }

    @Test
    void RatioBooleanBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0");
    }

    @Test
    void RatioBooleanBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Patient")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("1.0");
    }

    @Test
    void ProportionBooleanBasisSingleGroup_Error_MissingRequiredPopulation() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionResourceBasisSingleGroupErrorPopulation")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        // `Initial Population`, `Numerator`, `Denominator` are required Population Definitions for Measure Scoring
        // Type: proportion
        assertThrows(NullPointerException.class, () -> when.then().report());
    }

    @Test
    void RatioBooleanBasisSingleGroup_Error_DenominatorExceptionPresent() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalRatioBooleanBasisSingleGroupErrorPopulation")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        // `Denominator Exception` are not permitted for MeasureScoring type: ratio
        assertThrows(IllegalArgumentException.class, () -> when.then().report());
    }

    @Test
    void RatioResourceBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        when.then()
                .firstGroup()
                .population("initial-population")
                .hasCount(9)
                .up()
                .population("denominator")
                .hasCount(7)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator")
                .hasCount(3)
                .up()
                .hasScore("0.42857142857142855");
    }

    @Test
    void RatioResourceBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0");
    }

    @Test
    void RatioResourceBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Encounter")
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("denominator")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("0.5");
    }

    @Test
    void CohortResourceBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        when.then().firstGroup().population("initial-population").hasCount(9);
    }

    @Test
    void CohortResourceBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population("initial-population")
                .hasCount(2);
    }

    @Test
    void CohortResourceBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasPeriodStart(Date.from(LocalDate.of(2024, Month.JANUARY, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()))
                .hasPeriodEnd(Date.from(LocalDate.of(2024, Month.DECEMBER, 31)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()))
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Encounter")
                .firstGroup()
                .population("initial-population")
                .hasSubjectResults()
                .hasCount(2);
    }

    @Test
    void CohortBooleanBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        when.then().firstGroup().population("initial-population").hasCount(8);
    }

    @Test
    void CohortBooleanBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population("initial-population")
                .hasCount(1);
    }

    @Test
    void CohortBooleanBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Patient")
                .firstGroup()
                .population("initial-population")
                .hasSubjectResults()
                .hasCount(1);
    }

    @Test
    void CohortBooleanBasisSingleGroup_Error_MissingRequiredPopulation() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalCohortBooleanBasisSingleGroupErrorPopulation")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        // `Initial Population` is a required Population Definition for Measure Scoring Type: cohort
        assertThrows(NullPointerException.class, () -> when.then().report());
    }

    @Test
    void ContinuousVariableResourceBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        when.then()
                .firstGroup()
                .population("initial-population")
                .hasCount(9)
                .up()
                .population("measure-population")
                .hasCount(7)
                .up()
                .population("measure-population-exclusion")
                .hasCount(2)
                .up()
                .population("measure-observation")
                .hasCount(7);
    }

    @Test
    void ContinuousVariableResourceBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("measure-population")
                .hasCount(2)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                .hasCount(2);
    }

    @Test
    void ContinuousVariableResourceBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Encounter")
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("measure-population")
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population("measure-population-exclusion")
                .hasCount(0)
                .up()
                .population("measure-observation")
                .hasCount(2);
    }

    @Test
    void ContinuousVariableBooleanBasisSingleGroup_Population() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalContinuousVariableBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        // uses resource library instead of boolean
        when.then()
                .firstGroup()
                .population("initial-population")
                .hasCount(8)
                .up()
                .population("measure-population")
                .hasCount(6)
                .up()
                .population("measure-population-exclusion")
                .hasCount(2);
    }

    @Test
    void ContinuousVariableBooleanBasisSingleGroup_Subject() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalContinuousVariableBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(1)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("measure-population")
                .hasCount(1)
                .up()
                .population("measure-population-exclusion")
                .hasCount(0);
    }

    @Test
    void ContinuousVariableBooleanBasisSingleGroup_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalContinuousVariableBooleanBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .subjectResultsValidation()
                .containedListHasCorrectResourceType("Patient")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population("measure-population")
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population("measure-population-exclusion")
                .hasCount(0);
    }

    @Test
    void ContinuousVariableBooleanBasisSingleGroup_Error_MissingRequiredPopulation() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalContinuousVariableBooleanBasisSingleGroupErrorPopulation")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .evaluate();
        // `Initial Population` & `Measure Population` are required Population Definitions for Measure Scoring Type:
        // continuous-variable
        assertThrows(NullPointerException.class, () -> when.then().report());
    }

    @Test
    void MinimalProportionNoBasisSingleGroup_Practitioner() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .practitioner("tester")
                .evaluate();

        when.then()
                .hasReportType("Summary")
                .hasSubjectReference("Practitioner/tester")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(0)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(0);
    }

    @Test
    void ProportionResourceBasisSingleGroup_Subject_WithDOC() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionResourceBasisSingleGroupWithDOC")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .hasDateOfCompliance()
                .population("initial-population")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0");
    }

    @Test
    void ProportionResourceBasisSingleGroup_Subject_WithBadDOC() {
        try {
            var when = GIVEN_REPO
                    .when()
                    .measureId("MinimalProportionResourceBasisSingleGroupWithBadDOC")
                    .periodStart("2024-01-01")
                    .periodEnd("2024-12-31")
                    .reportType("subject")
                    .subject("Patient/female-1988")
                    .evaluate();

            when.then().report();
            Assertions.fail();
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "no expression was listed for extension: http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-care-gap-date-of-compliance-expression"));
        }
    }

    // MinimalProportionResourceBasisSingleGroupExclusionTest, Numerator value should be forced to false because of
    // Denominator exclusion=true
    @Test
    void MinimalProportionBooleanBasisSingleGroupExclusionTest() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionBooleanBasisSingleGroupExclusionTest")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();
        when.then()
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(0)
                .up()
                .population("denominator-exclusion")
                .hasCount(1)
                .up()
                .population("denominator-exception")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(0);
    }
}
