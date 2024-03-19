package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.exception.InvalidInterval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class R4CareGapsTest {
    private final Repository myRepository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(this.getClass())
                    + "/org/opencds/cqf/fhir/cr/measure/r4/BreastCancerScreeningFhir"));

    public void verifyCareGapsPropertiesAreReadable() {
        assertNotNull(myRepository.read(Organization.class, new IdType("alphora")));
        assertNotNull(myRepository.read(Organization.class, new IdType("alphora-author")));
    }

    @Test
    public void exm125_careGaps_closedGap() {
        verifyCareGapsPropertiesAreReadable();
        CareGaps.given()
                .repository(myRepository)
                .when()
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
    public void exm125_careGaps_openGap() {
        verifyCareGapsPropertiesAreReadable();
        CareGaps.given()
                .repository(myRepository)
                .when()
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
    public void exm125_careGaps_NA() {
        verifyCareGapsPropertiesAreReadable();
        CareGaps.given()
                .repository(myRepository)
                .when()
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
    public void exm125_careGaps_group() {
        verifyCareGapsPropertiesAreReadable();
        CareGaps.given()
                .repository(myRepository)
                .when()
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
    public void exm125_careGaps_twoMeasuresById() {
        verifyCareGapsPropertiesAreReadable();
        CareGaps.given()
                .repository(myRepository)
                .when()
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
    public void exm125_careGaps_twoMeasuresByUrl() {
        verifyCareGapsPropertiesAreReadable();
        CareGaps.given()
                .repository(myRepository)
                .when()
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
    public void exm125_careGaps_twoMeasuresByUrlAndId() {
        verifyCareGapsPropertiesAreReadable();
        CareGaps.given()
                .repository(myRepository)
                .when()
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
    public void exm125_careGaps_error_wrongSubjectParam() {
        verifyCareGapsPropertiesAreReadable();
        assertThrows(ResourceNotFoundException.class, () -> {
            CareGaps.given()
                    .repository(myRepository)
                    .when()
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
    public void exm125_careGaps_error_wrongMeasureParam() {
        verifyCareGapsPropertiesAreReadable();
        assertThrows(ResourceNotFoundException.class, () -> {
            CareGaps.given()
                    .repository(myRepository)
                    .when()
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
    public void exm125_careGaps_error_wrongPeriodParam() {
        verifyCareGapsPropertiesAreReadable();
        assertThrows(InvalidInterval.class, () -> {
            CareGaps.given()
                    .repository(myRepository)
                    .when()
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
    public void exm125_careGaps_error_wrongsStatusParam() {
        verifyCareGapsPropertiesAreReadable();
        assertThrows(RuntimeException.class, () -> {
            CareGaps.given()
                    .repository(myRepository)
                    .when()
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
    public void exm125_careGaps_error_noStatusParam() {
        verifyCareGapsPropertiesAreReadable();
        assertThrows(RuntimeException.class, () -> {
            CareGaps.given()
                    .repository(myRepository)
                    .when()
                    .subject("Patient/numer-EXM125")
                    .periodStart("2019-01-01")
                    .periodEnd("2019-12-31")
                    .measureIds("BreastCancerScreeningFHIR")
                    .getCareGapsReport()
                    .then();
        });
    }
}
