package org.opencds.cqf.fhir.cr.plandefinition;

import static org.opencds.cqf.fhir.cr.plandefinition.PlanDefinition.given;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

@Disabled
public class PlanDefinitionProcessorTests {

    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Test
    public void testChildRoutineVisit() {
        var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
        var patientID = "Patient/ChildRoutine-Reportable";
        var data = "child-routine-visit/child_routine_visit_patient.json";
        var content = "child-routine-visit/child_routine_visit_plan_definition.json";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApply()
                .isEqualsTo("r4/child-routine-visit/child_routine_visit_careplan.json");
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApplyR5()
                .isEqualsTo("r4/child-routine-visit/child_routine_visit_bundle.json");
    }

    @Test
    public void testAncVisitContainedActivityDefinition() {
        var planDefinitionID = "AncVisit-PlanDefinition";
        var patientID = "Patient/TEST_PATIENT";
        var data = "anc-visit/anc_visit_patient.json";
        var content = "anc-visit/anc_visit_plan_definition.json";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApply()
                .isEqualsTo("anc-visit/anc_visit_careplan.json");
        given().repositoryFor(fhirContextR4, content)
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApplyR5()
                .isEqualsTo("anc-visit/anc_visit_bundle.json");
    }

    @Test
    public void testANCDT17() {
        var planDefinitionID = "ANCDT17";
        var patientID = "Patient/5946f880-b197-400b-9caa-a3c661d23041";
        var encounterID = "Encounter/helloworld-patient-1-encounter-1";
        var repository = TestRepositoryFactory.createRepository(
                fhirContextR4, this.getClass(), "org/opencds/cqf/fhir/cr/plandefinition/r4/anc-dak");
        var parameters = parameters(part("encounter", "helloworld-patient-1-encounter-1"));
        given().repository(repository)
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .parameters(parameters)
                .thenApply()
                .isEqualsTo(new IdType("CarePlan", planDefinitionID));
        given().repository(repository)
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .parameters(parameters)
                .thenApplyR5()
                .isEqualsTo(new IdType("Bundle", planDefinitionID));
    }

    @Test
    public void testANCDT17WithElm() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId("ANCDT17")
                .data("anc-dak/data-bundle.json")
                .content("anc-dak/content-bundle.json")
                .terminology("anc-dak/terminology-bundle.json")
                .parameters(parameters(part("encounter", "ANCDT17-encounter")))
                .thenApply()
                .isEqualsTo("anc-dak/output-careplan.json");
    }

    @Test
    public void testFhirPath() {
        var planDefinitionID = "DischargeInstructionsPlan";
        var patientID = "Patient/Patient1";
        var practitionerID = "Practitioner/Practitioner1";
        var data = "tests/Bundle-DischargeInstructions-Patient-Data.json";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .practitionerId(practitionerID)
                .additionalData(data)
                .thenApplyR5()
                .hasCommunicationRequestPayload();
    }

    @Test
    public void testHelloWorld() {
        var planDefinitionID = "hello-world-patient-view";
        var patientID = "helloworld-patient-1";
        var encounterID = "helloworld-patient-1-encounter-1";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .thenApply()
                .isEqualsTo(new IdType("CarePlan", planDefinitionID));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .thenApplyR5()
                .isEqualsTo(new IdType("Bundle", planDefinitionID));
    }

    @Test
    public void testOpioidRec10PatientView() {
        var planDefinitionID = "opioidcds-10-patient-view";
        var patientID = "example-rec-10-patient-view-POS-Cocaine-drugs";
        var encounterID = "example-rec-10-patient-view-POS-Cocaine-drugs-prefetch";
        given().repositoryFor(fhirContextR4, "r4/opioid-Rec10-patient-view")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .thenApply()
                .isEqualsTo(new IdType("CarePlan", planDefinitionID));
        given().repositoryFor(fhirContextR4, "r4/opioid-Rec10-patient-view")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .thenApplyR5()
                .isEqualsTo(new IdType("Bundle", planDefinitionID));
    }

    @Test
    public void testCDSHooksMultipleActions() {
        var planDefinitionID = "CdsHooksMultipleActions-PlanDefinition-1.0.0";
        var patientID = "patient-CdsHooksMultipleActions";
        var data = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_patient_data.json";
        var content = "cds-hooks-multiple-actions/cds_hooks_multiple_actions_plan_definition.json";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApply()
                .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_careplan.json");
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApplyR5()
                .isEqualsTo("cds-hooks-multiple-actions/cds_hooks_multiple_actions_bundle.json");
    }

    @Test
    public void testQuestionnairePrepopulate() {
        var planDefinitionID = "prepopulate";
        var patientID = "OPA-Patient1";
        var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApply()
                .isEqualsTo(new IdType("CarePlan", "prepopulate"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .isEqualsTo(new IdType("Bundle", "prepopulate"));
    }

    @Test
    public void testQuestionnairePrepopulate_NoLibrary() {
        var planDefinitionID = "prepopulate-noLibrary";
        var patientID = "OPA-Patient1";
        var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApply()
                .hasOperationOutcome();
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .hasQuestionnaireOperationOutcome();
    }

    @Test
    public void testQuestionnaireResponse() {
        var planDefinitionID = "prepopulate";
        var patientID = "OPA-Patient1";
        var dataId = new IdType("QuestionnaireResponse", "OutpatientPriorAuthorizationRequest-OPA-Patient1");
        var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .additionalDataId(dataId)
                .parameters(parameters)
                .thenApply()
                .hasContained(4);
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .additionalDataId(dataId)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(4);
    }

    @Test
    public void testGenerateQuestionnaire() {
        var planDefinitionID = "generate-questionnaire";
        var patientID = "OPA-Patient1";
        var parameters = parameters(stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .isEqualsTo(new IdType("Bundle", "generate-questionnaire"));
    }

    @Test
    public void testASLPA1() {
        var planDefinitionID = "ASLPA1";
        var patientID = "positive";
        var parameters = parameters(
                stringPart("Service Request Id", "SleepStudy"),
                stringPart("Service Request Id", "SleepStudy2"),
                stringPart("Coverage Id", "Coverage-positive"));
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(2);
    }

    @Test
    public void testPackageASLPA1() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .planDefinitionId("ASLPA1")
                .thenPackage()
                .hasEntry(20);
    }
}
