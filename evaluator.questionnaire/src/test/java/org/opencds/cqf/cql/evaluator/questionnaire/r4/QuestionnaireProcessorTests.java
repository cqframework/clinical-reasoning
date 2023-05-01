package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;

import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.cql.evaluator.fhir.test.TestRepositoryFactory;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class QuestionnaireProcessorTests {
  @Test
  void testPrePopulate() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), "OPA-Patient1")
        .withRepository(repository)
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulate()
        .isEqualsTo("questionnaire-for-order-populated.json");
  }

  @Test
  void testPrePopulate_NoLibrary() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"),
            "OPA-Patient1")
        .withRepository(repository).withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .prePopulate().isEqualsTo("questionnaire-for-order-populated-noLibrary.json");
  }

  @Test
  void testPrePopulate_HasErrors() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"),
            "OPA-Patient1")
        .withRepository(repository)
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).prePopulate()
        .hasErrors();
  }

  @Test
  void testPrePopulate_noQuestionnaire_throwsException() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    assertThrows(IllegalArgumentException.class, () -> {
      TestQuestionnaire.Assert.that("", null).withRepository(repository).prePopulate();
    });
  }

  @Test
  void testPrePopulate_notQuestionnaire_throwsException() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    assertThrows(ClassCastException.class, () -> {
      TestQuestionnaire.Assert
          .that("questionnaire-invalid-questionnaire.json", null)
          .withRepository(repository)
          .prePopulate();
    });
  }

  @Test
  void testPopulate() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());

    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), "OPA-Patient1")
        .withRepository(repository)
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populate()
        .isEqualsTo("questionnaire-response-populated.json");
  }

  @Test
  void testPopulate_NoLibrary() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-noLibrary"),
            "OPA-Patient1")
        .withRepository(repository).withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
        .populate().isEqualsTo("questionnaire-response-populated-noLibrary.json");
  }

  @Test
  void testPopulate_HasErrors() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest-Errors"),
            "OPA-Patient1")
        .withRepository(repository)
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).populate()
        .hasErrors();
  }

  @Test
  void testPopulate_noQuestionnaire_throwsException() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    assertThrows(IllegalArgumentException.class, () -> {
      TestQuestionnaire.Assert.that("", null).withRepository(repository).populate();
    });
  }

  @Test
  void testPopulate_notQuestionnaire_throwsException() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    assertThrows(ClassCastException.class, () -> {
      TestQuestionnaire.Assert.that("questionnaire-invalid-questionnaire.json", null)
          .withRepository(repository)
          .populate();
    });
  }

  @Test
  void testQuestionnairePackage() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass());
    var generatedPackage = TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "OutpatientPriorAuthorizationRequest"), null)
        .withRepository(repository)
        .questionnairePackage();

    assertEquals(generatedPackage.getEntry().size(), 3);
    assertEquals(generatedPackage.getEntry().get(0).getResource().fhirType(),
        FHIRAllTypes.QUESTIONNAIRE.toCode());
  }

  @Test
  void testPA_ASLP_PrePopulate() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass(),
            "pa-aslp");
    TestQuestionnaire.Assert.that(new IdType("Questionnaire", "ASLPA1"), "positive")
        .withRepository(repository)
        .withParameters(parameters(stringPart("Service Request Id", "SleepStudy"),
            stringPart("Service Request Id", "SleepStudy2"),
            stringPart("Coverage Id", "Coverage-positive")))
        .prePopulate().hasItems(13).itemHasInitial("1").itemHasInitial("2");
  }

  @Test
  void testPA_ASLP_Populate() {
    var repository = TestRepositoryFactory.createRepository(FhirContext.forR4Cached(),
        this.getClass(), "pa-aslp");
    TestQuestionnaire.Assert.that(new IdType("Questionnaire", "ASLPA1"), "positive")
        .withRepository(repository)
        .withParameters(parameters(stringPart("Service Request Id", "SleepStudy"),
            stringPart("Service Request Id", "SleepStudy2"),
            stringPart("Coverage Id", "Coverage-positive")))
        .populate().hasItems(13).itemHasAnswer("1").itemHasAnswer("2");
  }

  @Test
  void testPA_ASLP_Package() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR4Cached(), this.getClass(),
            "pa-aslp");
    var generatedPackage = TestQuestionnaire.Assert
        .that(new IdType("Questionnaire", "ASLPA1"), null).withRepository(repository)
        .questionnairePackage();

    assertFalse(generatedPackage.getEntry().isEmpty(), null);
    assertEquals(generatedPackage.getEntry().size(), 11);
  }
}
