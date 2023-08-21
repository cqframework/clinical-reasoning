package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import org.junit.jupiter.api.Test;

public class ItemGeneratorTests {
  @Test
  void testGenerateItem() {
    TestItemGenerator.Assert.that("Patient",
        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient", "OPA-Patient1")
        .generateItem().isEqualsTo("tests/Questionnaire-RouteOnePatient.json");
  }
}
