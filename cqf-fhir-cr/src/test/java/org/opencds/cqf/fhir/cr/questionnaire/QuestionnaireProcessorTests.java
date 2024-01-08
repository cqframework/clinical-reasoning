package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.questionnaire.TestQuestionnaire.given;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

public class QuestionnaireProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();
    private final Repository repositoryDstu3 = TestRepositoryFactory.createRepository(
            fhirContextDstu3, this.getClass(), CLASS_PATH + "/dstu3", IGLayoutMode.TYPE_PREFIX);
    private final Repository repositoryR4 = TestRepositoryFactory.createRepository(
            fhirContextR4, this.getClass(), CLASS_PATH + "/r4", IGLayoutMode.TYPE_PREFIX);
    private final Repository repositoryR5 = TestRepositoryFactory.createRepository(
            fhirContextR5, this.getClass(), CLASS_PATH + "/r4", IGLayoutMode.TYPE_PREFIX);

    @Test
    void testPrePopulate() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .thenPrepopulate()
                .isEqualsToExpected(org.hl7.fhir.r4.model.Questionnaire.class);
    }

    @Test
    void testPrePopulate_NoLibrary() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(
                        Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .thenPrepopulate()
                .isEqualsToExpected(org.hl7.fhir.r4.model.Questionnaire.class);
    }

    @Test
    void testPrePopulate_HasErrors() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(
                        Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .thenPrepopulate()
                .hasErrors();
    }

    @Test
    void testPrePopulate_noQuestionnaire_throwsException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            given().repository(repositoryR4)
                    .when()
                    .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "null"))
                    .thenPrepopulate();
        });
    }

    @Test
    void testPopulate() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .thenPopulate()
                .isEqualsToExpected(org.hl7.fhir.r4.model.QuestionnaireResponse.class);
    }

    @Test
    void testPopulate_NoLibrary() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(
                        Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .thenPopulate()
                .isEqualsToExpected(org.hl7.fhir.r4.model.QuestionnaireResponse.class);
    }

    @Test
    void testPopulate_HasErrors() {
        given().repository(repositoryR4)
                .when()
                .questionnaireId(
                        Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"))
                .subjectId("OPA-Patient1")
                .parameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .thenPopulate()
                .hasErrors();
    }

    @Test
    void testPopulate_noQuestionnaire_throwsException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            given().repository(repositoryR4)
                    .when()
                    .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "null"))
                    .thenPopulate();
        });
    }

    @Test
    void testQuestionnairePackageDstu3() {
        var bundle = (org.hl7.fhir.dstu3.model.Bundle) given().repository(repositoryDstu3)
                .when()
                .questionnaireId(Ids.newId(fhirContextDstu3, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenPackage();

        assertEquals(bundle.getEntry().size(), 3);
        assertEquals(bundle.getEntry().get(0).getResource().fhirType(), "Questionnaire");
    }

    @Test
    void testQuestionnairePackageR4() {
        var bundle = (org.hl7.fhir.r4.model.Bundle) given().repository(repositoryR4)
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenPackage();

        assertEquals(bundle.getEntry().size(), 3);
        assertEquals(bundle.getEntry().get(0).getResource().fhirType(), "Questionnaire");
    }

    @Test
    void testQuestionnairePackageR5() {
        var bundle = (org.hl7.fhir.r5.model.Bundle) given().repository(repositoryR5)
                .when()
                .questionnaireId(Ids.newId(fhirContextR5, "Questionnaire", "OutpatientPriorAuthorizationRequest"))
                .thenPackage();

        assertEquals(bundle.getEntry().size(), 3);
        assertEquals(bundle.getEntry().get(0).getResource().fhirType(), "Questionnaire");
    }

    @Test
    void testPA_ASLP_PrePopulate() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "ASLPA1"))
                .subjectId("positive")
                .parameters(parameters(
                        stringPart("Service Request Id", "SleepStudy"),
                        stringPart("Service Request Id", "SleepStudy2"),
                        stringPart("Coverage Id", "Coverage-positive")))
                .thenPrepopulate()
                .hasItems(13)
                .itemHasInitial("1")
                .itemHasInitial("2");
    }

    @Test
    void testPA_ASLP_Populate() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "ASLPA1"))
                .subjectId("positive")
                .parameters(parameters(
                        stringPart("Service Request Id", "SleepStudy"),
                        stringPart("Service Request Id", "SleepStudy2"),
                        stringPart("Coverage Id", "Coverage-positive")))
                .thenPopulate()
                .hasItems(13)
                .itemHasAnswer("1")
                .itemHasAnswer("2");
    }

    @Test
    void testPA_ASLP_Package() {
        var bundle = (org.hl7.fhir.r4.model.Bundle) given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .questionnaireId(Ids.newId(fhirContextR4, "Questionnaire", "ASLPA1"))
                .thenPackage();

        assertFalse(bundle.getEntry().isEmpty());
        assertEquals(bundle.getEntry().size(), 11);
    }
}
