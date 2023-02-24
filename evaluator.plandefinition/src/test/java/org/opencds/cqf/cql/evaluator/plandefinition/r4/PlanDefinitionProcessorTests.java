package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import org.hl7.fhir.r4.model.Parameters;
import org.testng.annotations.Test;

public class PlanDefinitionProcessorTests extends PlanDefinition {
  @Test
  public void testChildRoutineVisit() {
    var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
    var patientID = "Patient/ChildRoutine-Reportable";
    var data = "child-routine-visit/child_routine_visit_patient.json";
    var library = "child-routine-visit/child_routine_visit_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).apply()
        .isEqualsTo("child-routine-visit/child_routine_visit_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).applyR5()
        .isEqualsTo("child-routine-visit/child_routine_visit_bundle.json");
  }

  @Test
  public void testAncVisitContainedActivityDefinition() {
    var planDefinitionID = "AncVisit-PlanDefinition";
    var patientID = "Patient/TEST_PATIENT";
    var data = "anc-visit/anc_visit_patient.json";
    var library = "anc-visit/anc_visit_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).apply().isEqualsTo("anc-visit/anc_visit_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).applyR5().isEqualsTo("anc-visit/anc_visit_bundle.json");
  }

  @Test
  public void testHelloWorld() {
    var planDefinitionID = "hello-world-patient-view";
    var patientID = "helloworld-patient-1";
    var encounterID = "helloworld-patient-1-encounter-1";
    var data = "hello-world/hello-world-patient-data.json";
    var library = "hello-world/hello-world-patient-view-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withData(data)
        .withLibrary(library).apply().isEqualsTo("hello-world/hello-world-careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withData(data)
        .withLibrary(library).applyR5().isEqualsTo("hello-world/hello-world-bundle.json");
  }

  @Test
  public void testOpioidRec10PatientView() {
    /*
     * NOTE: All dynamicValues with the path equaling action.extension have been removed from the
     * plandefinition until the issue in the link https://github.com/DBCG/cqf-ruler/issues/539 has
     * been resolved.
     */
    var planDefinitionID = "opioidcds-10-patient-view";
    var patientID = "example-rec-10-patient-view-POS-Cocaine-drugs";
    var encounterID = "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch";
    var data = "opioid-Rec10-patient-view/opioid-Rec10-patient-view-patient-data.json";
    var library = "opioid-Rec10-patient-view/opioid-Rec10-patient-view-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withData(data)
        .withLibrary(library).apply()
        .isEqualsTo("opioid-Rec10-patient-view/opioid-Rec10-patient-view-careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withData(data)
        .withLibrary(library).applyR5()
        .isEqualsTo("opioid-Rec10-patient-view/opioid-Rec10-patient-view-result.json");
  }

  @Test
  public void testRuleFiltersNotReportable() {
    var planDefinitionID = "plandefinition-RuleFilters-1.0.0";
    var patientID = "NotReportable";
    var data = "rule-filters/tests-NotReportable-bundle.json";
    var library = "rule-filters/RuleFilters-1.0.0-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).apply().isEqualsTo("rule-filters/NotReportableCarePlan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).applyR5().isEqualsTo("rule-filters/NotReportableBundle.json");
  }

  @Test
  public void testRuleFiltersReportable() {
    var planDefinitionID = "plandefinition-RuleFilters-1.0.0";
    var patientID = "Reportable";
    var data = "rule-filters/tests-Reportable-bundle.json";
    var library = "rule-filters/RuleFilters-1.0.0-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).apply().isEqualsTo("rule-filters/ReportableCarePlan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).applyR5().isEqualsTo("rule-filters/ReportableBundle.json");
  }

  @Test
  public void testCDSHooksMultipleActions() {
    var planDefinitionID = "CdsHooksMultipleActions-PlanDefinition-1.0.0";
    var patientID = "patient-CdsHooksMultipleActions";
    var data = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_patient_data.json";
    var library = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).apply()
        .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).applyR5()
        .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_bundle.json");
  }

  @Test
  public void testQuestionnairePrepopulate() {
    var planDefinitionID = "prepopulate";
    var patientID = "OPA-Patient1";
    var data = "prepopulate/prepopulate-patient-data.json";
    var library = "prepopulate/prepopulate-content-bundle.json";
    var parameters = new Parameters().addParameter("ClaimId", "OPA-Claim1");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).apply()
        .isEqualsTo("prepopulate/prepopulate-careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).applyR5()
        .isEqualsTo("prepopulate/prepopulate-bundle.json");
  }

  @Test
  public void testQuestionnairePrepopulate_NoLibrary() {
    var planDefinitionID = "prepopulate";
    var patientID = "OPA-Patient1";
    var data = "prepopulate/prepopulate-patient-data.json";
    var library = "prepopulate/prepopulate-content-bundle-noLibrary.json";
    var parameters = new Parameters().addParameter("ClaimId", "OPA-Claim1");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).apply()
        .isEqualsTo("prepopulate/prepopulate-careplan-noLibrary.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).applyR5()
        .isEqualsTo("prepopulate/prepopulate-bundle-noLibrary.json");
  }

  @Test
  public void testQuestionnaireResponse() {
    var planDefinitionID = "prepopulate";
    var patientID = "OPA-Patient1";
    var data = "extract-questionnaireresponse/patient-data.json";
    var library = "prepopulate/prepopulate-content-bundle.json";
    var parameters = new Parameters().addParameter("ClaimId", "OPA-Claim1");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).apply()
        .isEqualsTo("extract-questionnaireresponse/careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).applyR5()
        .isEqualsTo("extract-questionnaireresponse/bundle.json");
  }

  @Test
  public void testGenerateQuestionnaire() {
    var planDefinitionID = "generate-questionnaire";
    var patientID = "OPA-Patient1";
    var data = "generate-questionnaire/patient-data.json";
    var library = "generate-questionnaire/content-bundle.json";
    var parameters = new Parameters().addParameter("ClaimId", "OPA-Claim1");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).apply()
        .isEqualsTo("generate-questionnaire/careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withLibrary(library).withParameters(parameters).applyR5()
        .isEqualsTo("generate-questionnaire/bundle.json");
  }
}
