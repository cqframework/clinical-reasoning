package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import static org.opencds.cqf.cql.evaluator.fhir.util.r5.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r5.Parameters.stringPart;

import org.opencds.cqf.cql.evaluator.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.cql.evaluator.questionnaire.r5.helpers.TestItemGenerator;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class ItemGeneratorTests {
  public static String QUESTIONNAIRE_PATIENT_FILE_NAME = "Questionnaire-RouteOnePatient.json";
  public static String QUESTIONNAIRE_SLEEP_STUDY_FILE_NAME =
      "Questionnaire-aslp-sleep-study-order.json";

  @Test
  void testGenerateItem() {
    var repository =
        TestRepositoryFactory.createRepository(FhirContext.forR5Cached(), this.getClass());
    TestItemGenerator.Assert.that("Patient",
        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient", "OPA-Patient1")
        .withRepository(repository)
        .generateItem()
        .isEqualsTo("../" + QUESTIONNAIRE_PATIENT_FILE_NAME);
  }

  @Test
  void testSleepStudyOrder() {
    var repository = TestRepositoryFactory.createRepository(
        FhirContext.forR5Cached(),
        this.getClass(),
        "pa-aslp");
    TestItemGenerator.Assert.that(
        "ServiceRequest",
        "http://example.org/sdh/dtr/aslp/StructureDefinition/aslp-sleep-study-order",
        "positive")
        .withRepository(repository)
        .withParameters(parameters(stringPart("Service Request Id", "SleepStudy"),
            stringPart("Service Request Id", "SleepStudy2"),
            stringPart("Coverage Id", "Coverage-positive")))
        .generateItem()
        .isEqualsTo("../" + QUESTIONNAIRE_SLEEP_STUDY_FILE_NAME);
  }
}
