package org.opencds.cqf.cql.evaluator.questionnaireresponse.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

import ca.uhn.fhir.context.FhirContext;

public class QuestionnaireResponseProcessorTests {
  private void testExtract(String questionnaireResponse) {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    TestQuestionnaireResponse.Assert
        .that(new IdType("QuestionnaireResponse", questionnaireResponse))
        .withRepository(repository)
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
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    TestQuestionnaireResponse.Assert
        .that(
            new IdType("QuestionnaireResponse", "OutpatientPriorAuthorizationRequest-OPA-Patient1"))
        .withRepository(repository)
        .withExpectedBundleId(
            new IdType("Bundle", "extract-OutpatientPriorAuthorizationRequest-OPA-Patient1"))
        .extract().hasEntry(2);
  }


  @Test
  @Disabled("JP - Don't know why this is disabled")
  void testDemographicsExtraction() {
    testExtract("demographics-qr");
  }
}
