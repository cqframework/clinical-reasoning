package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.nio.file.Path;
import java.util.Arrays;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.TestQuestionnaireResponse;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings({"squid:S2699", "UnstableApiUsage"})
class QuestionnaireProcessorTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();
    private final IRepository repositoryR4 =
            new IgRepository(fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
    private final IRepository repositoryR5 =
            new IgRepository(fhirContextR5, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r5"));

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
    void populateR4() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(newParameters(fhirContextR4, newStringPart(fhirContextR4, "ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .hasItems(35)
                .itemHasAnswerValue("1.1", new org.hl7.fhir.r4.model.StringType("Acme Clinic"));

        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "definition"))
                .subjectId("OPA-Patient1")
                .parameters(newParameters(fhirContextR4, newStringPart(fhirContextR4, "ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .hasItems(2)
                .hasNoErrors()
                .itemHasAnswerValue("1.1", new org.hl7.fhir.r4.model.StringType("Acme Clinic"));
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
                .hasItems(35)
                .hasErrors();
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
        var when = given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "null"));
        assertThrows(ResourceNotFoundException.class, () -> when.thenPopulate(true));
    }

    @Test
    void populateNoSubjectThrowsException() {
        var when = given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"));
        assertThrows(IllegalArgumentException.class, () -> when.thenPopulate(false));
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
                .hasNoErrors()
                .itemHasAnswer("family-name")
                .itemHasAnswer("provider-name");
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
    void testItemContextPopulationWithoutDefinition() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "observation"))
                .subjectId("Patient1")
                .thenPopulate(true)
                .hasItems(2)
                .itemHasAnswerValue("1", new org.hl7.fhir.r4.model.Quantity(50));
    }

    @Test
    void testIntegration() {
        var patientId = "Patient1";
        var questionnaire = given().repository(repositoryR4)
                .when()
                .profileId(Ids.newId(fhirContextR4, "StructureDefinition", "LaunchContexts"))
                .thenGenerate()
                .hasItems(2)
                .questionnaire
                .get();
        var questionnaireResponse = (QuestionnaireResponse) given().repository(repositoryR4)
                .when()
                .questionnaire(questionnaire)
                .subjectId(patientId)
                .context(Arrays.asList(
                        (IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "patient"),
                                newPart(fhirContextR4, "Reference", "content", "Patient/" + patientId)),
                        (IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "encounter"),
                                newPart(fhirContextR4, "Reference", "content", "Encounter/Encounter1"))))
                .thenPopulate(true)
                .hasNoErrors()
                .hasItems(2)
                .itemHasAnswer("1.1")
                .itemHasAuthorExt("1.1")
                .questionnaireResponse
                .get();
        assertEquals("Patient/" + patientId, questionnaireResponse.getSubject().getReference());
        assertNotNull(questionnaireResponse.getAuthored());
        var bundle = TestQuestionnaireResponse.given()
                .repository(repositoryR4)
                .when()
                .questionnaireResponse(questionnaireResponse)
                .questionnaire(questionnaire)
                .extract()
                .hasEntry(1)
                .getBundle();
        assertInstanceOf(Observation.class, BundleHelper.getEntryResourceFirstRep(bundle));
        var observation = (Observation) BundleHelper.getEntryResourceFirstRep(bundle);
        assertNotNull(observation);
        assertEquals("Patient/" + patientId, observation.getSubject().getReference());
        assertTrue(observation.hasCategory());
        assertTrue(observation.hasCode());
        assertTrue(observation.hasEffective());
        assertTrue(observation.hasValueQuantity());
        assertEquals("cm", observation.getValueQuantity().getUnit());
    }

    @Test
    void testGMTPQuestionnaire() {
        given().repositoryFor(fhirContextR4, "r4/gmtp-questionnaire")
                .when()
                .questionnaireUrl(canonicalTypeForVersion(
                        FhirVersionEnum.R4, "http://hl7.org/fhir/us/cql/Questionnaire/GMTPQuestionnaire"))
                .subjectId("USCorePatient-GMTP-1")
                .thenPopulate(true)
                .hasNoErrors()
                .itemHasAnswerValue("billing-provider-question", "No")
                .itemHasAnswerValue("billing-provider-info|name", "Oak Street Billing Provider")
                .itemHasAnswerValue("billing-provider-info|phone-number", "+1 (123) 4567890")
                .itemHasAnswerValue("billing-provider-info|npi", "1144221849")
                .itemHasAnswerValue("billing-provider-info|fax", "+1 (123) 4567890")
                .itemHasAnswer("test-requested|service-date")
                .itemHasAnswerValue("test-requested|test-id", "405825005, Z13.89, 81479")
                .itemHasAnswerValue(
                        "test-requested|test-name",
                        "Molecular genetic test (procedure),  Encounter for screening for other disorder, Unlisted molecular pathology procedure")
                .itemHasAnswerValue(
                        "test-requested|diagnosis-description",
                        "Type 2 diabetes mellitus with other diabetic arthropathy")
                .itemHasAnswerValue("history|patient-history|diagnosis", "E11.618")
                .itemHasAnswerValue("history|patient-history|age-at-onset", "22")
                .itemHasNoAnswer("history|patient-history|abatement")
                .itemHasNoAnswer("history|other-findings");
    }
}
