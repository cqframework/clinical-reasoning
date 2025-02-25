package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.nio.file.Paths;
import java.util.Arrays;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.TestQuestionnaireResponse;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class QuestionnaireProcessorTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();
    private final Repository repositoryR4 =
            new IgRepository(fhirContextR4, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
    private final Repository repositoryR5 =
            new IgRepository(fhirContextR5, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r5"));

    @Test
    void populateR4() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(newParameters(fhirContextR4, newStringPart(fhirContextR4, "ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .isEqualsToExpected(org.hl7.fhir.r4.model.QuestionnaireResponse.class);

        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "definition"))
                .subjectId("OPA-Patient1")
                .parameters(newParameters(fhirContextR4, newStringPart(fhirContextR4, "ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .hasItems(2)
                .itemHasAnswerValue("1.1", new org.hl7.fhir.r4.model.StringType("Acme Clinic"))
                .hasNoErrors();
    }

    @Test
    void populateR5() {
        // R5 CQL evaluation is failing with model errors from the engine
        // Using this to test building the request in the processor
        given().repository(repositoryR5)
                .when()
                .questionnaireId(Ids.newId(fhirContextR5, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(newParameters(fhirContextR5, newStringPart(fhirContextR5, "ClaimId", "OPA-Claim1")))
                .thenPopulate(false);
    }

    @Test
    void populateNoLibrary() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(
                        Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"))
                .subjectId("OPA-Patient1")
                .parameters(newParameters(fhirContextR4, newStringPart(fhirContextR4, "ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .isEqualsToExpected(org.hl7.fhir.r4.model.QuestionnaireResponse.class);
    }

    @Test
    void populateHasErrors() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(
                        Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"))
                .subjectId("OPA-Patient1")
                .parameters(newParameters(fhirContextR4, newStringPart(fhirContextR4, "ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .hasErrors();
    }

    @Test
    void populateNoQuestionnaireThrowsException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            given().repository(repositoryR4)
                    .when()
                    .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "null"))
                    .thenPopulate(true);
        });
    }

    @Test
    void populateNoSubjectThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            given().repository(repositoryR4)
                    .when()
                    .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                    .thenPopulate(false);
        });
    }

    @Test
    void populateWithLaunchContextResolvesParametersR4() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(
                        fhirContextR4, "Questionnaire", "questionnaire-sdc-test-fhirpath-prepop-initialexpression"))
                .subjectId("OPA-Patient1")
                .context(Arrays.asList(
                        (IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "user"),
                                newPart(fhirContextR4, "Reference", "content", "Practitioner/OPA-AttendingPhysician1")),
                        (IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "patient"),
                                newPart(fhirContextR4, "Reference", "content", "Patient/OPA-Patient1"))))
                .thenPopulate(true)
                .hasItems(14)
                .itemHasAnswer("family-name")
                .itemHasAnswer("provider-name")
                .hasNoErrors();
    }

    @Test
    @Disabled("R5 CQL evaluation currently fails")
    void populateWithLaunchContextResolvesParametersR5() {
        given().repository(repositoryR5)
                .when()
                .questionnaireId(Ids.newId(
                        fhirContextR5, "Questionnaire", "questionnaire-sdc-test-fhirpath-prepop-initialexpression"))
                .subjectId("OPA-Patient1")
                .context(Arrays.asList(
                        (IBaseBackboneElement) newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "user"),
                                newPart(fhirContextR5, "Reference", "content", "Practitioner/OPA-AttendingPhysician1")),
                        (IBaseBackboneElement) newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "patient"),
                                newPart(
                                        fhirContextR5,
                                        "Reference",
                                        "content",
                                        "Practitioner/OPA-AttendingPhysician1"))))
                .thenPopulate(true)
                .hasItems(14)
                .itemHasAnswer("family-name")
                .itemHasAnswer("provider-name")
                .hasNoErrors();
    }

    @Test
    void questionnairePackageR4() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenPackage()
                .hasEntry(3)
                .firstEntryIsType(org.hl7.fhir.r4.model.Questionnaire.class);
    }

    @Test
    void questionnairePackageR5() {
        given().repository(repositoryR5)
                .when()
                .questionnaireId(Ids.newId(fhirContextR5, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenPackage()
                .hasEntry(3)
                .firstEntryIsType(org.hl7.fhir.r5.model.Questionnaire.class);
    }

    @Test
    void dataRequirementsR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenDataRequirements()
                .hasDataRequirements(30);
    }

    @Test
    void dataRequirementsR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .questionnaireId(Ids.newId(fhirContextR5, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenDataRequirements()
                .hasDataRequirements(30);
    }

    @Test
    void pa_aslp_Populate() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "ASLPA1"))
                .subjectId("positive")
                .parameters(newParameters(
                        fhirContextR4,
                        newStringPart(fhirContextR4, "Service Request Id", "SleepStudy"),
                        newStringPart(fhirContextR4, "Service Request Id", "SleepStudy2"),
                        newStringPart(fhirContextR4, "Coverage Id", "Coverage-positive")))
                .thenPopulate(true)
                .hasItems(10)
                .itemHasAnswer("1")
                .itemHasAuthorExt("1")
                .itemHasAnswer("2")
                .itemHasAuthorExt("2");
    }

    @Test
    void pa_aslp_Package() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "ASLPA1"))
                .isPut(Boolean.TRUE)
                .thenPackage()
                .hasEntry(18)
                .firstEntryIsType(org.hl7.fhir.r4.model.Questionnaire.class);
    }

    @Test
    void processors() {
        var bundle = given().repository(repositoryR4)
                .generateProcessor(new GenerateProcessor(repositoryR4))
                .packageProcessor(new PackageProcessor(repositoryR4))
                .populateProcessor(new PopulateProcessor())
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .isPut(Boolean.FALSE)
                .thenPackage()
                .getBundle();
        assertNotNull(bundle);
    }

    @Test
    void testItemContextPopulationWithoutDefinition() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "observation"))
                .subjectId("Patient1")
                .thenPopulate(true)
                .hasItems(2)
                .itemHasAnswerValue("1", new org.hl7.fhir.r4.model.IntegerType(50));
    }

    @Test
    @Disabled("Currently failing due to an issue in the CHF CQL")
    void testIntegration() {
        var questionnaire = given().repository(repositoryR4)
                .when()
                .profileId(Ids.newId(fhirContextR4, "StructureDefinition", "chf-bodyweight-change"))
                .thenGenerate()
                .questionnaire;
        var questionnaireResponse = given().repository(repositoryR4)
                .when()
                .questionnaire(questionnaire)
                .subjectId("chf-scenario1-patient")
                .context(Arrays.asList(
                        (IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "patient"),
                                newPart(fhirContextR4, "Reference", "content", "Patient/chf-scenario1-patient")),
                        (IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "encounter"),
                                newPart(fhirContextR4, "Reference", "content", "Encounter/chf-scenario1-encounter"))))
                .thenPopulate(true)
                .hasItems(11)
                // .itemHasAnswerValue("1.2.1", "-1.4")
                .itemHasAuthorExt("1.2.1")
                .questionnaireResponse;
        TestQuestionnaireResponse.given()
                .repository(repositoryR4)
                .when()
                .questionnaireResponse(questionnaireResponse)
                .extract()
                .hasEntry(1);
    }
}
