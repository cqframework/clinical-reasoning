package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r5.Parameters.stringPart;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.evaluator.questionnaire.r5.helpers.TestItemGenerator;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

import ca.uhn.fhir.context.FhirContext;

public class ItemGeneratorTests {
  public static String QUESTIONNAIRE_PATIENT_FILE_NAME = "Questionnaire-RouteOnePatient.json";
  public static String QUESTIONNAIRE_SLEEP_STUDY_FILE_NAME =
      "Questionnaire-aslp-sleep-study-order.json";

  @Test
  void testGenerateItem() {
    TestItemGenerator.Assert.that("Patient",
        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient", "OPA-Patient1")
        .generateItem()
        .isEqualsTo("../" + QUESTIONNAIRE_PATIENT_FILE_NAME);
  }

  @Test
  void testSleepStudyOrder() {
    var repository = TestRepositoryFactory.createRepository(
        TestItemGenerator.fhirContext,
        this.getClass(),
        "org/opencds/cqf/cql/evaluator/questionnaire/r5/pa-aslp");
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
