package org.opencds.cqf.fhir.cr.questionnaire.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.questionnaire.r4.helpers.TestQuestionnaire;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

public class QuestionnaireProcessorTests {
    static final String QUESTIONNAIRE_RESPONSE_POPULATED = "questionnaire-response-populated.json";
    static final String QUESTIONNAIRE_RESPONSE_POPULATED_NO_LIBRARY = "questionnaire-response-populated-noLibrary.json";
    static final String QUESTIONNAIRE_INVALID_QUESTIONS = "questionnaire-invalid-questionnaire.json";
    static final String QUESTIONNAIRE_ORDER_POPULATED = "questionnaire-for-order-populated.json";
    static final String QUESTIONNAIRE_ORDER_POPULATED_NO_LIBRARY = "questionnaire-for-order-populated-noLibrary.json";

    @Test
    void testPrePopulate() {
        TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .prePopulate()
                .isEqualsTo("../" + QUESTIONNAIRE_ORDER_POPULATED);
    }

    @Test
    void testPrePopulate_NoLibrary() {
        TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"), "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .prePopulate()
                .isEqualsTo("../" + QUESTIONNAIRE_ORDER_POPULATED_NO_LIBRARY);
    }

    @Test
    void testPrePopulate_HasErrors() {
        TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"), "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .prePopulate()
                .hasErrors();
    }

    @Test
    void testPrePopulate_noQuestionnaire_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestQuestionnaire.Assert.that("", null).prePopulate();
        });
    }

    @Test
    void testPrePopulate_notQuestionnaire_throwsException() {
        assertThrows(ClassCastException.class, () -> {
            TestQuestionnaire.Assert.that("../" + QUESTIONNAIRE_INVALID_QUESTIONS, null)
                    .prePopulate();
        });
    }

    @Test
    void testPopulate() {
        TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .populate()
                .isEqualsTo("../" + QUESTIONNAIRE_RESPONSE_POPULATED);
    }

    @Test
    void testPopulate_NoLibrary() {
        TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"), "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .populate()
                .isEqualsTo("../" + QUESTIONNAIRE_RESPONSE_POPULATED_NO_LIBRARY);
    }

    @Test
    void testPopulate_HasErrors() {
        TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"), "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .populate()
                .hasErrors();
    }

    @Test
    void testPopulate_noQuestionnaire_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestQuestionnaire.Assert.that("", null).populate();
        });
    }

    @Test
    void testPopulate_notQuestionnaire_throwsException() {
        assertThrows(ClassCastException.class, () -> {
            TestQuestionnaire.Assert.that("../" + QUESTIONNAIRE_INVALID_QUESTIONS, null)
                    .populate();
        });
    }

    @Test
    void testQuestionnairePackage() {
        var generatedPackage = TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), null)
                .questionnairePackage();

        assertEquals(generatedPackage.getEntry().size(), 3);
        assertEquals(generatedPackage.getEntry().get(0).getResource().fhirType(), FHIRAllTypes.QUESTIONNAIRE.toCode());
    }

    @Test
    void testPA_ASLP_PrePopulate() {
        var repository = TestRepositoryFactory.createRepository(
                FhirContext.forR4Cached(), this.getClass(), "org/opencds/cqf/fhir/cr/questionnaire/r4/pa-aslp");
        TestQuestionnaire.Assert.that(new IdType("Questionnaire", "ASLPA1"), "positive")
                .withRepository(repository)
                .withParameters(parameters(
                        stringPart("Service Request Id", "SleepStudy"),
                        stringPart("Service Request Id", "SleepStudy2"),
                        stringPart("Coverage Id", "Coverage-positive")))
                .prePopulate()
                .hasItems(13)
                .itemHasInitial("1")
                .itemHasInitial("2");
    }

    @Test
    void testPA_ASLP_Populate() {
        var repository = TestRepositoryFactory.createRepository(
                FhirContext.forR4Cached(), this.getClass(), "org/opencds/cqf/fhir/cr/questionnaire/r4/pa-aslp");
        TestQuestionnaire.Assert.that(new IdType("Questionnaire", "ASLPA1"), "positive")
                .withRepository(repository)
                .withParameters(parameters(
                        stringPart("Service Request Id", "SleepStudy"),
                        stringPart("Service Request Id", "SleepStudy2"),
                        stringPart("Coverage Id", "Coverage-positive")))
                .populate()
                .hasItems(13)
                .itemHasAnswer("1")
                .itemHasAnswer("2");
    }

    @Test
    void testPA_ASLP_Package() {
        var repository = TestRepositoryFactory.createRepository(
                FhirContext.forR4Cached(), this.getClass(), "org/opencds/cqf/fhir/cr/questionnaire/r4/pa-aslp");
        var generatedPackage = TestQuestionnaire.Assert.that(new IdType("Questionnaire", "ASLPA1"), null)
                .withRepository(repository)
                .questionnairePackage();

        assertFalse(generatedPackage.getEntry().isEmpty());
        assertEquals(generatedPackage.getEntry().size(), 11);
    }
}
