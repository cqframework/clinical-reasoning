package org.opencds.cqf.fhir.cr.plandefinition;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.plandefinition.PlanDefinition.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.plandefinition.PlanDefinition.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyProcessor;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class PlanDefinitionProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Test
    void defaultSettings() {
        var repository =
                new IgRepository(fhirContextR4, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
        var processor = new PlanDefinitionProcessor(repository);
        assertNotNull(processor.evaluationSettings());
    }

    @Test
    void processor() {
        var repository =
                new IgRepository(fhirContextR5, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r5"));
        var modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R5);
        var activityProcessor = new org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyProcessor(
                repository, IRequestResolverFactory.getDefault(FhirVersionEnum.R5));
        var packageProcessor = new PackageProcessor(repository);
        var requestResolverFactory = IRequestResolverFactory.getDefault(FhirVersionEnum.R5);
        var processor = new PlanDefinitionProcessor(
                repository,
                EvaluationSettings.getDefault(),
                new ApplyProcessor(repository, modelResolver, activityProcessor),
                packageProcessor,
                activityProcessor,
                requestResolverFactory);
        assertNotNull(processor.evaluationSettings());
        var result = processor.apply(
                Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", "DischargeInstructionsPlan")),
                "Patient1",
                "Encounter1",
                "Practitioner1",
                "Organization1",
                null,
                null,
                null,
                null,
                null);
        assertNotNull(result);
    }

    @Test
    void applyNoSubjectThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            var planDefinitionID = "hello-world-patient-view";
            given().repositoryFor(fhirContextR4, "r4")
                    .when()
                    .planDefinitionId(planDefinitionID)
                    .thenApply();
        });
    }

    @Test
    void childRoutineVisitDstu3() {
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
    void childRoutineVisitR4() {
        var planDefinitionID = "ChildRoutineVisit-PlanDefinition-1.0.0";
        var patientID = "Patient/ChildRoutine-Reportable";
        var data = "r4/child-routine-visit/child_routine_visit_patient.json";
        var content = "r4/child-routine-visit/child_routine_visit_plan_definition.json";
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
    void childRoutineVisitR5() {
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
    void ancVisitContainedActivityDefinition() {
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
        given().repositoryFor(fhirContextR4, "r4")
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
    void ancdt17() {
        var planDefinitionId = "ANCDT17";
        var patientId = "Patient/5946f880-b197-400b-9caa-a3c661d23041";
        var encounterId = "Encounter/helloworld-patient-1-encounter-1";
        var repository = new IgRepository(
                fhirContextR4, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4/anc-dak"));
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
    void ancdt17WithElm() {
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
    void fhirPath() {
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
    void helloWorld() {
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
    void opioidRec10PatientView() {
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
    void cdsHooksMultipleActions() {
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
    void multipleActionsUsingSameActivity() {
        var planDefinitionID = "multi-action-activity";
        var patientID = "OPA-Patient1";
        var result = given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .thenApplyR5()
                .generatedBundle;
        var resources = BundleHelper.getEntryResources(result);
        var resource1 = (org.hl7.fhir.r4.model.CommunicationRequest) resources.get(1);
        var resource2 = (org.hl7.fhir.r4.model.CommunicationRequest) resources.get(2);
        assertNotNull(resource1);
        assertNotNull(resource2);
        assertNotEquals(resource1.getId(), resource2.getId());
    }

    @Test
    void questionnaireTask() {
        var planDefinitionID = "prepopulate";
        var patientID = "OPA-Patient1";
        var parameters = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .isEqualsTo(new org.hl7.fhir.r4.model.IdType("Bundle", "prepopulate"));
    }

    @Test
    void questionnaireResponseDstu3() {
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
    void questionnaireResponseR4() {
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
                .hasContained(2);
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .additionalDataId(dataId)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(2);
    }

    @Test
    void questionnaireResponseR5() {
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
                .hasEntry(2);
    }

    @Test
    void generateQuestionnaireDstu3() {
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
                .hasContained(4)
                .hasQuestionnaire();
    }

    @Test
    void generateQuestionnaireR4() {
        var planDefinitionID = "generate-questionnaire";
        var patientID = "OPA-Patient1";
        var parameters = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.stringPart("ClaimId", "OPA-Claim1"));
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .parameters(parameters)
                .thenApplyR5()
                .hasEntry(3)
                .hasQuestionnaire();
    }

    @Test
    void generateQuestionnaireSleepStudy() {
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
    void generateQuestionnaireR5() {
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
    void packageDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .planDefinitionId("generate-questionnaire")
                .thenPackage()
                .hasEntry(9);

        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .planDefinitionId("DischargeInstructionsPlan")
                .thenPackage()
                .hasEntry(1);
    }

    @Test
    void packageR4() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .planDefinitionId("ASLPA1")
                .thenPackage()
                .hasEntry(20);

        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId("DischargeInstructionsPlan")
                .isPut(Boolean.TRUE)
                .thenPackage()
                .hasEntry(2);
    }

    @Test
    void packageR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .planDefinitionId("generate-questionnaire")
                .thenPackage()
                .hasEntry(10);

        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .planDefinitionId("DischargeInstructionsPlan")
                .isPut(Boolean.TRUE)
                .thenPackage()
                .hasEntry(2);
    }

    @Test
    void nestedDefinitionErrorShouldReturnOperationOutcome() {
        var planDefinitionID = "test-nested-error-1";
        var patientID = "helloworld-patient-1";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .planDefinitionId(planDefinitionID)
                .subjectId(patientID)
                .thenApplyR5()
                .hasEntry(2)
                .entryHasOperationOutcome(1);
    }
}
