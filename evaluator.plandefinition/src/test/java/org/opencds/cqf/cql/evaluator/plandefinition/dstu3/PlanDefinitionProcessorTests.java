package org.opencds.cqf.cql.evaluator.plandefinition.dstu3;

import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.testng.annotations.Test;

import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.dstu3.Parameters.stringPart;

public class PlanDefinitionProcessorTests extends PlanDefinition {
  @Test
  public void testChildRoutineVisit() {
    PlanDefinition.Assert
        .that("ChildRoutineVisit-PlanDefinition-1.0.0", "Patient/ChildRoutine-Reportable", null)
        .withData("child-routine-visit/child_routine_visit_patient.json")
        .withLibrary("child-routine-visit/child_routine_visit_plan_definition.json").apply()
        .isEqualsTo("child-routine-visit/child_routine_visit_careplan.json");
  }

  @Test
  public void testHelloWorld() {
    PlanDefinition.Assert
        .that("hello-world-patient-view", "helloworld-patient-1",
            "helloworld-patient-1-encounter-1")
        .withData("hello-world/hello-world-patient-data.json")
        .withLibrary("hello-world/hello-world-patient-view-bundle.json").apply()
        .isEqualsTo("hello-world/hello-world-careplan.json");
  }

  @Test
  public void testOpioidRec10PatientView() {
    /*
     * NOTE: All dynamicValues with the path equaling action.extension have been removed from the
     * plandefinition until the issue in the link https://github.com/DBCG/cqf-ruler/issues/539 has
     * been resolved.
     */
    PlanDefinition.Assert
        .that("opioidcds-10", "example-rec-10-patient-view-POS-Cocaine-drugs",
            "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch")
        .withData("opioid-Rec10-patient-view/opioid-Rec10-patient-view-patient-data.json")
        .withLibrary("opioid-Rec10-patient-view/opioid-Rec10-patient-view-bundle.json").apply()
        .isEqualsTo("opioid-Rec10-patient-view/opioid-Rec10-patient-view-careplan.json");
  }

  @Test
  public void testRuleFiltersNotReportable() {
    PlanDefinition.Assert.that("plandefinition-RuleFilters-1.0.0", "NotReportable", null)
        .withData("rule-filters/tests-NotReportable-bundle.json")
        .withLibrary("rule-filters/RuleFilters-1.0.0-bundle.json").apply()
        .isEqualsTo("rule-filters/NotReportableCarePlan.json");
  }

  @Test
  public void testRuleFiltersReportable() {
    PlanDefinition.Assert.that("plandefinition-RuleFilters-1.0.0", "Reportable", null)
        .withData("rule-filters/tests-Reportable-bundle.json")
        .withLibrary("rule-filters/RuleFilters-1.0.0-bundle.json").apply()
        .isEqualsTo("rule-filters/ReportableCarePlan.json");
  }

    @Test (enabled = false) // Need valid dstu3 content for this test
    public void testQuestionnairePrepopulate() {
        PlanDefinition.Assert.that(
                    "prepopulate",
                    "OPA-Patient1",
                    null
                )
                .withData("prepopulate/prepopulate-patient-data.json")
                .withLibrary("prepopulate/prepopulate-content-bundle.json")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .apply()
                .isEqualsTo("prepopulate/prepopulate-careplan.json");
    }

    @Test (enabled = false) // Need valid dstu3 content for this test
    public void testQuestionnairePrepopulate_NoLibrary() {
        PlanDefinition.Assert.that(
                        "prepopulate",
                        "OPA-Patient1",
                        null
                )
                .withData("prepopulate/prepopulate-patient-data.json")
                .withLibrary("prepopulate/prepopulate-content-bundle-noLibrary.json")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .apply()
                .isEqualsTo("prepopulate/prepopulate-careplan.json");
    }

    @Test
    public void testQuestionnaireResponse() {
        PlanDefinition.Assert.that(
                        "prepopulate",
                        "OPA-Patient1",
                        null
                )
                .withData("extract-questionnaireresponse/patient-data.json")
                .withLibrary("prepopulate/prepopulate-content-bundle.json")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .apply()
                .isEqualsTo("extract-questionnaireresponse/careplan.json");
    }

    @Test (enabled = false) // Not implemented
    public void testGenerateQuestionnaire() {
        PlanDefinition.Assert.that(
                        "generate-Questionnaire",
                        "OPA-Patient1",
                        null
                )
                .withData("generate-questionnaire/patient-data.json")
                .withLibrary("generate-questionnaire/content-bundle.json")
                .withParameters(parameters(stringPart("ClaimId", "OPA-Claim1")))
                .apply()
                .isEqualsTo("generate-questionnaire/careplan.json");
    }
}
