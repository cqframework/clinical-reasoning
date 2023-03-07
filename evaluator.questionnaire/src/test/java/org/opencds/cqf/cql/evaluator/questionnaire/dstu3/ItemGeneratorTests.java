package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import org.testng.annotations.Test;

public class ItemGeneratorTests {
  @Test(enabled = false) // Need valid dstu3 content for this test
  void testGenerateItem() {
    TestItemGenerator.Assert.that("Patient",
        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient", "OPA-Patient1")
        .generateItem().isEqualsTo("tests/Questionnaire-RouteOnePatient.json");
  }
}
