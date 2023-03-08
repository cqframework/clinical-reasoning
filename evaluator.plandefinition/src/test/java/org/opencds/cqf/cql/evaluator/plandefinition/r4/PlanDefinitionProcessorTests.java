package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

import java.util.List;

import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.fhir.api.Repository;
import org.testng.annotations.Test;

public class PlanDefinitionProcessorTests extends PlanDefinition {
  private Repository createRepositoryForPath(String path) {
    var data = new FhirRepository(this.getClass(), List.of(path + "/tests"), false);
    var content = new FhirRepository(this.getClass(), List.of(path + "/content"), false);
    var terminology =
        new FhirRepository(this.getClass(), List.of(path + "/vocabulary/ValueSet"), false);

    return Repositories.proxy(data, content, terminology);
  }

  @Test
  public void testChildRoutineVisit() {
    var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
    var patientID = "Patient/ChildRoutine-Reportable";
    var data = "child-routine-visit/child_routine_visit_patient.json";
    var content = "child-routine-visit/child_routine_visit_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).apply()
        .isEqualsTo("child-routine-visit/child_routine_visit_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).applyR5()
        .isEqualsTo("child-routine-visit/child_routine_visit_bundle.json");
  }

  @Test
  public void testAncVisitContainedActivityDefinition() {
    var planDefinitionID = "AncVisit-PlanDefinition";
    var patientID = "Patient/TEST_PATIENT";
    var data = "anc-visit/anc_visit_patient.json";
    var content = "anc-visit/anc_visit_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).apply().isEqualsTo("anc-visit/anc_visit_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).applyR5().isEqualsTo("anc-visit/anc_visit_bundle.json");
  }

  @Test
  public void testANCDT17() {
    var planDefinitionID = "ANCDT17";
    var patientID = "Patient/5946f880-b197-400b-9caa-a3c661d23041";
    var encounterID = "Encounter/helloworld-patient-1-encounter-1";
    var repository = createRepositoryForPath("anc-dak");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withRepository(repository)
        .apply().isEqualsTo("anc-dak/tests/CarePlan-ANCDT17.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withRepository(repository)
        .applyR5().isEqualsTo("anc-dak/tests/Bundle-ANCDT17.json");
  }

  @Test(enabled = false) // Need patient data to test this
  public void testECRWithFhirPath() {
    var planDefinitionID = "us-ecr-specification";
    var patientID = "helloworld-patient-1";
    var encounterID = "helloworld-patient-1-encounter-1";
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).apply()
        .isEqualsTo("tests/CarePlan-us-ecr-specification.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).applyR5()
        .isEqualsTo("tests/Bundle-us-ecr-specification.json");
  }

  @Test
  public void testHelloWorld() {
    var planDefinitionID = "hello-world-patient-view";
    var patientID = "helloworld-patient-1";
    var encounterID = "helloworld-patient-1-encounter-1";
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).apply()
        .isEqualsTo("tests/CarePlan-hello-world-patient-view.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).applyR5()
        .isEqualsTo("tests/Bundle-hello-world-patient-view.json");
  }

  @Test
  public void testOpioidRec10PatientView() {
    var planDefinitionID = "opioidcds-10-patient-view";
    var patientID = "example-rec-10-patient-view-POS-Cocaine-drugs";
    var encounterID = "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch";
    var repository = createRepositoryForPath("opioid-Rec10-patient-view");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withRepository(repository)
        .apply()
        .isEqualsTo("opioid-Rec10-patient-view/tests/CarePlan-opioid-Rec10-patient-view.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID).withRepository(repository)
        .applyR5()
        .isEqualsTo("opioid-Rec10-patient-view/tests/Bundle-opioid-Rec10-patient-view.json");
  }

  @Test
  public void testRuleFiltersNotReportable() {
    var planDefinitionID = "plandefinition-RuleFilters-1.0.0";
    var patientID = "NotReportable";
    var data = "rule-filters/tests-NotReportable-bundle.json";
    var content = "rule-filters/RuleFilters-1.0.0-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).apply().isEqualsTo("rule-filters/NotReportableCarePlan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).applyR5().isEqualsTo("rule-filters/NotReportableBundle.json");
  }

  @Test
  public void testRuleFiltersReportable() {
    var planDefinitionID = "plandefinition-RuleFilters-1.0.0";
    var patientID = "Reportable";
    var data = "rule-filters/tests-Reportable-bundle.json";
    var content = "rule-filters/RuleFilters-1.0.0-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).apply().isEqualsTo("rule-filters/ReportableCarePlan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).applyR5().isEqualsTo("rule-filters/ReportableBundle.json");
  }

  @Test
  public void testCDSHooksMultipleActions() {
    var planDefinitionID = "CdsHooksMultipleActions-PlanDefinition-1.0.0";
    var patientID = "patient-CdsHooksMultipleActions";
    var data = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_patient_data.json";
    var content = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).apply()
        .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withContent(content).applyR5()
        .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_bundle.json");
  }

  @Test
  public void testQuestionnairePrepopulate() {
    var planDefinitionID = "prepopulate";
    var patientID = "OPA-Patient1";
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withParameters(parameters).apply()
        .isEqualsTo("tests/CarePlan-prepopulate.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withParameters(parameters)
        .applyR5().isEqualsTo("tests/Bundle-prepopulate.json");
  }

  @Test
  public void testQuestionnairePrepopulate_NoLibrary() {
    var planDefinitionID = "prepopulate-noLibrary";
    var patientID = "OPA-Patient1";
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withParameters(parameters).apply()
        .isEqualsTo("tests/CarePlan-prepopulate-noLibrary.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withParameters(parameters)
        .applyR5().isEqualsTo("tests/Bundle-prepopulate-noLibrary.json");
  }

  @Test
  public void testQuestionnaireResponse() {
    var planDefinitionID = "prepopulate";
    var patientID = "OPA-Patient1";
    var data = "tests/QuestionnaireResponse-OutpatientPriorAuthorizationRequest-OPA-Patient1.json";
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withParameters(parameters).apply()
        .isEqualsTo("tests/CarePlan-extract-questionnaireresponse.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withAdditionalData(data)
        .withParameters(parameters).applyR5()
        .isEqualsTo("tests/Bundle-extract-questionnaireresponse.json");
  }

  @Test
  public void testGenerateQuestionnaire() {
    var planDefinitionID = "generate-questionnaire";
    var patientID = "OPA-Patient1";
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withParameters(parameters).apply()
        .isEqualsTo("tests/CarePlan-generate-questionnaire.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withParameters(parameters)
        .applyR5().isEqualsTo("tests/Bundle-generate-questionnaire.json");
  }
}
