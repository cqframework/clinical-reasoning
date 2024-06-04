package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class QuestionnaireProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();
    private final Repository repositoryDstu3 = new IgRepository(
            fhirContextDstu3, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/dstu3"));
    private final Repository repositoryR4 =
            new IgRepository(fhirContextR4, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
    private final Repository repositoryR5 =
            new IgRepository(fhirContextR5, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r5"));

    // @Test
    // void prePopulateDstu3() {
    //     given().repository(repositoryDstu3)
    //             .when()
    //             .questionnaireId(Ids.newId(fhirContextDstu3, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
    //             .subjectId("OPA-Patient1")
    //             .parameters(org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters(
    //                     org.opencds.cqf.fhir.utility.dstu3.Parameters.stringPart("ClaimId", "OPA-Claim1")))
    //             .thenPrepopulate(true)
    //             .isEqualsToExpected(org.hl7.fhir.dstu3.model.Questionnaire.class);
    // }

    // @Test
    // void prePopulateR4() {
    //     given().repository(repositoryR4)
    //             .when()
    //             .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
    //             .subjectId("OPA-Patient1")
    //             .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
    //             .thenPrepopulate(true)
    //             .isEqualsToExpected(org.hl7.fhir.r4.model.Questionnaire.class);
    // }

    // @Test
    // void prePopulateR5() {
    //     // R5 CQL evaluation is failing with model errors from the engine
    //     // Using this to test building the request in the processor
    //     given().repository(repositoryR5)
    //             .when()
    //             .questionnaireId(Ids.newId(fhirContextR5, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
    //             .subjectId("OPA-Patient1")
    //             .parameters(org.opencds.cqf.fhir.utility.r5.Parameters.parameters(
    //                     org.opencds.cqf.fhir.utility.r5.Parameters.stringPart("ClaimId", "OPA-Claim1")))
    //             .thenPrepopulate(false);
    // }

    // @Test
    // void prePopulateNoLibrary() {
    //     given().repository(repositoryR4)
    //             .when()
    //             .questionnaireId(
    //                     Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"))
    //             .subjectId("OPA-Patient1")
    //             .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
    //             .thenPrepopulate(true)
    //             .isEqualsToExpected(org.hl7.fhir.r4.model.Questionnaire.class);
    // }

    // @Test
    // void prePopulateHasErrors() {
    //     given().repository(repositoryR4)
    //             .when()
    //             .questionnaireId(
    //                     Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"))
    //             .subjectId("OPA-Patient1")
    //             .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
    //             .thenPrepopulate(true)
    //             .hasErrors();
    // }

    // @Test
    // void prePopulateNoQuestionnaireThrowsException() {
    //     assertThrows(ResourceNotFoundException.class, () -> {
    //         given().repository(repositoryR4)
    //                 .when()
    //                 .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "null"))
    //                 .thenPrepopulate(true);
    //     });
    // }

    @Test
    void populateDstu3() {
        given().repository(repositoryDstu3)
                .when()
                .questionnaireId(Ids.newId(fhirContextDstu3, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters(
                        org.opencds.cqf.fhir.utility.dstu3.Parameters.stringPart("ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .isEqualsToExpected(org.hl7.fhir.dstu3.model.QuestionnaireResponse.class);
    }

    @Test
    void populateR4() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .thenPopulate(true)
                .isEqualsToExpected(org.hl7.fhir.r4.model.QuestionnaireResponse.class);
    }

    @Test
    void populateR5() {
        // R5 CQL evaluation is failing with model errors from the engine
        // Using this to test building the request in the processor
        given().repository(repositoryR5)
                .when()
                .questionnaireId(Ids.newId(fhirContextR5, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(org.opencds.cqf.fhir.utility.r5.Parameters.parameters(
                        org.opencds.cqf.fhir.utility.r5.Parameters.stringPart("ClaimId", "OPA-Claim1")))
                .thenPopulate(false);
    }

    @Test
    void populateNoLibrary() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(
                        Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
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
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
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
    void questionnairePackageDstu3() {
        given().repository(repositoryDstu3)
                .when()
                .questionnaireId(Ids.newId(fhirContextDstu3, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenPackage()
                .hasEntry(3)
                .firstEntryIsType(org.hl7.fhir.dstu3.model.Questionnaire.class);
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

    // @Test
    // void pa_aslp_PrePopulate() {
    //     given().repositoryFor(fhirContextR4, "r4/pa-aslp")
    //             .when()
    //             .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "ASLPA1"))
    //             .subjectId("positive")
    //             .parameters(parameters(
    //                     stringPart("Service Request Id", "SleepStudy"),
    //                     stringPart("Service Request Id", "SleepStudy2"),
    //                     stringPart("Coverage Id", "Coverage-positive")))
    //             .thenPrepopulate(true)
    //             .hasItems(13)
    //             .itemHasInitial("1")
    //             .itemHasInitial("2");
    // }

    @Test
    void pa_aslp_Populate() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "ASLPA1"))
                .subjectId("positive")
                .parameters(parameters(
                        stringPart("Service Request Id", "SleepStudy"),
                        stringPart("Service Request Id", "SleepStudy2"),
                        stringPart("Coverage Id", "Coverage-positive")))
                .thenPopulate(true)
                .hasItems(13)
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
}
