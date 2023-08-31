package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.dstu3.Parameters.stringPart;

import java.util.List;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers.TestQuestionnaire;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

import ca.uhn.fhir.context.FhirContext;

public class QuestionnaireProcessorTests {
  final static String QUESTIONNAIRE_INVALID_QUESTIONS = "questionnaire-invalid-questionnaire.json";

  @Test
  @Disabled
  void testPrePopulate() {
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulate()
        .isEqualsTo("questionnaire-for-order-populated.json");
  }

  @Test
  void testPrePopulate_NoLibrary() {
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"),
            "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .prePopulate().isEqualsTo("../questionnaire-for-order-populated-noLibrary.json");
  }

  @Test
  void testPrePopulate_HasErrors() {
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"),
            "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulate()
        .hasErrors();
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
      TestQuestionnaire.Assert
          .that("../" + QUESTIONNAIRE_INVALID_QUESTIONS, null)
          .prePopulate();
    });
  }

  @Test
  @Disabled
  void testPopulate() {
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populate()
        .isEqualsTo("questionnaire-response-populated.json");
  }

  @Test
  void testPopulate_NoLibrary() {
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"),
            "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .populate().isEqualsTo("../questionnaire-response-populated-noLibrary.json");
  }

  @Test
  void testPopulate_HasErrors() {
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"),
            "OPA-Patient1")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populate()
        .hasErrors();
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
      TestQuestionnaire.Assert
          .that("../" + QUESTIONNAIRE_INVALID_QUESTIONS, null)
          .populate();
    });
  }

  @Test
  void testQuestionnairePackage() {
    var generatedPackage = TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), null)
        .questionnairePackage();

    assertEquals(generatedPackage.getEntry().size(), 3);
    assertEquals(generatedPackage.getEntry().get(0).getResource().fhirType(),
        FHIRAllTypes.QUESTIONNAIRE.toCode());
  }
}
