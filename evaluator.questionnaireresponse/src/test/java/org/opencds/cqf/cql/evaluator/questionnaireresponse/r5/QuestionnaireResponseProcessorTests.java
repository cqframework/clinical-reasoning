package org.opencds.cqf.cql.evaluator.questionnaireresponse.r5;

import org.testng.annotations.Test;

import static org.testng.Assert.assertThrows;

public class QuestionnaireResponseProcessorTests {
    @Test
    void testExtract() {
        TestQuestionnaireResponse.Assert.that("questionnaire_response_1558.json")
                .withData("questionnaire_1559.json").extract()
                .isEqualsTo("questionnaire_response_1558_bundle.json");
    }

    @Test
    void testExtract_noQuestionnaireReference_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestQuestionnaireResponse.Assert.that("mypain-questionnaire-response-no-url.json")
                    .extract();
        });
    }

    @Test
    void testIsSubjectExtension() {
        TestQuestionnaireResponse.Assert.that("questionnaire_response_is_subject.json")
                .withData("questionnaire_is_subject.json").extract()
                .isEqualsTo("questionnaire_response_is_subject_bundle.json");
    }

    @Test
    void testDefinitionBasedExtraction() {
        TestQuestionnaireResponse.Assert.that("questionnaire_response_definition.json")
                .withData("questionnaire_definition.json").extract()
                .isEqualsTo("questionnaire_response_definition_bundle.json");
    }

    @Test(enabled = false)
    void testDemographicsExtraction() {
        TestQuestionnaireResponse.Assert.that("questionnaire_response_demographics.json")
                .withData("questionnaire_demographics.json").extract()
                .isEqualsTo("questionnaire_response_demographics_bundle.json");
    }
}
