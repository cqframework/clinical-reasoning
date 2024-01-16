package org.opencds.cqf.fhir.cr.plandefinition;

import static org.opencds.cqf.fhir.cr.plandefinition.PlanDefinition.given;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;

public class PlanDefinitionProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Test
    public void testChildRoutineVisitDstu3() {
        var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
        var patientID = "Patient/ChildRoutine-Reportable";
        var data = "dstu3/child-routine-visit/child_routine_visit_patient.json";
        var content = "dstu3/child-routine-visit/child_routine_visit_plan_definition.json";
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApply()
                .isEqualsTo("dstu3/child-routine-visit/child_routine_visit_careplan.json");
    }

    @Test
    public void testChildRoutineVisitR4() {
        var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
        var patientID = "Patient/ChildRoutine-Reportable";
        var data = "r4/child-routine-visit/child_routine_visit_patient.json";
        var content = "r4/child-routine-visit/child_routine_visit_plan_definition.json";
        // given().repositoryFor(fhirContextR4, "r4")
        //         .when()
        //         .planDefinitionId(planDefinitionID)
        //         .subjectId(patientID)
        //         .data(data)
        //         .content(content)
        //         .terminology(content)
        //         .thenApply()
        //         .isEqualsTo("r4/child-routine-visit/child_routine_visit_careplan.json");
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
    public void testChildRoutineVisitR5() {
        var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
        var patientID = "Patient/ChildRoutine-Reportable";
        var data = "r5/child-routine-visit/child_routine_visit_patient.json";
        var content = "r5/child-routine-visit/child_routine_visit_plan_definition.json";
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApplyR5()
                .isEqualsTo("r5/child-routine-visit/child_routine_visit_bundle.json");
    }

    @Test
    public void testAncVisitContainedActivityDefinition() {
        var planDefinitionID = "AncVisit-PlanDefinition";
        var patientID = "Patient/TEST_PATIENT";
        var data = "r4/anc-visit/anc_visit_patient.json";
        var content = "r4/anc-visit/anc_visit_plan_definition.json";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApply()
                .isEqualsTo("r4/anc-visit/anc_visit_careplan.json");
        given().repositoryFor(fhirContextR4, content)
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApplyR5()
                .isEqualsTo("r4/anc-visit/anc_visit_bundle.json");
    }

    @Test
    public void testANCDT17() {
        var planDefinitionId = "ANCDT17";
        var patientId = "Patient/5946f880-b197-400b-9caa-a3c661d23041";
        var encounterId = "Encounter/helloworld-patient-1-encounter-1";
        var repository = TestRepositoryFactory.createRepository(
                fhirContextR4, this.getClass(), "org/opencds/cqf/fhir/cr/plandefinition/r4/anc-dak");
        var parameters = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.part("encounter", "helloworld-patient-1-encounter-1"));
        given().repository(repository)
                .when()
                .planDefinitionId(planDefinitionId)
                .subjectId(patientId)
                .encounterId(encounterId)
                .parameters(parameters)
                .thenApply()
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("CarePlan", planDefinitionId));
        given().repository(repository)
                .when()
                .planDefinitionId(planDefinitionId)
                .subjectId(patientId)
                .encounterId(encounterId)
                .parameters(parameters)
                .thenApplyR5()
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("Bundle", planDefinitionId));
    }

    @Test
    public void testANCDT17WithElm() {
        var patientId = "Patient/5946f880-b197-400b-9caa-a3c661d23041";
        var encounterId = "Encounter/ANCDT17-encounter";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId("ANCDT17")
                .subjectId(patientId)
                .encounterId(encounterId)
                .useServerData(false)
                .data("r4/anc-dak/data-bundle.json")
                .content("r4/anc-dak/content-bundle.json")
                .terminology("r4/anc-dak/terminology-bundle.json")
                .parameters(org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                        org.opencds.cqf.fhir.utility.r4.Parameters.part("encounter", "ANCDT17-encounter")))
                .thenApply()
                .isEqualsTo("r4/anc-dak/output-careplan.json");
    }

    @Test
    public void testFhirPath() {
        var planDefinitionID = "DischargeInstructionsPlan";
        var patientID = "Patient/Patient1";
        var practitionerID = "Practitioner/Practitioner1";
        var data = "r4/tests/Bundle-DischargeInstructions-Patient-Data.json";
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
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("CarePlan", planDefinitionID));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .thenApplyR5()
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("Bundle", planDefinitionID));
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
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("CarePlan", planDefinitionID));
        given().repositoryFor(fhirContextR4, "r4/opioid-Rec10-patient-view")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .encounterId(encounterID)
                .thenApplyR5()
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("Bundle", planDefinitionID));
    }

    @Test
    public void testCDSHooksMultipleActions() {
        var planDefinitionID = "CdsHooksMultipleActions-PlanDefinition-1.0.0";
        var patientID = "patient-CdsHooksMultipleActions";
        var data = "r4/cds-hooks-multiple-actions/cds_hooks_multiple_actions_patient_data.json";
        var content = "r4/cds-hooks-multiple-actions/cds_hooks_multiple_actions_plan_definition.json";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApply()
                .isEqualsTo("r4/cds-hooks-multiple-actions/cds_hooks_multiple_actions_careplan.json");
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .data(data)
                .content(content)
                .terminology(content)
                .thenApplyR5()
                .isEqualsTo("r4/cds-hooks-multiple-actions/cds_hooks_multiple_actions_bundle.json");
    }

    @Test
    @Disabled // Disabling this test for now.  This implementation of getting the prepopulate call included has never
    // been used and is likely to change
    public void testQuestionnairePrepopulate() {
        var planDefinitionID = "prepopulate";
        var patientID = "OPA-Patient1";
        var parameters = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApply()
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("CarePlan", "prepopulate"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("Bundle", "prepopulate"));
    }

    @Test
    @Disabled // Disabling this test for now.  This implementation of getting the prepopulate call included has never
    // been used and is likely to change
    public void testQuestionnairePrepopulate_NoLibrary() {
        var planDefinitionID = "prepopulate-noLibrary";
        var patientID = "OPA-Patient1";
        var parameters = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("ClaimId", "OPA-Claim1"));
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
    public void testQuestionnaireResponseDstu3() {
        // The content this test is using was intended for an old implementation of a custom prepopulate step that is no
        // longer used.  The content still works to test $extract but no Questionnaire is returned as originally
        // expected.
        var planDefinitionID = "prepopulate";
        var patientID = "OPA-Patient1";
        var data = "dstu3/extract-questionnaireresponse/patient-data.json";
        var content = "dstu3/prepopulate/prepopulate-content-bundle.json";
        var parameters = org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters(
                org.opencds.cqf.fhir.utility.dstu3.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .additionalData(data)
                .content(content)
                .parameters(parameters)
                .thenApply()
                .hasContained(3);
    }

    @Test
    public void testQuestionnaireResponseR4() {
        // The content this test is using was intended for an old implementation of a custom prepopulate step that is no
        // longer used.  The content still works to test $extract but no Questionnaire is returned as originally
        // expected.
        var planDefinitionID = "prepopulate";
        var patientID = "OPA-Patient1";
        var dataId = new org.hl7.fhir.r4.model.IdType(
                "QuestionnaireResponse", "OutpatientPriorAuthorizationRequest-OPA-Patient1");
        var parameters = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .additionalDataId(dataId)
                .parameters(parameters)
                .thenApply()
                .hasContained(3);
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .additionalDataId(dataId)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(3);
    }

    @Test
    public void testQuestionnaireResponseR5() {
        // The content this test is using was intended for an old implementation of a custom prepopulate step that is no
        // longer used.  The content still works to test $extract but no Questionnaire is returned as originally
        // expected.
        var planDefinitionID = "prepopulate";
        var patientID = "OPA-Patient1";
        var data = "r5/extract-questionnaireresponse/patient-data.json";
        var content = "r5/prepopulate/prepopulate-content-bundle.json";
        var parameters = org.opencds.cqf.fhir.utility.r5.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r5.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .additionalData(data)
                .content(content)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(3);
    }

    @Test
    public void testGenerateQuestionnaireDstu3() {
        var planDefinitionID = "generate-questionnaire";
        var patientID = "OPA-Patient1";
        var parameters = org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters(
                org.opencds.cqf.fhir.utility.dstu3.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApply()
                .hasContained(3)
                .hasQuestionnaire();
    }

    @Test
    public void testGenerateQuestionnaireR4() {
        var planDefinitionID = "ASLPA1";
        var patientID = "positive";
        var parameters = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("Service Request Id", "SleepStudy"),
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("Service Request Id", "SleepStudy2"),
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("Coverage Id", "Coverage-positive"));
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(2)
                .hasQuestionnaire();
    }

    @Test
    public void testGenerateQuestionnaireR5() {
        var planDefinitionID = "generate-questionnaire";
        var patientID = "OPA-Patient1";
        var parameters = org.opencds.cqf.fhir.utility.r5.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r5.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(3)
                .hasQuestionnaire();
    }

    @Test
    public void testPackageDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .planDefinitionId("generate-questionnaire")
                .thenPackage()
                .hasEntry(8);
    }

    @Test
    public void testPackageR4() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .planDefinitionId("ASLPA1")
                .thenPackage()
                .hasEntry(20);
    }

    @Test
    public void testPackageR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .planDefinitionId("generate-questionnaire")
                .thenPackage()
                .hasEntry(8);
    }
}
