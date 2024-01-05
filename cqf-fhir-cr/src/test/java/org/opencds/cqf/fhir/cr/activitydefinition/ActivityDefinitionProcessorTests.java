package org.opencds.cqf.fhir.cr.activitydefinition;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

@TestInstance(Lifecycle.PER_CLASS)
public class ActivityDefinitionProcessorTests {
    private Repository repositoryDstu3;
    private Repository repositoryR4;
    private Repository repositoryR5;
    private ActivityDefinitionProcessor activityDefinitionProcessorDstu3;
    private ActivityDefinitionProcessor activityDefinitionProcessorR4;
    private ActivityDefinitionProcessor activityDefinitionProcessorR5;

    private Repository createRepository(FhirContext fhirContext, String version) {
        return new IGFileStructureRepository(
                fhirContext,
                this.getClass()
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .getPath() + "org/opencds/cqf/fhir/cr/activitydefinition/" + version,
                IGLayoutMode.TYPE_PREFIX,
                EncodingEnum.JSON);
    }

    private ActivityDefinitionProcessor createProcessor(Repository repository) {
        return new ActivityDefinitionProcessor(repository);
    }

    @BeforeAll
    public void setup() {
        repositoryDstu3 = createRepository(FhirContext.forDstu3Cached(), "dstu3");
        activityDefinitionProcessorDstu3 = createProcessor(repositoryDstu3);
        repositoryR4 = createRepository(FhirContext.forR4Cached(), "r4");
        activityDefinitionProcessorR4 = createProcessor(repositoryR4);
        repositoryR5 = createRepository(FhirContext.forR5Cached(), "r5");
        activityDefinitionProcessorR5 = createProcessor(repositoryR5);
    }

    @Test
    public void testActivityDefinitionApplyDstu3() throws FHIRException {
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
        Assertions.assertTrue(result instanceof ProcedureRequest);
        var request = (ProcedureRequest) result;
        Assertions.assertTrue(request.getDoNotPerform());
    }

    @Test
    public void testActivityDefinitionApplyR4() throws FHIRException {
        var result = activityDefinitionProcessorR4.apply(
                Eithers.forMiddle3(Ids.newId(
                        activityDefinitionProcessorR4.fhirContext(), "ActivityDefinition", "activityDefinition-test")),
                "patient-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        Assertions.assertTrue(result instanceof org.hl7.fhir.r4.model.MedicationRequest);
        org.hl7.fhir.r4.model.MedicationRequest request = (org.hl7.fhir.r4.model.MedicationRequest) result;
        Assertions.assertTrue(request.getDoNotPerform());
    }

    @Test
    public void testDynamicValueWithNestedPathR4() {
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
        Assertions.assertTrue(result instanceof Task);
        var task = (Task) result;
        Assertions.assertTrue(task.hasInput());
        var input = task.getInput().get(0);
        Assertions.assertEquals(
                "collect-information", input.getType().getCoding().get(0).getCode());
        Assertions.assertEquals(
                "http://example.org/sdh/dtr/aslp/Questionnaire/ASLPA1",
                ((CanonicalType) input.getValue()).getValueAsString());
        var input2 = task.getInput().get(1);
        Assertions.assertEquals(
                "http://example.org/sdh/dtr/aslp/Questionnaire/ASLPA2",
                ((CanonicalType) input2.getValue()).getValueAsString());
    }

    @Test
    // @Disabled // Unable to load R5 packages and run CQL
    public void testActivityDefinitionApplyR5() throws FHIRException {
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
        Assertions.assertTrue(result instanceof org.hl7.fhir.r5.model.MedicationRequest);
        org.hl7.fhir.r5.model.MedicationRequest request = (org.hl7.fhir.r5.model.MedicationRequest) result;
        Assertions.assertTrue(request.getDoNotPerform());
    }
}
