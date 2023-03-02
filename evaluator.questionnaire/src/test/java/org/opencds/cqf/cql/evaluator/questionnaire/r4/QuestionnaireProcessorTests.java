package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;
import static org.testng.Assert.assertThrows;

import java.util.List;

import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.testng.annotations.Test;

public class QuestionnaireProcessorTests {
  @Test
  void testPrePopulate() {
    TestQuestionnaire.Assert
        .that("res/content/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulateWithEngine()
        .isEqualsTo("questionnaire-for-order-populated.json");
  }

  @Test
  void testPrePopulate_NoLibrary() {
    FhirRepository data = new FhirRepository(this.getClass(), List.of("res/tests"), false);

    var repository = Repositories.proxy(data, null, null);
    TestQuestionnaire.Assert
        .that("res/content/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
        .withRepository(repository).withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .prePopulateWithEngine().isEqualsTo("questionnaire-for-order-populated-noLibrary.json");
  }

  @Test
  void testPrePopulate_HasErrors() {
    TestQuestionnaire.Assert.that("questionnaire-for-order-errors.json", "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulateWithEngine()
        .isEqualsTo("questionnaire-for-order-populated-errors.json");
  }

  @Test
  void testPrePopulate_noQuestionnaire_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      TestQuestionnaire.Assert.that("", null).prePopulateWithEngine();
    });
  }

  @Test
  void testPrePopulate_notQuestionnaire_throwsException() {
    assertThrows(ClassCastException.class, () -> {
      TestQuestionnaire.Assert.that("invalid-questionnaire.json", null).prePopulateWithEngine();
    });
  }

  @Test
  void testPopulate() {
    TestQuestionnaire.Assert
        .that("res/content/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populateWithEngine()
        .isEqualsTo("questionnaire-response-populated.json");
  }

  @Test
  void testPopulate_NoLibrary() {
    FhirRepository data = new FhirRepository(this.getClass(), List.of("res/tests"), false);

    var repository = Repositories.proxy(data, null, null);
    TestQuestionnaire.Assert.that("questionnaire-for-order.json", "OPA-Patient1")
        .withRepository(repository).withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .populateWithEngine().isEqualsTo("questionnaire-response-populated-noLibrary.json");
  }

  @Test
  void testPopulate_HasErrors() {
    TestQuestionnaire.Assert.that("questionnaire-for-order-errors.json", "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populateWithEngine()
        .isEqualsTo("questionnaire-response-populated-errors.json");
  }

  @Test
  void testPopulate_noQuestionnaire_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      TestQuestionnaire.Assert.that("", null).populateWithEngine();
    });
  }

  @Test
  void testPopulate_notQuestionnaire_throwsException() {
    assertThrows(ClassCastException.class, () -> {
      TestQuestionnaire.Assert.that("invalid-questionnaire.json", null).populateWithEngine();
    });
  }
}
