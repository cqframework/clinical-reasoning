package org.opencds.cqf.fhir.cr.activitydefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Paths;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@TestInstance(Lifecycle.PER_CLASS)
public class ActivityDefinitionProcessorTests {
    private Repository repositoryDstu3;
    private Repository repositoryR4;
    private Repository repositoryR5;
    private ActivityDefinitionProcessor activityDefinitionProcessorDstu3;
    private ActivityDefinitionProcessor activityDefinitionProcessorR4;
    private ActivityDefinitionProcessor activityDefinitionProcessorR5;

    private Repository createRepository(FhirContext fhirContext, String version) {
        return new IgRepository(
                fhirContext,
                Paths.get(getResourcePath(this.getClass()) + "/org/opencds/cqf/fhir/cr/activitydefinition/" + version));
    }

    private ActivityDefinitionProcessor createProcessor(Repository repository) {
        return new ActivityDefinitionProcessor(repository);
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
    void testActivityDefinitionApplyDstu3() throws FHIRException {
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
        assertTrue(result instanceof ProcedureRequest);
        var request = (ProcedureRequest) result;
        assertTrue(request.getDoNotPerform());
    }

    @Test
    void testActivityDefinitionApplyR4() throws FHIRException {
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
        assertTrue(result instanceof org.hl7.fhir.r4.model.MedicationRequest);
        org.hl7.fhir.r4.model.MedicationRequest request = (org.hl7.fhir.r4.model.MedicationRequest) result;
        assertTrue(request.getDoNotPerform());
    }

    @Test
    void testDynamicValueWithNestedPathR4() {
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
        assertTrue(result instanceof Task);
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
    // @Disabled // Unable to load R5 packages and run CQL
    void testActivityDefinitionApplyR5() throws FHIRException {
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
        assertTrue(result instanceof org.hl7.fhir.r5.model.MedicationRequest);
        org.hl7.fhir.r5.model.MedicationRequest request = (org.hl7.fhir.r5.model.MedicationRequest) result;
        assertTrue(request.getDoNotPerform());
    }

    @Test
    void testUnsupportedFhirVersion() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            IRequestResolverFactory.getDefault(FhirVersionEnum.R4B);
        });
    }

    @Test
    void testSendMessageActivityWithFhirPath() {
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
    void testGenerateReportActivityWithFhirPath() {
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
        Assertions.assertTrue(task.hasInput());
        var input = task.getInput().get(0);
        assertEquals("generate-report", input.getType().getCoding().get(0).getCode());
        assertEquals(
                "http://hl7.org/fhir/uv/cpg/Measure/activity-example-generatereport",
                input.getValue().primitiveValue());
    }
}
