package org.opencds.cqf.cql.evaluator.questionnaireresponse.dstu3;

import static org.testng.Assert.assertThrows;

import org.testng.annotations.Test;

public class QuestionnaireResponseProcessorTests {
  private void testExtract(String questionnaireResponse) {
    TestQuestionnaireResponse.Assert
        .that("tests/QuestionnaireResponse-" + questionnaireResponse + ".json").extract()
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

  @Test(enabled = false)
  void testDemographicsExtraction() {
    testExtract("demographics-qr");
  }
}
