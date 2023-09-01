package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class QuestionnaireResponseProcessorTests {
  private void testExtract(String questionnaireResponse) {
    TestQuestionnaireResponse.Assert
        .that(new IdType("QuestionnaireResponse", questionnaireResponse))
        .withExpectedBundleId(new IdType("Bundle", "extract-" + questionnaireResponse))
        .extract()
        .isEqualsToExpected();
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
    TestQuestionnaireResponse.Assert
        .that(
            new IdType("QuestionnaireResponse", "OutpatientPriorAuthorizationRequest-OPA-Patient1"))
        .withExpectedBundleId(
            new IdType("Bundle", "extract-OutpatientPriorAuthorizationRequest-OPA-Patient1"))
        .extract().hasEntry(2);
  }

  @Test
  void testNestedDefinitionBaseExtraction() {
    TestQuestionnaireResponse.Assert
        .that(
            new IdType("QuestionnaireResponse", "cc-screening-pathway-definition-answers"))
        .withExpectedBundleId(
            new IdType("Bundle", "extract-cc-screening-pathway-definition-answers"))
        .extract().hasEntry(3);
  }

  @Test
  @Disabled("JP - Don't know why this is disabled")
  void testDemographicsExtraction() {
    testExtract("demographics-qr");
  }
}
