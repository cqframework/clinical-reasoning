package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
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
        assertThrows(IllegalArgumentException.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM124")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .statuses("closed-gap")
                    .statuses("open-gap")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
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
        assertThrows(RuntimeException.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .statuses("closed-ga")
                    .statuses("open-gap")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
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
    void exm125_careGaps_error_notSupported() {
        assertThrows(NotImplementedOperationException.class, () -> {
            given.when()
                    // .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_notSupportedIdentifier() {
        assertThrows(NotImplementedOperationException.class, () -> {
            given.when()
                    .measureIdentifiers("cms125")
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_notSupportedProgram() {
        assertThrows(NotImplementedOperationException.class, () -> {
            given.when()
                    .programs("program")
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_notSupportedPractitioner() {
        assertThrows(NotImplementedOperationException.class, () -> {
            given.when()
                    .practitioner("Practitioner/error")
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_notSupportedTopic() {
        assertThrows(NotImplementedOperationException.class, () -> {
            given.when()
                    .topics("Practitioner/error")
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_notSupportedOrg() {
        assertThrows(NotImplementedOperationException.class, () -> {
            given.when()
                    .organization("Organization/error")
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_invalidPatient() {
        assertThrows(IllegalArgumentException.class, () -> {
            given.when()
                    .subject("Patient/numer-EXM126") // invali
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_invalidGroup() {
        assertThrows(IllegalArgumentException.class, () -> {
            given.when()
                    .subject("Group/numer-EXM126") // invalid
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }

    @Test
    void exm125_careGaps_error_notPersonOrGroup() {
        given.when()
                .subject("Practitioner/error") // invalid
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
                .subject("Patient/female-1988") // invalid
                .periodStart("2019-01-01")
                .periodEnd("2019-12-31")
                .measureIds("MinimalProportionBooleanBasisSingleGroup")
                .statuses("closed-gap")
                .statuses("open-gap")
                .getCareGapsReport()
                .then()
                .hasBundleCount(1); // no results
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
}
