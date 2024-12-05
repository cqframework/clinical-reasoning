package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.exception.InvalidInterval;
import org.opencds.cqf.fhir.cr.measure.r4.CareGaps.Given;

class R4CareGapsTest {
    private static final Given given = CareGaps.given().repositoryFor("BreastCancerScreeningFHIR");
    private static final Given GIVEN_REPO = CareGaps.given().repositoryFor("MinimalMeasureEvaluation");

    @Test
    void exm125_careGaps_closedGap() {
        given.when()
                .subject("Patient/numer-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .measureId("BreastCancerScreeningFHIR")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .measureReportEvaluatedResourcesFound()
                .detectedIssue()
                .hasCareGapStatus("closed-gap")
                .hasPatientReference("Patient/numer-EXM125")
                .hasMeasureReportEvidence()
                .up()
                .composition()
                .hasSubjectReference("Patient/numer-EXM125")
                .hasAuthor("Organization/alphora-author")
                .sectionCount(1)
                .up()
                .organization()
                .orgResourceMatches("Organization/alphora-author")
                .up()
                .measureReport()
                .measureReportMatches("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureReportSubjectMatches("Patient/numer-EXM125")
                .measureReportTypeIndividual();
    }

    @Test
    void exm125_careGaps_openGap() {
        given.when()
                .subject("Patient/denom-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .measureId("BreastCancerScreeningFHIR")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .measureReportEvaluatedResourcesFound()
                .detectedIssue()
                .hasCareGapStatus("open-gap")
                .hasPatientReference("Patient/denom-EXM125")
                .hasMeasureReportEvidence()
                .up()
                .composition()
                .hasSubjectReference("Patient/denom-EXM125")
                .hasAuthor("Organization/alphora-author")
                .sectionCount(1)
                .up()
                .organization()
                .orgResourceMatches("Organization/alphora-author")
                .up()
                .measureReport()
                .measureReportMatches("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureReportSubjectMatches("Patient/denom-EXM125")
                .measureReportTypeIndividual();
    }

    @Test
    void exm125_careGaps_NA() {
        // validates initial-population=false returns 'not-applicable' status
        given.when()
                .subject("Patient/neg-denom-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("not-applicable")
                .status("closed-gap")
                .status("open-gap")
                .measureId("BreastCancerScreeningFHIR")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .measureReportEvaluatedResourcesFound()
                .detectedIssue()
                .hasCareGapStatus("not-applicable")
                .hasPatientReference("Patient/neg-denom-EXM125")
                .hasMeasureReportEvidence()
                .up()
                .composition()
                .hasSubjectReference("Patient/neg-denom-EXM125")
                .hasAuthor("Organization/alphora-author")
                .sectionCount(1)
                .up()
                .organization()
                .orgResourceMatches("Organization/alphora-author")
                .up()
                .measureReport()
                .measureReportMatches("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureReportSubjectMatches("Patient/neg-denom-EXM125")
                .measureReportTypeIndividual();
    }

    @Test
    void exm125_careGaps_group() {
        given.when()
                .subject("Group/exm125-group")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .measureId("BreastCancerScreeningFHIR")
                .getCareGapsReport()
                .then()
                .hasBundleCount(2)
                .parameter("denom-EXM125")
                .measureReportEvaluatedResourcesFound()
                .detectedIssue()
                .hasCareGapStatus("open-gap")
                .hasPatientReference("Patient/denom-EXM125")
                .hasMeasureReportEvidence()
                .up()
                .composition()
                .hasSubjectReference("Patient/denom-EXM125")
                .hasAuthor("Organization/alphora-author")
                .sectionCount(1)
                .up()
                .organization()
                .orgResourceMatches("Organization/alphora-author")
                .up()
                .measureReport()
                .measureReportMatches("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureReportSubjectMatches("Patient/denom-EXM125")
                .measureReportTypeIndividual()
                .up()
                .up()
                .parameter("numer-EXM125")
                .detectedIssue()
                .hasCareGapStatus("closed-gap")
                .hasPatientReference("Patient/numer-EXM125")
                .hasMeasureReportEvidence()
                .up()
                .composition()
                .hasSubjectReference("Patient/numer-EXM125")
                .hasAuthor("Organization/alphora-author")
                .sectionCount(1)
                .up()
                .organization()
                .orgResourceMatches("Organization/alphora-author")
                .up()
                .measureReport()
                .measureReportMatches("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureReportSubjectMatches("Patient/numer-EXM125")
                .measureReportTypeIndividual();
    }

    @Test
    void exm125_careGaps_twoMeasuresById() {
        given.when()
                .subject("Patient/numer-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .measureId("BreastCancerScreeningFHIR")
                .measureId("measure-EXM108-8.3.000")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .measureReportCount(2)
                .detectedIssueCount(2);
    }

    @Test
    void exm125_careGaps_twoMeasuresByUrl() {
        given.when()
                .subject("Patient/numer-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .measureUrl("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureUrl("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108|8.3.000")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .measureReportCount(2)
                .detectedIssueCount(2);
    }

    @Test
    void exm125_careGaps_twoMeasuresByUrlAndId() {
        given.when()
                .subject("Patient/numer-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .measureId("BreastCancerScreeningFHIR")
                .measureUrl("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108|8.3.000")
                .notDocument(false)
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .measureReportCount(2)
                .detectedIssueCount(2);
    }

    @Test
    void exm125_careGaps_twoMeasuresByUrlAndId_NonDocumentMode() {
        given.when()
                .subject("Patient/numer-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .measureId("BreastCancerScreeningFHIR")
                .measureUrl("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108|8.3.000")
                .notDocument(true)
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .bundleEntryCount(2) // only DetectedIssues should be within Bundle
                .detectedIssueCount(2);
    }

    @Test
    void exm125_careGaps_error_wrongSubjectParam() {
        try {
            given.when()
                    .subject("Patient/numer-EXM124")
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .status("closed-gap")
                    .status("open-gap")
                    .measureId("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
            fail("this should fail with no resource found");
        } catch (ResourceNotFoundException e) {
            Assertions.assertTrue(e.getMessage().contains("Resource Patient/numer-EXM124 is not known"));
        }
    }

    @Test
    void exm125_careGaps_error_wrongMeasureParam() {
        assertThrows(ResourceNotFoundException.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .status("closed-gap")
                    .status("open-gap")
                    .measureId("BreastCancerScreeningFHI")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_wrongPeriodParam() {
        assertThrows(InvalidInterval.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart(
                            LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .status("closed-gap")
                    .status("open-gap")
                    .measureId("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_wrongsStatusParam() {
        try {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .status("closed-ga")
                    .status("open-gap")
                    .measureId("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
            fail("this should fail with no resource found");
        } catch (InvalidRequestException e) {
            Assertions.assertTrue(
                    e.getMessage()
                            .contains(
                                    "CareGap status parameter: closed-ga, is not an accepted value for Measure: [BreastCancerScreeningFHIR]"));
        }
    }

    @Test
    void exm125_careGaps_error_noStatusParam() {
        assertThrows(RuntimeException.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_byIdentifier() {
        given.when()
                .measureIdentifier("80366f35-e0a0-4ba7-a746-ad5760b79e01")
                .measureId(null)
                .subject("Patient/numer-EXM125")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .status("closed-gap")
                .status("open-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .detectedIssue()
                .hasCareGapStatus("closed-gap")
                .hasPatientReference("Patient/numer-EXM125")
                .hasMeasureReportEvidence()
                .up()
                .composition()
                .hasSubjectReference("Patient/numer-EXM125")
                .hasAuthor("Organization/alphora-author")
                .sectionCount(1)
                .up()
                .organization()
                .orgResourceMatches("Organization/alphora-author")
                .up()
                .measureReport()
                .measureReportMatches("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureReportSubjectMatches("Patient/numer-EXM125")
                .measureReportTypeIndividual();
    }

    @Test
    void exm125_careGaps_error_invalidPatient() {
        try {
            given.when()
                    .subject("Patient/numer-EXM126") // invalid
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("BreastCancerScreeningFHIR")
                    .status("open-gap")
                    .getCareGapsReport()
                    .then();
            fail("this should fail with no resource found");
        } catch (ResourceNotFoundException e) {
            Assertions.assertTrue(e.getMessage().contains("Resource Patient/numer-EXM126 is not known"));
        }
    }

    @Test
    void exm125_careGaps_error_invalidGroup() {
        try {
            given.when()
                    .subject("Group/numer-EXM126") // invalid
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("BreastCancerScreeningFHIR")
                    .status("closed-gap")
                    .getCareGapsReport()
                    .then();
            fail("this should fail with no resource found");
        } catch (ResourceNotFoundException e) {
            Assertions.assertTrue(e.getMessage().contains("Resource Group/numer-EXM126 is not known"));
        }
    }

    @Test
    void exm125_careGaps_Practitioner() {
        given.when()
                .subject("Practitioner/error")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("BreastCancerScreeningFHIR")
                .status("closed-gap")
                .status("open-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(0); // no results
    }

    // Issue #466 concurrent modification exception when improvementNotation is not populated on measure.
    @Test
    void ProportionBooleanBasisSingleGroup_Subject_noImprovementNotation() {
        GIVEN_REPO
                .when()
                .subject("Patient/female-1988")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .status("closed-gap")
                .status("open-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1);
    }

    // Issue #466 unable to process group level scoring definition
    @Test
    void ProportionBooleanBasisSingleGroup_Subject_groupScoringDef() {
        GIVEN_REPO
                .when()
                .subject("Patient/female-1988") // invalid
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisSingleGroupGroupScoringDef")
                .status("closed-gap")
                .status("open-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1); // no results
    }

    @Test
    void ProportionBooleanBasisSingleGroup_All_Subjects() {
        GIVEN_REPO
                .when()
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .getCareGapsReport()
                .then()
                .hasBundleCount(9); // All 8 subjects have a bundle
    }

    @Test
    void ProportionBooleanBasisSingleGroup_group_practitioner_type() {
        GIVEN_REPO
                .when()
                .subject("Group/group-practitioners-1")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1); // 1 Subject has matched generalPractitioner
    }

    @Test
    void MinimalRatioBooleanBasisSingleGroup_practitioner() {
        GIVEN_REPO
                .when()
                .subject("Practitioner/tester")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1); // 1 Subject has matched generalPractitioner
    }

    @Test
    void Cohort_ScoringTypeError() {
        try {
            GIVEN_REPO
                    .when()
                    .subject("Practitioner/tester")
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("MinimalCohortResourceBasisSingleGroup")
                    .status("closed-gap")
                    .status("open-gap")
                    .status("not-applicable")
                    .getCareGapsReport()
                    .then()
                    .hasBundleCount(1);

            fail("method should error");
        } catch (InvalidRequestException e) {
            Assertions.assertTrue(e.getMessage()
                    .contains("MeasureScoring type: Cohort, is not an accepted Type for care-gaps service"));
        }
    }

    @Test
    void ContinuousVariable_ScoringTypeError() {
        try {
            GIVEN_REPO
                    .when()
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                    .status("closed-gap")
                    .status("open-gap")
                    .status("not-applicable")
                    .getCareGapsReport()
                    .then()
                    .hasBundleCount(1);

            fail("method should error");
        } catch (InvalidRequestException e) {
            Assertions.assertTrue(e.getMessage()
                    .contains(
                            "MeasureScoring type: Continuous Variable, is not an accepted Type for care-gaps service"));
        }
    }

    // MinimalProportionResourceBasisSingleGroup
    @Test
    void MinimalProportionResourceBasisSingleGroup_Subject() {
        try {
            GIVEN_REPO
                    .when()
                    .subject("Patient/female-1988")
                    .periodStart(
                            LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2024, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("MinimalProportionResourceBasisSingleGroup")
                    .status("closed-gap")
                    .status("open-gap")
                    .getCareGapsReport()
                    .then()
                    .hasBundleCount(1);
            fail("resource based measures should fail");
        } catch (InvalidRequestException e) {
            Assertions.assertTrue(
                    e.getMessage()
                            .contains(
                                    "CareGaps can't process Measure: MinimalProportionResourceBasisSingleGroup, it is not Boolean basis"));
        }
    }

    @Test
    void MinimalProportionBooleanBasisMultiGroup() {
        GIVEN_REPO
                .when()
                .subject("Patient/female-1988")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisMultiGroup")
                .status("closed-gap")
                .status("open-gap")
                .status("not-applicable")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .detectedIssueCount(2); // 1 Detected issue per groupId
    }

    @Test
    void MinimalProportionBooleanBasisMultiGroupDifferentStatus() {
        GIVEN_REPO
                .when()
                .subject("Patient/female-1988")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisMultiGroupDifferentStatus")
                .status("closed-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .detectedIssueCount(
                        1); // 2 Detected issue per groupId, 1 open-gap, 1 closed-gap. Only "closed-gap" should show
    }

    @Test
    void MinimalProportionBooleanBasisMultiGroup_NoId() {
        try {
            GIVEN_REPO
                    .when()
                    .subject("Patient/female-1988")
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("MinimalProportionBooleanBasisMultiGroupNoGroupId")
                    .status("closed-gap")
                    .status("open-gap")
                    .status("not-applicable")
                    .getCareGapsReport()
                    .then()
                    .hasBundleCount(1)
                    .firstParameter()
                    .detectedIssueCount(2); // 1 Detected issue per groupId
            fail("this should fail without a groupId");
        } catch (InvalidRequestException e) {
            Assertions.assertTrue(
                    e.getMessage()
                            .contains(
                                    "Multi-rate Measure resources require unique 'id' for GroupComponents to be populated for Measure: http://example.com/Measure/MinimalProportionBooleanBasisMultiGroupNoGroupId"));
        }
    }

    // 'prospective gap' test relies on date of report to decide if 'prospective' or 'open' gap. This will fail EOY
    // 2025.
    @Test
    void MinimalProportionBooleanBasisSingleGroupWithDOC_DocumentMode_NO_date_of_compliance() {
        GIVEN_REPO
                .when()
                .subject("Patient/male-2022")
                .periodStart(LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2024, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisSingleGroupWithDOC-NO-date-of-compliance")
                .notDocument(false)
                .status("prospective-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(0);
    }

    @Test
    void MinimalProportionBooleanBasisSingleGroupWithDOC_DocumentMode_YES_date_of_compliance() {
        try {
            GIVEN_REPO
                    .when()
                    .subject("Patient/male-2022")
                    .periodStart(
                            LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2024, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("MinimalProportionBooleanBasisSingleGroupWithDOC-YES-date-of-compliance")
                    .notDocument(false)
                    .status("prospective-gap")
                    .getCareGapsReport()
                    .then();
            fail("this should fail with a date of compliance expression");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "group expression criteria results for expression: [date of compliance] and scoring: [PROPORTION] must match the same type: [org.opencds.cqf.cql.engine.runtime.Interval] as population basis: [boolean] for Measure: http://example.com/Measure/MinimalProportionBooleanBasisSingleGroupWithDOC-YES-date-of-compliance",
                    exception.getMessage());
        }
    }

    @Test
    void MinimalProportionBooleanBasisSingleGroupWithDOC_NonDocumentMode_NO_date_of_compliance() {
        GIVEN_REPO
                .when()
                .subject("Patient/male-2022")
                .periodStart(LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2024, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisSingleGroupWithDOC-NO-date-of-compliance")
                .notDocument(true)
                .status("prospective-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(0);
    }

    @Test
    void MinimalProportionBooleanBasisSingleGroupWithDOC_NonDocumentMode_YES_date_of_compliance() {
        try {
            GIVEN_REPO
                    .when()
                    .subject("Patient/male-2022")
                    .periodStart(
                            LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2024, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId("MinimalProportionBooleanBasisSingleGroupWithDOC-YES-date-of-compliance")
                    .notDocument(true)
                    .status("prospective-gap")
                    .getCareGapsReport()
                    .then();
            fail("this should fail with a date of compliance expression");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "group expression criteria results for expression: [date of compliance] and scoring: [PROPORTION] must match the same type: [org.opencds.cqf.cql.engine.runtime.Interval] as population basis: [boolean] for Measure: http://example.com/Measure/MinimalProportionBooleanBasisSingleGroupWithDOC-YES-date-of-compliance",
                    exception.getMessage());
        }
    }

    @Test
    void MinimalProportionBooleanBasisMultiGroupGroupImpNotation() {
        // 2 Detected issues,1 per groupId, one 'closed-gap' the other 'open-gap'
        // make sure each group matches the correct status and has extension reference
        GIVEN_REPO
                .when()
                .subject("Patient/female-1988")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisMultiGroupGroupImpNotation")
                .measureIdentifier(null)
                .measureUrl(null)
                .status("closed-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .detectedIssueCount(1)
                .detectedIssue()
                .hasGroupIdReportExtension("group-2");

        GIVEN_REPO
                .when()
                .subject("Patient/female-1988")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionBooleanBasisMultiGroupGroupImpNotation")
                .status("open-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .detectedIssueCount(1)
                .detectedIssue()
                .hasGroupIdReportExtension("group-1");
    }

    @Test
    void noMeasureSpecified() {
        try {
            GIVEN_REPO
                    .when()
                    .subject("Patient/female-1988")
                    .periodStart(
                            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                    .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31)
                            .atStartOfDay()
                            .atZone(ZoneId.systemDefault()))
                    .measureId(null)
                    .measureIdentifier(null)
                    .measureUrl(null)
                    .status("closed-gap")
                    .getCareGapsReport()
                    .then()
                    .hasBundleCount(1)
                    .firstParameter()
                    .detectedIssueCount(1)
                    .detectedIssue()
                    .hasGroupIdReportExtension("group-2");
            fail();
        } catch (InvalidRequestException e) {
            assertTrue(e.getMessage().contains("no measure resolving parameter was specified"));
        }
    }
    // MinimalProportionDenominatorExclusion
    @Test
    void InInitalPopulationAndDenominatorExclusion() {
        // when subject is in initial population
        // not in Denominator
        // they should produce 'closed-gap'
        GIVEN_REPO
                .when()
                .subject("Patient/female-1988")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay().atZone(ZoneId.systemDefault()))
                .measureId("MinimalProportionDenominatorExclusion")
                .measureIdentifier(null)
                .measureUrl(null)
                .status("closed-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .detectedIssueCount(1)
                .detectedIssue()
                .hasCareGapStatus("closed-gap");
    }
}
