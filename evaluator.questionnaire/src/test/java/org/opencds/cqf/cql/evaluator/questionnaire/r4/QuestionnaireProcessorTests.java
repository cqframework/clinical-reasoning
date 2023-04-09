package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;

import java.util.List;

import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.fhir.api.Repository;
import org.testng.annotations.Test;

public class QuestionnaireProcessorTests {
  private Repository createRepositoryForPath(String path) {
    var data = new FhirRepository(this.getClass(), List.of(path + "/tests"), false);
    var content = new FhirRepository(this.getClass(), List.of(path + "/content"), false);
    var terminology =
        new FhirRepository(this.getClass(), List.of(path + "/vocabulary/ValueSet"), false);

    return Repositories.proxy(data, content, terminology);
  }

  @Test
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

  @Test
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

  @Test
  void testPA_ASLP_PrePopulate() {
    var repository = createRepositoryForPath("pa-aslp");
    TestQuestionnaire.Assert.that("pa-aslp/content/Questionnaire-ASLPA1.json", "positive")
        .withRepository(repository)
        .withParameters(parameters(stringPart("Service Request Id", "SleepStudy"),
            stringPart("Service Request Id", "SleepStudy2"),
            stringPart("Coverage Id", "Coverage-positive")))
        .prePopulate().hasItems(13).itemHasInitialValue("1").itemHasInitialValue("2");
  }

  @Test
  void testPA_ASLP_Populate() {
    var repository = createRepositoryForPath("pa-aslp");
    TestQuestionnaire.Assert.that("pa-aslp/content/Questionnaire-ASLPA1.json", "positive")
        .withRepository(repository)
        .withParameters(parameters(stringPart("Service Request Id", "SleepStudy"),
            stringPart("Service Request Id", "SleepStudy2"),
            stringPart("Coverage Id", "Coverage-positive")))
        .populate().hasItems(13).itemHasAnswer("1").itemHasAnswer("2");
  }

  @Test
  void testPA_ASLP_Package() {
    var repository = createRepositoryForPath("pa-aslp");
    var generatedPackage = TestQuestionnaire.Assert
        .that("pa-aslp/content/Questionnaire-ASLPA1.json", null).withRepository(repository)
        .questionnairePackage();

    assertFalse(generatedPackage.getEntry().isEmpty(), null);
    assertEquals(generatedPackage.getEntry().size(), 11);
  }
}
