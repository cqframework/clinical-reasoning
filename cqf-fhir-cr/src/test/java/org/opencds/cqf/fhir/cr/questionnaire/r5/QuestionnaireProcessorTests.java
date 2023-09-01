package org.opencds.cqf.fhir.cr.questionnaire.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r5.Parameters.stringPart;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.IdType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.questionnaire.r5.helpers.TestQuestionnaire;

public class QuestionnaireProcessorTests {

    private final FhirContext fhirContext = FhirContext.forR5Cached();

    @Test
    @Disabled // Unable to load R5 packages
    void testPrePopulate() {
        TestQuestionnaire.Assert.that(
                        "resources/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .prePopulate()
                .isEqualsTo("questionnaire-for-order-populated.json");
    }

    @Test
    @Disabled // Unable to load R5 packages and run CQL
    void testPrePopulate_NoLibrary() {
        TestQuestionnaire.Assert.that(
                        "../resources/Questionnaire-OutpatientPriorAuthorizationRequest-noLibrary.json", "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .prePopulate()
                .isEqualsTo("../questionnaire-for-order-populated-noLibrary.json");
    }

    @Test
    @Disabled // Unable to load R5 packages and run CQL
    void testPrePopulate_HasErrors() {
        TestQuestionnaire.Assert.that(
                        "../resources/Questionnaire-OutpatientPriorAuthorizationRequest-Errors.json", "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .prePopulate()
                .isEqualsTo("../questionnaire-for-order-populated-errors.json");
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
            TestQuestionnaire.Assert.that("../resources/Questionnaire-invalid-questionnaire.json", null)
                    .prePopulate();
        });
    }

    @Test
    @Disabled // Unable to load R5 packages and run CQL
    void testPopulate() {
        TestQuestionnaire.Assert.that(
                        "resources/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .populate()
                .isEqualsTo("questionnaire-response-populated.json");
    }

    @Test
    @Disabled // Unable to load R5 packages and run CQL
    void testPopulate_NoLibrary() {
        TestQuestionnaire.Assert.that(
                        "../resources/Questionnaire-OutpatientPriorAuthorizationRequest-noLibrary.json", "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .populate()
                .isEqualsTo("../questionnaire-response-populated-noLibrary.json");
    }

    @Test
    void testPopulate_HasErrors() {
        TestQuestionnaire.Assert.that(
                        "../resources/Questionnaire-OutpatientPriorAuthorizationRequest-Errors.json", "OPA-Patient1")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .populate();
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
            TestQuestionnaire.Assert.that("../resources/Questionnaire-invalid-questionnaire.json", null)
                    .populate();
        });
    }

    @Test
    void testQuestionnairePackage() {
        var generatedPackage = TestQuestionnaire.Assert.that(
                        new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), null)
                .questionnairePackage();

        assertEquals(generatedPackage.getEntry().size(), 3);
        assertEquals(generatedPackage.getEntry().get(0).getResource().fhirType(), FHIRTypes.QUESTIONNAIRE.toCode());
    }
}
