package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import org.testng.annotations.Test;

public class PlanDefinitionProcessorTests extends PlanDefinitionProcessorTestBase {
    @Test
    public void testChildRoutineVisit() {
        test(
                "child-routine-visit/child_routine_visit_patient.json",
                "child-routine-visit/child_routine_visit_plan_definition.json",
                "ChildRoutineVisit-PlanDefinition-1.0.0",
                "Patient/ChildRoutine-Reportable",
                null,
                "child-routine-visit/child_routine_visit_careplan.json"
        );
    }

    @Test
    public void testHelloWorld() {
        test(
                "hello-world/hello-world-patient-data.json",
                "hello-world/hello-world-patient-view-bundle.json",
                "hello-world-patient-view",
                "helloworld-patient-1",
                "helloworld-patient-1-encounter-1",
                "hello-world/hello-world-careplan.json"
        );
    }

    @Test
    public void testOpioidRec10PatientView() {
        test(
                "opioid-Rec10-patient-view/opioid-Rec10-patient-view-patient-data.json",
                "opioid-Rec10-patient-view/opioid-Rec10-patient-view-bundle.json",
                "opioidcds-10-patient-view",
                "example-rec-10-patient-view-POS-Cocaine-drugs",
                "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch",
                "opioid-Rec10-patient-view/opioid-Rec10-patient-view-careplan.json"
        );
    }

    @Test
    public void testRuleFiltersNotReportable() {
        test(
                "rule-filters/tests-NotReportable-bundle.json",
                "rule-filters/RuleFilters-1.0.0-bundle.json",
                "plandefinition-RuleFilters-1.0.0",
                "NotReportable",
                null,
                "rule-filters/NotReportableCarePlan.json"
        );
    }

    @Test
    public void testRuleFiltersReportable() {
        test(
                "rule-filters/tests-Reportable-bundle.json",
                "rule-filters/RuleFilters-1.0.0-bundle.json",
                "plandefinition-RuleFilters-1.0.0",
                "Reportable",
                null,
                "rule-filters/ReportableCarePlan.json"
        );
    }
}