package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ItemGeneratorTests {
  @Test
  @Disabled("Need valid dstu3 content for this test")
  void testGenerateItem() {
    TestItemGenerator.Assert.that("Patient",
        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient", "OPA-Patient1")
        .generateItem().isEqualsTo("tests/Questionnaire-RouteOnePatient.json");
  }
}
