package org.opencds.cqf.cql.evaluator.plandefinition.r5;

import org.testng.annotations.Test;

public class PlanDefinitionProcessorTests extends PlanDefinition {

    @Test()
    public void testChildRoutineVisit() {
        PlanDefinition.Assert.that(
                        "ChildRoutineVisit-PlanDefinition-1.0.0",
                        "Patient/ChildRoutine-Reportable",
                        null
                )
                .withData("child-routine-visit/child_routine_visit_patient.json")
                .withLibrary("child-routine-visit/child_routine_visit_plan_definition.json")
                .apply()
                .isEqualsTo("child-routine-visit/child_routine_visit_bundle.json");
    }

    // Disabling due to incompatibility issues with R5 currently
    @Test(enabled = false)
    public void testHelloWorld() {
        PlanDefinition.Assert.that(
                        "hello-world-patient-view",
                        "helloworld-patient-1",
                        "helloworld-patient-1-encounter-1"
                )
                .withData("hello-world/hello-world-patient-data.json")
                .withLibrary("hello-world/hello-world-patient-view-bundle.json")
                .apply()
                .isEqualsTo("hello-world/hello-world-bundle.json");
    }

    // Disabling this test because the current resources are using R4
    @Test(enabled = false)
    public void testOpioidRec10PatientView() {
         /* NOTE:
             All dynamicValues with the path equaling action.extension have been removed from the
             plandefinition until the issue in the link https://github.com/DBCG/cqf-ruler/issues/539 has been resolved.
          */
        PlanDefinition.Assert.that(
                        "opioidcds-10-patient-view",
                        "example-rec-10-patient-view-POS-Cocaine-drugs",
                        "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch"
                )
                .withData(
                        "opioid-Rec10-patient-view/opioid-Rec10-patient-view-patient-data.json"
                )
                .withLibrary(
                        "opioid-Rec10-patient-view/opioid-Rec10-patient-view-bundle.json"
                )
                .apply()
                .isEqualsTo(
                        "opioid-Rec10-patient-view/opioid-Rec10-patient-view-result.json"
                );
    }

    // In R5 ActivityDefinitions can no longer create Tasks which breaks this test
    @Test(enabled = false)
    public void testRuleFiltersNotReportable() {
        PlanDefinition.Assert.that(
                        "plandefinition-RuleFilters-1.0.0",
                        "NotReportable",
                        null
                )
                .withData("rule-filters/tests-NotReportable-bundle.json")
                .withLibrary("rule-filters/RuleFilters-1.0.0-bundle.json")
                .apply()
                .isEqualsTo("rule-filters/NotReportableBundle.json");
    }

    // In R5 ActivityDefinitions can no longer create Tasks which breaks this test
    @Test(enabled = false)
    public void testRuleFiltersReportable() {
        PlanDefinition.Assert.that(
                        "plandefinition-RuleFilters-1.0.0",
                        "Reportable",
                        null
                )
                .withData("rule-filters/tests-Reportable-bundle.json")
                .withLibrary("rule-filters/RuleFilters-1.0.0-bundle.json")
                .apply()
                .isEqualsTo("rule-filters/ReportableBundle.json");
    }
}