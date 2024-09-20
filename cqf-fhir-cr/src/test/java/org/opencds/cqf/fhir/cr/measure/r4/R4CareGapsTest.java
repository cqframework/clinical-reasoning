package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("closed-gap")
                .statuses("open-gap")
                .measureIds("BreastCancerScreeningFHIR")
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
    void exm125_careGaps_openGap() {
        given.when()
                .subject("Patient/denom-EXM125")
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("closed-gap")
                .statuses("open-gap")
                .measureIds("BreastCancerScreeningFHIR")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
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
        given.when()
                .subject("Patient/neg-denom-EXM125")
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("not-applicable")
                .statuses("closed-gap")
                .statuses("open-gap")
                .measureIds("BreastCancerScreeningFHIR")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("closed-gap")
                .statuses("open-gap")
                .measureIds("BreastCancerScreeningFHIR")
                .getCareGapsReport()
                .then()
                .hasBundleCount(2)
                .parameter("denom-EXM125")
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("closed-gap")
                .statuses("open-gap")
                .statuses("not-applicable")
                .measureIds("BreastCancerScreeningFHIR")
                .measureIds("measure-EXM108-8.3.000")
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("closed-gap")
                .statuses("open-gap")
                .statuses("not-applicable")
                .measureUrls("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR|2.0.003")
                .measureUrls("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108|8.3.000")
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("closed-gap")
                .statuses("open-gap")
                .statuses("not-applicable")
                .measureIds("BreastCancerScreeningFHIR")
                .measureUrls("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108|8.3.000")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1)
                .firstParameter()
                .measureReportCount(2)
                .detectedIssueCount(2);
    }

    @Test
    void exm125_careGaps_error_wrongSubjectParam() {
        try {
            given.when()
                    .subject("Patient/numer-EXM124")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .statuses("closed-gap")
                    .statuses("open-gap")
                    .measureIds("BreastCancerScreeningFHIR")
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
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .statuses("closed-gap")
                    .statuses("open-gap")
                    .measureIds("BreastCancerScreeningFHI")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_wrongPeriodParam() {
        assertThrows(InvalidInterval.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart("2020-01-01")
                    .periodEnd("2019-12-31")
                    .statuses("closed-gap")
                    .statuses("open-gap")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_wrongsStatusParam() {
        try {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .statuses("closed-ga")
                    .statuses("open-gap")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
            fail("this should fail with no resource found");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("CareGap status parameter: closed-ga is not an accepted value"));
        }
    }

    @Test
    void exm125_careGaps_error_noStatusParam() {
        assertThrows(RuntimeException.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_byIdentifier() {
        given.when()
                .measureIdentifiers("80366f35-e0a0-4ba7-a746-ad5760b79e01")
                .subject("Patient/numer-EXM125")
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .statuses("closed-gap")
                .statuses("open-gap")
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
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("BreastCancerScreeningFHIR")
                    .statuses("open-gap")
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
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("BreastCancerScreeningFHIR")
                    .statuses("closed-gap")
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .measureIds("BreastCancerScreeningFHIR")
                .statuses("closed-gap")
                .statuses("open-gap")
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .measureIds("MinimalProportionBooleanBasisSingleGroup")
                .statuses("closed-gap")
                .statuses("open-gap")
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
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .measureIds("MinimalProportionBooleanBasisSingleGroupGroupScoringDef")
                .statuses("closed-gap")
                .statuses("open-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1); // no results
    }

    @Test
    void ProportionBooleanBasisSingleGroup_All_Subjects() {
        GIVEN_REPO
                .when()
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .measureIds("MinimalProportionBooleanBasisSingleGroup")
                .statuses("closed-gap")
                .statuses("open-gap")
                .statuses("not-applicable")
                .getCareGapsReport()
                .then()
                .hasBundleCount(8); // All 8 subjects have a bundle
    }

    @Test
    void ProportionBooleanBasisSingleGroup_group_practitioner_type() {
        GIVEN_REPO
                .when()
                .subject("Group/group-practitioners-1")
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .measureIds("MinimalProportionBooleanBasisSingleGroup")
                .statuses("closed-gap")
                .statuses("open-gap")
                .statuses("not-applicable")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1); // 1 Subject has matched generalPractitioner
    }

    @Test
    void MinimalRatioBooleanBasisSingleGroup_practitioner() {
        GIVEN_REPO
                .when()
                .subject("Practitioner/tester")
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .measureIds("MinimalRatioBooleanBasisSingleGroup")
                .statuses("closed-gap")
                .statuses("open-gap")
                .statuses("not-applicable")
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
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("MinimalCohortResourceBasisSingleGroup")
                    .statuses("closed-gap")
                    .statuses("open-gap")
                    .statuses("not-applicable")
                    .getCareGapsReport()
                    .then()
                    .hasBundleCount(1);

            fail("method should error");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage()
                    .contains("MeasureScoring type: Cohort, is not an accepted Type for care-gaps service"));
        }
    }

    @Test
    void ContinuousVariable_ScoringTypeError() {
        try {
            GIVEN_REPO
                    .when()
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("MinimalContinuousVariableBooleanBasisSingleGroup")
                    .statuses("closed-gap")
                    .statuses("open-gap")
                    .statuses("not-applicable")
                    .getCareGapsReport()
                    .then()
                    .hasBundleCount(1);

            fail("method should error");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage()
                    .contains(
                            "MeasureScoring type: Continuous Variable, is not an accepted Type for care-gaps service"));
        }
    }
}
