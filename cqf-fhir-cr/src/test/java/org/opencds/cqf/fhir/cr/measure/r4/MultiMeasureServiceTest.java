package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureLookupException;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

@SuppressWarnings({"java:S2699"})
class MultiMeasureServiceTest {
    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MinimalMeasureEvaluation");
    private static final Given GIVEN_STRATIFIER_REPO = MultiMeasure.given().repositoryFor("MeasureStratifierTest");

    @Test
    void MultiMeasure_AllSubjects_MeasureIdentifier() {
        var when = GIVEN_REPO
                .when()
                .measureIdentifier("test123")
                .measureIdentifier("124")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                .hasSupplementalDataSearchParameter()
                // This is a population/summary report so we should have a single bundle containing
                // all MeasureReports
                .hasBundleCount(1)
                .hasMeasureReportCount(2)
                .reportBundles();
    }

    @Test
    void MultiMeasure_EightMeasures_AllSubjects_MeasureId() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                // This is a population/summary report so we should have a single bundle containing
                // all MeasureReports
                .hasBundleCount(1)
                .hasMeasureReportCount(7)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasReportType("Summary")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                .hasScore("0.5")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(6)
                .up()
                .hasScore("0.375")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATIONEXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.MEASUREOBSERVATION)
                .hasCount(8) // this is due to measurePopulation-Exclusion removed
                .up()
                .up()
                .hasMeasureReportStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Patient/female-1988-2")
                .hasContainedOperationOutcomeMsg("Invalid Interval");
    }

    @Test
    void MultiMeasure_EightMeasures_AllSubjects_MeasureUrl() {
        var when = GIVEN_REPO
                .when()
                .measureUrl("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .defs()
                .hasCount(7)
                .byMeasureUrl("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .first()
                .hasNoErrors()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                // TODO: Add score assertion in subsequent measure scoring refactoring PR
                .up()
                .up()
                .up()
                .up()
                .reportBundles()
                .logReportBundlesJson()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                // This is a population/summary report so we should have a single bundle containing
                // all MeasureReports
                .hasBundleCount(1)
                .hasMeasureReportCount(7)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .logReportJson()
                .hasReportType("Summary")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasReportType("Summary")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                .hasScore("0.5")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(6)
                .up()
                .hasScore("0.375")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .hasMeasureReportStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Patient/female-1988-2")
                .hasContainedOperationOutcomeMsg("Invalid Interval")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATIONEXCLUSION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.MEASUREOBSERVATION)
                .hasCount(8); // removed exclusions
    }

    @Test
    void MultiMeasure_EightMeasures_SubjectEvalType_AllSubjects() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .evaluate();

        when.then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .defs()
                .hasCount(70)
                .first()
                .hasNoErrors()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(7)
                .up()
                // TODO: Add score assertion in subsequent measure scoring refactoring PR
                .up()
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                // This is a subject/individual report so we should have one bundle per subject
                // so 10 bundles
                .hasBundleCount(10)
                .hasMeasureReportCount(70)
                .hasMeasureReportCountPerUrl(10, "http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasMeasureReportCountPerUrl(10, "http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(10, "http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(10, "http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(10, "http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(10, "http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(
                        10, "http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .getFirstMeasureReport()
                .hasReportType("Individual")
                .up()
                .measureReport(
                        "http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup",
                        "Patient/female-1988-2")
                .hasMeasureReportStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Patient/female-1988-2")
                .hasContainedOperationOutcomeMsg("Invalid Interval");
    }

    @Test
    void MultiMeasure_EightMeasures_SubjectEvalType_GroupPatients() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Group/group-patients-1")
                .evaluate();

        when.then()
                // This is a subject/individual report so we should have one bundle per subject
                // the group resolves to 8 individual patient subjects, so 8 bundles
                .hasBundleCount(8)
                .hasMeasureReportCount(56)
                .hasMeasureReportCountPerUrl(8, "http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasMeasureReportCountPerUrl(8, "http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(8, "http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(8, "http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(8, "http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(8, "http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(
                        8, "http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .getFirstMeasureReport()
                .hasReportType("Individual");
    }

    @Test
    void MultiMeasure_EightMeasures_SubjectEvalType_GroupPractitioner() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Group/group-practitioners-1")
                .evaluate();

        when.then()
                // This is a subject/individual report so we should have one bundle per subject
                // so the group references a single practitioner which references a single patient,
                // so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(7)
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(
                        1, "http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .getFirstMeasureReport()
                .hasReportType("Individual");
    }

    @Test
    void MultiMeasure_EightMeasures_SubjectEvalType_Practitioner() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Practitioner/tester")
                .evaluate();

        when.then()
                // This is a subject/individual report so we should have one bundle per subject
                // this practitioner resolves to only one patients, so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(7)
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .hasMeasureReportCountPerUrl(1, "http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .hasMeasureReportCountPerUrl(
                        1, "http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .getFirstMeasureReport()
                .hasReportType("Individual");
    }

    @Test
    void MultiMeasure_EightMeasures_Patient() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988")
                .evaluate();

        when.then()
                // This is a subject/individual report so we should have one bundle per subject
                // but we passed only one subject, so only one bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(7)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .up()
                .hasScore("0.5")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(2)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(2)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .hasReportType("Individual")
                .hasSubjectReference("Patient/female-1988")
                .hasEvaluatedResourceCount(3)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATION)
                .hasCount(2)
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATIONEXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.MEASUREOBSERVATION)
                .hasCount(2)
                .up()
                .up()
                .up();
    }

    @Test
    void MultiMeasure_EightMeasures_SubjectList() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject-list")
                .subject("Patient/female-1988")
                .evaluate();

        when.then()
                // This is a subject-list report so we should have one bundle per subject
                // but we have only one subject, so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(7)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("1.0")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("1.0")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("1.0")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1)
                .hasSubjectResults()
                .up()
                .hasScore("0.5")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasSubjectResults()
                .hasCount(2)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasSubjectResults()
                .hasCount(1)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .hasReportType("Subject List")
                .hasSubjectReference("Patient/female-1988")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATION)
                .hasCount(2)
                .hasSubjectResults()
                .up()
                .population(MeasurePopulationType.MEASUREPOPULATIONEXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.MEASUREOBSERVATION)
                .hasCount(2)
                .up()
                .up()
                .up();
    }

    @Test
    void MultiMeasure_EightMeasures_Practitioner() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .evaluate();

        when.then()
                // This is a population report so we should have one bundle per subject
                // so 1 bundle?
                .hasBundleCount(1)
                .hasMeasureReportCount(1)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .hasSubjectReference("Practitioner/tester")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.DENOMINATOREXCEPTION)
                .hasCount(1)
                .up()
                .population(MeasurePopulationType.NUMERATOREXCLUSION)
                .hasCount(0)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(0);
    }

    @Test
    void MultiMeasure_EightMeasures_ReporterPractitioner() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .reporter("Practitioner/empty")
                .evaluate();

        when.then()
                // This is a population/summary report so we should have one bundle per subject
                // so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(1)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .hasSubjectReference("Practitioner/tester")
                .hasReporter("Practitioner/empty");
    }

    @Test
    void MultiMeasure_EightMeasures_ReporterPractitionerRole() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .reporter("PractitionerRole/test")
                .evaluate();

        when.then()
                // This is a population/summary report so we should have one bundle per subject
                // so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(1)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .hasSubjectReference("Practitioner/tester")
                .hasReporter("PractitionerRole/test");
    }

    @Test
    void MultiMeasure_EightMeasures_ReporterLocation() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .reporter("Location/office")
                .evaluate();

        when.then()
                // This is a population/summary report so we should have one bundle per subject
                // so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(1)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .hasSubjectReference("Practitioner/tester")
                .hasReporter("Location/office")
                .hasPeriodStart(Date.from(LocalDate.of(2024, Month.JANUARY, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()))
                .hasPeriodEnd(Date.from(LocalDate.of(2024, Month.DECEMBER, 31)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()));
    }

    @Test
    void MultiMeasure_EightMeasures_ReporterOrganization() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .reporter("Organization/payer")
                .evaluate();

        when.then()
                // This is a population/summary report so we should have one bundle per subject
                // so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(1)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .hasSubjectReference("Practitioner/tester")
                .hasReporter("Organization/payer");
    }

    @Test
    void MultiMeasure_EightMeasures_ReporterJustId() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .reporter("payer")
                .evaluate();

        assertThrows(IllegalArgumentException.class, when::then);
    }

    @Test
    void MultiMeasure_EightMeasures_ReporterNull() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .reporter(null)
                .evaluate();

        when.then()
                // This is a population/summary report so we should have one bundle per subject
                // so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(1)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .hasSubjectReference("Practitioner/tester")
                .hasReporter(null);
    }

    @Test
    void MultiMeasure_EightMeasures_ReporterNotAcceptedResource() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Practitioner/tester")
                .reporter("Patient/male-2022")
                .evaluate();

        assertThrows(IllegalArgumentException.class, when::then);
    }

    // This test is effectively a sanity test to ensure that Organization subjects
    // do not error out as they did previous to the new feature that enables to this feature./
    // For more complex scenarios please see R4RepositorySubjectProviderTest.
    @Test
    void MultiMeasure_EightMeasures_SubjectOrganization() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .subject("Organization/organization-linked-by-managingOrganization")
                .evaluate();

        when.then()
                // This is a population/summary report so we should have one bundle per subject
                // so 1 bundle
                .hasBundleCount(1)
                .hasMeasureReportCount(1)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .hasSubjectReference("Organization/organization-linked-by-managingOrganization");
    }

    /**
     * Test designed to show how error messages are captured in a Measure Report, when encountered.
     * The associated Encounter has an invalid period that prevents evaluation and will populate an
     * empty MeasureReport with contained OperationOutcome that references PatientId and error message.
     * It also changes MeasureReport.status to Error.
     */
    @Test
    void MultiMeasure_CQLFailure() {
        var when = GIVEN_REPO
                .when()
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .subject("Patient/female-1988-2")
                .evaluate()
                .then();

        when.getFirstMeasureReport()
                .hasMeasureReportStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Patient/female-1988-2");
    }

    /**
     * CDO-789: multi-component stratifier where two components resolve to the same scalar value
     * must not collapse those components in the MeasureReport stratum, even when reached via the
     * multi-measure orchestration path.
     */
    @Test
    void MultiMeasure_MultiComponentStratifier_SameValueAcrossComponents_KeepsAllComponents() {
        var stratifier = GIVEN_STRATIFIER_REPO
                .when()
                .measureId("CohortBooleanStratSameValueComponents")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate()
                .then()
                .measureReport("http://example.com/Measure/CohortBooleanStratSameValueComponents")
                .firstGroup()
                .firstStratifier()
                .value();

        assertEquals(2, stratifier.getStratum().size());
        for (var stratum : stratifier.getStratum()) {
            assertEquals(
                    3,
                    stratum.getComponent().size(),
                    "expected 3 components in stratum but got "
                            + stratum.getComponent().size());
        }
    }

    @Test
    void MultiMeasure_ThrowsErrorWithDuplicatePopulationIds() {
        var givenInvalidRepo = MultiMeasure.given().repositoryFor("InvalidMeasure");

        var when = givenInvalidRepo
                .when()
                .measureId("DuplicatePopulationIds")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        var e = assertThrows(InvalidRequestException.class, when::then);
        assertTrue(e.getMessage().contains("Duplicate population ID"));
        assertTrue(e.getMessage().contains("initial-population"));
    }

    @Test
    void MultiMeasure_NoMeasureReferencesSupplied_throws() {
        var when = GIVEN_REPO
                .when()
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        var e = assertThrows(MeasureLookupException.class, when::then);
        assertEquals("At least one of measureId, measureIdentifier, or measureUrl must be supplied", e.getMessage());
    }
}
