package org.opencds.cqf.fhir.cr.questionnaireresponse.r5;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class QuestionnaireResponseProcessorTests {
    private void testExtract(String questionnaireResponse) {
        TestQuestionnaireResponse.Assert.that("tests/QuestionnaireResponse-" + questionnaireResponse + ".json")
                .extract()
                .isEqualsTo("tests/Bundle-" + questionnaireResponse + ".json");
    }

    @Test
    void test() {
        testExtract("QRSharonDecision");
    }

    @Test
    void testExtract_noQuestionnaireReference_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            testExtract("mypain-no-url");
        });
    }

    @Test
    void testIsSubjectExtension() {
        testExtract("sdc-profile-example-multi-subject");
    }

    @Test
    void testDefinitionBasedExtraction() {
        testExtract("OutpatientPriorAuthorizationRequest");
    }

    @Test
    @Disabled("JP - Don't know why this is disabled")
    void testDemographicsExtraction() {
        testExtract("demographics-qr");
    }
}
