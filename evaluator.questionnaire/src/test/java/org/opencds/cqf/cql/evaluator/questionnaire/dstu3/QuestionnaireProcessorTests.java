package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.stringPart;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.util.List;

import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.opencds.cqf.cql.evaluator.fhir.repository.dstu3.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.testng.annotations.Test;

public class QuestionnaireProcessorTests {
  @Test(enabled = false)
  void testPrePopulate() {
    TestQuestionnaire.Assert
        .that("content/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulate()
        .isEqualsTo("questionnaire-for-order-populated.json");
  }

  @Test
  void testPrePopulate_NoLibrary() {
    var data = new FhirRepository(this.getClass(), List.of("tests"), false);
    var repository = Repositories.proxy(data, null, null);
    TestQuestionnaire.Assert
        .that("content/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
        .withRepository(repository).withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .prePopulate().isEqualsTo("questionnaire-for-order-populated-noLibrary.json");
  }

  @Test
  void testPrePopulate_HasErrors() {
    TestQuestionnaire.Assert
        .that("content/Questionnaire-OutpatientPriorAuthorizationRequest-Errors.json",
            "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulate()
        .isEqualsTo("questionnaire-for-order-populated-errors.json");
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
      TestQuestionnaire.Assert.that("content/Questionnaire-invalid-questionnaire.json", null)
          .prePopulate();
    });
  }

  @Test(enabled = false)
  void testPopulate() {
    TestQuestionnaire.Assert
        .that("content/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populate()
        .isEqualsTo("questionnaire-response-populated.json");
  }

  @Test
  void testPopulate_NoLibrary() {
    var data = new FhirRepository(this.getClass(), List.of("tests"), false);
    var repository = Repositories.proxy(data, null, null);
    TestQuestionnaire.Assert
        .that("content/Questionnaire-OutpatientPriorAuthorizationRequest.json", "OPA-Patient1")
        .withRepository(repository).withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .populate().isEqualsTo("questionnaire-response-populated-noLibrary.json");
  }

  @Test
  void testPopulate_HasErrors() {
    TestQuestionnaire.Assert
        .that("content/Questionnaire-OutpatientPriorAuthorizationRequest-Errors.json",
            "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populate()
        .isEqualsTo("questionnaire-response-populated-errors.json");
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
      TestQuestionnaire.Assert.that("content/Questionnaire-invalid-questionnaire.json", null)
          .populate();
    });
  }

  @Test
  void testQuestionnairePackage() {
    var generatedPackage = TestQuestionnaire.Assert
        .that("content/Questionnaire-OutpatientPriorAuthorizationRequest.json", null)
        .questionnairePackage();

    assertEquals(generatedPackage.getEntry().size(), 3);
    assertEquals(generatedPackage.getEntry().get(0).getResource().fhirType(),
        FHIRAllTypes.QUESTIONNAIRE.toCode());
  }
}
