package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers.TestItemGenerator;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

import ca.uhn.fhir.context.FhirContext;

public class ItemGeneratorTests {
  public static String QUESTIONNAIRE_PATIENT_FILE_NAME = "Questionnaire-RouteOnePatient.json";
  public static String QUESTIONNAIRE_SLEEP_STUDY_FILE_NAME =
      "Questionnaire-aslp-sleep-study-order.json";

  @Test
  void testGenerateItem() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forDstu3Cached(), this.getClass());
    TestItemGenerator.Assert.that("Patient",
        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient", "OPA-Patient1")
        .withRepository(repository)
        .generateItem()
        .isEqualsTo("../" + QUESTIONNAIRE_PATIENT_FILE_NAME);
  }
}
