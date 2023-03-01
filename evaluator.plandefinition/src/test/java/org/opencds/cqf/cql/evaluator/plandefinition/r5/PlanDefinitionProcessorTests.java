package org.opencds.cqf.cql.evaluator.plandefinition.r5;

import static org.opencds.cqf.cql.evaluator.fhir.util.r5.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r5.Parameters.stringPart;

import org.hl7.fhir.r5.model.Parameters;
import org.testng.annotations.Test;

public class PlanDefinitionProcessorTests extends PlanDefinition {

  @Test()
  public void testChildRoutineVisit() {
    PlanDefinition.Assert
        .that("ChildRoutineVisit-PlanDefinition-1.0.0", "Patient/ChildRoutine-Reportable", null)
        .withAdditionalData("child-routine-visit/child_routine_visit_patient.json")
        .withContent("child-routine-visit/child_routine_visit_plan_definition.json").apply()
        .isEqualsTo("child-routine-visit/child_routine_visit_bundle.json");
  }

  // Disabling due to incompatibility issues with R5 currently
  @Test(enabled = false)
  public void testHelloWorld() {
    PlanDefinition.Assert
        .that("hello-world-patient-view", "helloworld-patient-1",
            "helloworld-patient-1-encounter-1")
        .withAdditionalData("hello-world/hello-world-patient-data.json")
        .withContent("hello-world/hello-world-patient-view-bundle.json").apply()
        .isEqualsTo("hello-world/hello-world-bundle.json");
  }

  // Disabling this test because the current resources are using R4
  @Test(enabled = false)
  public void testOpioidRec10PatientView() {
    /*
     * NOTE: All dynamicValues with the path equaling action.extension have been removed from the
     * plandefinition until the issue in the link https://github.com/DBCG/cqf-ruler/issues/539 has
     * been resolved.
     */
    PlanDefinition.Assert
        .that("opioidcds-10-patient-view", "example-rec-10-patient-view-POS-Cocaine-drugs",
            "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch")
        .withAdditionalData("opioid-Rec10-patient-view/opioid-Rec10-patient-view-patient-data.json")
        .withContent("opioid-Rec10-patient-view/opioid-Rec10-patient-view-bundle.json").apply()
        .isEqualsTo("opioid-Rec10-patient-view/opioid-Rec10-patient-view-result.json");
  }

  // In R5 ActivityDefinitions can no longer create Tasks which breaks this test
  @Test(enabled = false)
  public void testRuleFiltersNotReportable() {
    PlanDefinition.Assert.that("plandefinition-RuleFilters-1.0.0", "NotReportable", null)
        .withAdditionalData("rule-filters/tests-NotReportable-bundle.json")
        .withContent("rule-filters/RuleFilters-1.0.0-bundle.json").apply()
        .isEqualsTo("rule-filters/NotReportableBundle.json");
  }

  // In R5 ActivityDefinitions can no longer create Tasks which breaks this test
  @Test(enabled = false)
  public void testRuleFiltersReportable() {
    PlanDefinition.Assert.that("plandefinition-RuleFilters-1.0.0", "Reportable", null)
        .withAdditionalData("rule-filters/tests-Reportable-bundle.json")
        .withContent("rule-filters/RuleFilters-1.0.0-bundle.json").apply()
        .isEqualsTo("rule-filters/ReportableBundle.json");
  }

  @Test(enabled = false) // Need valid r5 content for this test
  public void testQuestionnairePrepopulate() {
    PlanDefinition.Assert.that("prepopulate", "OPA-Patient1", null)
        .withAdditionalData("prepopulate/prepopulate-patient-data.json")
        .withContent("prepopulate/prepopulate-content-bundle.json")
        .withParameters(new Parameters().addParameter("ClaimId", "OPA-Claim1")).apply()
        .isEqualsTo("prepopulate/prepopulate-bundle.json");
  }

  @Test(enabled = false) // Need valid r5 content for this test
  public void testQuestionnairePrepopulate_NoLibrary() {
    PlanDefinition.Assert.that("prepopulate", "OPA-Patient1", null)
        .withAdditionalData("prepopulate/prepopulate-patient-data.json")
        .withContent("prepopulate/prepopulate-content-bundle-noLibrary.json")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).apply()
        .isEqualsTo("prepopulate/prepopulate-bundle-noLibrary.json");
  }

  @Test
  public void testQuestionnaireResponse() {
    PlanDefinition.Assert.that("prepopulate", "OPA-Patient1", null)
        .withAdditionalData("extract-questionnaireresponse/patient-data.json")
        .withContent("prepopulate/prepopulate-content-bundle.json")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).apply()
        .isEqualsTo("extract-questionnaireresponse/bundle.json");
  }

  @Test(enabled = false) // Not implemented
  public void testGenerateQuestionnaire() {
    PlanDefinition.Assert.that("generate-questionnaire", "OPA-Patient1", null)
        .withAdditionalData("generate-questionnaire/patient-data.json")
        .withContent("generate-questionnaire/content-bundle.json")
        .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1"))).apply()
        .isEqualsTo("generate-questionnaire/bundle.json");
  }
}
