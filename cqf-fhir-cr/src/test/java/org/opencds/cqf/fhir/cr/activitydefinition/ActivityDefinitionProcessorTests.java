package org.opencds.cqf.fhir.cr.activitydefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.Engines.EngineInitializationContext;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class ActivityDefinitionProcessorTests {
    private IRepository repositoryDstu3;
    private IRepository repositoryR4;
    private IRepository repositoryR5;
    private ActivityDefinitionProcessor activityDefinitionProcessorDstu3;
    private ActivityDefinitionProcessor activityDefinitionProcessorR4;
    private ActivityDefinitionProcessor activityDefinitionProcessorR5;

    @Mock
    LibraryEngine libraryEngine;

    private IRepository createRepository(FhirContext fhirContext, String version) {
        return new IgRepository(
                fhirContext, Path.of(getResourcePath(this.getClass()) + "/org/opencds/cqf/fhir/cr/shared/" + version));
    }

    private ActivityDefinitionProcessor createProcessor(IRepository repository) {
        var engineInitializationContext =
                new EngineInitializationContext(repository, NpmPackageLoader.DEFAULT, EvaluationSettings.getDefault());

        return new ActivityDefinitionProcessor(repository, engineInitializationContext);
    }

    @BeforeAll
    void setup() {
        repositoryDstu3 = createRepository(FhirContext.forDstu3Cached(), "dstu3");
        activityDefinitionProcessorDstu3 = createProcessor(repositoryDstu3);
        repositoryR4 = createRepository(FhirContext.forR4Cached(), "r4");
        activityDefinitionProcessorR4 = createProcessor(repositoryR4);
        repositoryR5 = createRepository(FhirContext.forR5Cached(), "r5");
        activityDefinitionProcessorR5 = createProcessor(repositoryR5);
    }

    @Test
    void testRequest() {
        var activityDefinition = new ActivityDefinition();
        doReturn(repositoryR4).when(libraryEngine).getRepository();
        var request = activityDefinitionProcessorR4.buildApplyRequest(
                Eithers.forRight3(activityDefinition),
                "patientId",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                libraryEngine);
        assertEquals("apply", request.getOperationName());
        assertEquals("patientId", request.getSubjectId().getIdPart());
        assertEquals(activityDefinition, request.getContextVariable());
    }

    @Test
    void applyNoSubjectThrowsException() {
        Either3<IPrimitiveType<String>, IIdType, IBaseResource> id = Eithers.forMiddle3(Ids.newId(
                activityDefinitionProcessorR4.fhirContext(), "ActivityDefinition", "activityDefinition-test"));
        assertThrows(IllegalArgumentException.class, () -> {
            activityDefinitionProcessorR4.apply(id, null, null, null, null, null, null, null, null, null);
        });
    }

    @Test
    void activityDefinitionApplyDstu3() throws FHIRException {
        var result = this.activityDefinitionProcessorDstu3.apply(
                Eithers.forMiddle3(Ids.newId(
                        activityDefinitionProcessorDstu3.fhirContext(),
                        "ActivityDefinition",
                        "activityDefinition-test")),
                "patient-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        assertInstanceOf(ProcedureRequest.class, result);
        var request = (ProcedureRequest) result;
        assertTrue(request.getDoNotPerform());
    }

    @Test
    void activityDefinitionApplyR4() throws FHIRException {
        var result = activityDefinitionProcessorR4.apply(
                Eithers.forMiddle3(Ids.newId(
                        activityDefinitionProcessorR4.fhirContext(), "ActivityDefinition", "activityDefinition-test")),
                "patient-1",
                "encounter-1",
                "practitioner-1",
                "organization-1",
                null,
                null,
                null,
                null,
                null);
        assertInstanceOf(MedicationRequest.class, result);
        org.hl7.fhir.r4.model.MedicationRequest request = (org.hl7.fhir.r4.model.MedicationRequest) result;
        assertTrue(request.getDoNotPerform());
    }

    @Test
    void dynamicValueWithNestedPathR4() {
        var result = activityDefinitionProcessorR4.apply(
                Eithers.forMiddle3(
                        Ids.newId(activityDefinitionProcessorR4.fhirContext(), "ActivityDefinition", "ASLPCrd")),
                "patient-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        assertInstanceOf(Task.class, result);
        var task = (Task) result;
        assertTrue(task.hasInput());
        var input = task.getInput().get(0);
        assertEquals("collect-information", input.getType().getCoding().get(0).getCode());
        assertEquals(
                "http://example.org/sdh/dtr/aslp/Questionnaire/ASLPA1",
                ((CanonicalType) input.getValue()).getValueAsString());
        var input2 = task.getInput().get(1);
        assertEquals(
                "http://example.org/sdh/dtr/aslp/Questionnaire/ASLPA2",
                ((CanonicalType) input2.getValue()).getValueAsString());
    }

    @Test
    void activityDefinitionApplyR5() throws FHIRException {
        var result = this.activityDefinitionProcessorR5.apply(
                Eithers.forMiddle3(Ids.newId(
                        activityDefinitionProcessorR5.fhirContext(), "ActivityDefinition", "medicationrequest-test")),
                "patient-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        assertInstanceOf(org.hl7.fhir.r5.model.MedicationRequest.class, result);
        org.hl7.fhir.r5.model.MedicationRequest request = (org.hl7.fhir.r5.model.MedicationRequest) result;
        assertTrue(request.getDoNotPerform());
    }

    @Test
    void unsupportedFhirVersion() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            IRequestResolverFactory.getDefault(FhirVersionEnum.R4B);
        });
    }

    @Test
    void sendMessageActivityWithFhirPath() {
        var result = activityDefinitionProcessorR4.apply(
                Eithers.forMiddle3(Ids.newId(
                        activityDefinitionProcessorR4.fhirContext(), "ActivityDefinition", "SendMessageActivity")),
                "Patient1",
                "Encounter1",
                "Practitioner1",
                null,
                null,
                null,
                null,
                null,
                null);
        assertNotNull(result);
        var request = (CommunicationRequest) result;
        assertTrue(request.hasPayload());
        assertFalse(request.hasContained());
    }

    @Test
    void generateReportActivityWithFhirPath() {
        var result = activityDefinitionProcessorR4.apply(
                Eithers.forMiddle3(Ids.newId(
                        activityDefinitionProcessorR4.fhirContext(), "ActivityDefinition", "GenerateReportActivity")),
                "Patient1",
                "Encounter1",
                "Practitioner1",
                null,
                null,
                null,
                null,
                null,
                null);
        assertNotNull(result);
        var task = (Task) result;
        assertTrue(task.hasInput());
        var input = task.getInput().get(0);
        assertEquals("generate-report", input.getType().getCoding().get(0).getCode());
        assertEquals(
                "http://hl7.org/fhir/uv/cpg/Measure/activity-example-generatereport",
                input.getValue().primitiveValue());
    }

    @Test
    void testRecordInferredObservationTask() {
        var result = activityDefinitionProcessorR4.apply(
                Eithers.forMiddle3(Ids.newId(
                        activityDefinitionProcessorR4.fhirContext(),
                        "ActivityDefinition",
                        "activity-example-recordinference-ad")),
                "Patient1",
                "Encounter1",
                "Practitioner1",
                null,
                null,
                null,
                null,
                null,
                null);
        assertNotNull(result);
        var task = (Task) result;
        assertTrue(task.hasContained());
        var contained = task.getContained().get(0);
        assertNotNull(contained);
        assertInstanceOf(Observation.class, contained);
        assertTrue(task.hasInput());
        var input = task.getInput().get(0);
        assertTrue(input.hasType());
        var inputRef = (Reference) input.getValue();
        assertNotNull(inputRef);
        assertEquals("#" + contained.getId(), inputRef.getReference());
    }
}
