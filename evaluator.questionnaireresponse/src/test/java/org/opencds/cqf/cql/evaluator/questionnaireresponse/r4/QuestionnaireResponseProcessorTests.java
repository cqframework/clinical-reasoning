package org.opencds.cqf.cql.evaluator.questionnaireresponse.r4;

import static org.testng.Assert.assertThrows;

import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.cql.evaluator.fhir.test.TestRepositoryFactory;
import org.testng.annotations.Test;

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

  @Test(enabled = false)
  void testDemographicsExtraction() {
    testExtract("demographics-qr");
  }
}
