package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.cql.evaluator.fhir.test.TestRepositoryFactory;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class PlanDefinitionProcessorTests {

  private final FhirContext fhirContext = FhirContext.forR4Cached();

  @Test
  public void testChildRoutineVisit() {
    var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
    var patientID = "Patient/ChildRoutine-Reportable";
    var data = "child-routine-visit/child_routine_visit_patient.json";
    var content = "child-routine-visit/child_routine_visit_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).apply()
        .isEqualsTo("child-routine-visit/child_routine_visit_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).applyR5()
        .isEqualsTo("child-routine-visit/child_routine_visit_bundle.json");
  }

  @Test
  public void testAncVisitContainedActivityDefinition() {
    var planDefinitionID = "AncVisit-PlanDefinition";
    var patientID = "Patient/TEST_PATIENT";
    var data = "anc-visit/anc_visit_patient.json";
    var content = "anc-visit/anc_visit_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).apply()
        .isEqualsTo("anc-visit/anc_visit_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).applyR5()
        .isEqualsTo("anc-visit/anc_visit_bundle.json");
  }

  @Test
  public void testANCDT17() {
    var planDefinitionID = "ANCDT17";
    var patientID = "Patient/5946f880-b197-400b-9caa-a3c661d23041";
    var encounterID = "Encounter/helloworld-patient-1-encounter-1";
    var repository =
        TestRepositoryFactory.createRepository(fhirContext, this.getClass(), "anc-dak");
    var parameters = parameters(part("encounter", "helloworld-patient-1-encounter-1"));
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withParameters(parameters)
        .withExpectedCarePlanId(new IdType("CarePlan", "ANCDT17"))
        .apply().isEqualsToExpected();
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withParameters(parameters)
        .withExpectedBundleId(new IdType("Bundle", "ANCDT17"))
        .applyR5().isEqualsToExpected();
  }

  // I think this is failing because we've enabled CQL only for the short term.
  @Test(enabled = false)
  public void testANCDT17WithElm() {
    PlanDefinition.Assert.that(
        "ANCDT17",
        "Patient/5946f880-b197-400b-9caa-a3c661d23041",
        "Encounter/ANCDT17-encounter")
        .withData("anc-dak/data-bundle.json")
        .withContent("anc-dak/content-bundle.json")
        .withTerminology("anc-dak/terminology-bundle.json")
        .apply()
        .isEqualsTo("anc-dak/output-careplan.json");
  }

  @Test(enabled = false) // Need patient data to test this
  public void testECRWithFhirPath() {
    var planDefinitionID = "us-ecr-specification";
    var patientID = "helloworld-patient-1";
    var encounterID = "helloworld-patient-1-encounter-1";
    var repository = TestRepositoryFactory.createRepository(fhirContext, this.getClass());
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withExpectedCarePlanId(new IdType("CarePlan", "us-ecr-specification")).apply()
        .isEqualsToExpected();
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withExpectedBundleId(new IdType("Bundle", "us-ecr-specification")).applyR5()
        .isEqualsToExpected();
  }

  @Test
  public void testHelloWorld() {
    var planDefinitionID = "hello-world-patient-view";
    var patientID = "helloworld-patient-1";
    var encounterID = "helloworld-patient-1-encounter-1";
    var repository = TestRepositoryFactory.createRepository(fhirContext, this.getClass());
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withExpectedCarePlanId(new IdType("CarePlan", "hello-world-patient-view")).apply()
        .isEqualsToExpected();
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withExpectedBundleId(new IdType("Bundle", "hello-world-patient-view")).applyR5()
        .isEqualsToExpected();
  }

  // @Test - Have to punt on this one. Need Brenin to take a look.
  public void testOpioidRec10PatientView() {
    var planDefinitionID = "opioidcds-10-patient-view";
    var patientID = "example-rec-10-patient-view-POS-Cocaine-drugs";
    var encounterID = "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch";

    var repository = TestRepositoryFactory.createRepository(fhirContext, this.getClass(),
        "opioid-Rec10-patient-view");
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withExpectedCarePlanId(new IdType("CarePlan", "opioidcds-10-patient-view")).apply()
        .isEqualsToExpected();
    PlanDefinition.Assert.that(planDefinitionID, patientID, encounterID)
        .withRepository(repository)
        .withExpectedBundleId(new IdType("Bundle", "opioidcds-10-patient-view")).applyR5()
        .isEqualsToExpected();
  }

  @Test
  public void testRuleFiltersNotReportable() {
    var planDefinitionID = "plandefinition-RuleFilters-1.0.0";
    var patientID = "NotReportable";
    var data = "rule-filters/tests-NotReportable-bundle.json";
    var content = "rule-filters/RuleFilters-1.0.0-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).apply()
        .isEqualsTo("rule-filters/NotReportableCarePlan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).applyR5()
        .isEqualsTo("rule-filters/NotReportableBundle.json");
  }

  @Test
  public void testRuleFiltersReportable() {
    var planDefinitionID = "plandefinition-RuleFilters-1.0.0";
    var patientID = "Reportable";
    var data = "rule-filters/tests-Reportable-bundle.json";
    var content = "rule-filters/RuleFilters-1.0.0-bundle.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).apply()
        .isEqualsTo("rule-filters/ReportableCarePlan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).applyR5()
        .isEqualsTo("rule-filters/ReportableBundle.json");
  }

  @Test
  public void testCDSHooksMultipleActions() {
    var planDefinitionID = "CdsHooksMultipleActions-PlanDefinition-1.0.0";
    var patientID = "patient-CdsHooksMultipleActions";
    var data = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_patient_data.json";
    var content = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_plan_definition.json";
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).apply()
        .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_careplan.json");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withData(data)
        .withContent(content).withTerminology(content).applyR5()
        .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_bundle.json");
  }

  @Test
  public void testQuestionnairePrepopulate() {
    var planDefinitionID = "prepopulate";
    var patientID = "OPA-Patient1";
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    var repository = TestRepositoryFactory.createRepository(fhirContext, this.getClass());
    PlanDefinition.Assert.that(planDefinitionID, patientID, null)
        .withRepository(repository).withParameters(parameters)
        .withExpectedCarePlanId(new IdType("CarePlan", "prepopulate"))
        .apply().isEqualsToExpected();
    PlanDefinition.Assert.that(planDefinitionID, patientID, null)
        .withRepository(repository).withParameters(parameters)
        .withExpectedBundleId(new IdType("Bundle", "prepopulate"))
        .applyR5().isEqualsToExpected();
  }

  @Test
  public void testQuestionnairePrepopulate_NoLibrary() {
    var planDefinitionID = "prepopulate-noLibrary";
    var patientID = "OPA-Patient1";
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    var repository = TestRepositoryFactory.createRepository(fhirContext, this.getClass());
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withRepository(repository)
        .withParameters(parameters)
        .apply().hasOperationOutcome();
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withRepository(repository)
        .withParameters(parameters)
        .applyR5().hasQuestionnaireOperationOutcome();
  }

  @Test
  public void testQuestionnaireResponse() {
    var planDefinitionID = "prepopulate";
    var patientID = "OPA-Patient1";
    var dataId =
        new IdType("QuestionnaireResponse", "OutpatientPriorAuthorizationRequest-OPA-Patient1");
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    var repository = TestRepositoryFactory.createRepository(fhirContext, this.getClass());
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withRepository(repository)
        .withAdditionalDataId(dataId).withParameters(parameters)
        .apply().hasContained(6);
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withRepository(repository)
        .withAdditionalDataId(dataId).withParameters(parameters)
        .applyR5().hasEntry(6);
  }

  @Test
  public void testGenerateQuestionnaire() {
    var planDefinitionID = "generate-questionnaire";
    var patientID = "OPA-Patient1";
    var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
    var repository = TestRepositoryFactory.createRepository(fhirContext, this.getClass());
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withRepository(repository)
        .withParameters(parameters)
        .withExpectedCarePlanId(new IdType("CarePlan", "generate-questionnaire"))
        .apply().isEqualsToExpected();
    PlanDefinition.Assert.that(planDefinitionID, patientID, null).withRepository(repository)
        .withParameters(parameters)
        .withExpectedBundleId(new IdType("Bundle", "generate-questionnaire"))
        .applyR5().isEqualsToExpected();
  }

  @Test
  public void testASLPA1() {
    var planDefinitionID = "ASLPA1";
    var patientID = "positive";
    var parameters = parameters(stringPart("Service Request Id", "SleepStudy"),
        stringPart("Service Request Id", "SleepStudy2"),
        stringPart("Coverage Id", "Coverage-positive"));
    var repository =
        TestRepositoryFactory.createRepository(fhirContext, this.getClass(), "pa-aslp");
    PlanDefinition.Assert.that(planDefinitionID, patientID, null)
        .withParameters(parameters).withRepository(repository).applyR5()
        .hasEntry(2);
  }

  @Test
  public void testPackageASLPA1() {
    var planDefinitionID = "ASLPA1";
    var repository =
        TestRepositoryFactory.createRepository(fhirContext, this.getClass(), "pa-aslp");
    PlanDefinition.Assert.that(planDefinitionID, null, null)
        .withRepository(repository).packagePlanDefinition().hasEntry(20);
  }
  
  @Test
  public void testANCDT17PlanDefinition() {
      PlanDefinition.Assert.that(
              "ANCDT17",
              "Patient/5946f880-b197-400b-9caa-a3c661d23041",
              "Encounter/ANCDT17-encounter"
          )
          .withData("anc-dak/data-bundle.json")
          .withContent("anc-dak/content-bundle.json")
          .withTerminology("anc-dak/terminology-bundle.json")
          .apply()
          .isEqualsTo("anc-dak/output-careplan.json");
  }
}
