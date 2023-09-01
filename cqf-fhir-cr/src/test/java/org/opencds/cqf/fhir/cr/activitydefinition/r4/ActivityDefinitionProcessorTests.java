package org.opencds.cqf.fhir.cr.activitydefinition.r4;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;

@TestInstance(Lifecycle.PER_CLASS)
public class ActivityDefinitionProcessorTests {
  private final FhirContext fhirContext = FhirContext.forR4Cached();

  private Repository repository;
  private ActivityDefinitionProcessor activityDefinitionProcessor;

  @BeforeAll
  public void setup() {
    repository = new IGFileStructureRepository(this.fhirContext,
        this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
            + "org/opencds/cqf/fhir/cr/activitydefinition/r4",
        IGLayoutMode.TYPE_PREFIX, EncodingEnum.JSON);
    activityDefinitionProcessor =
        new ActivityDefinitionProcessor(repository);
  }

  @Test
  public void testActivityDefinitionApply() throws FHIRException {
    var result = this.activityDefinitionProcessor.apply(
        new IdType("ActivityDefinition", "activityDefinition-test"), null,
        null, "patient-1", null, null, null, null, null, null, null, null);
    Assertions.assertTrue(result instanceof MedicationRequest);
    MedicationRequest request = (MedicationRequest) result;
    Assertions.assertTrue(request.getDoNotPerform());
  }

  @Test
  public void testDynamicValueWithNestedPath() {
    var result = this.activityDefinitionProcessor.apply(new IdType("ASLPCrd"), null,
        null, "patient-1", null, null, null, null, null, null, null, null);
    Assertions.assertTrue(result instanceof Task);
    var task = (Task) result;
    Assertions.assertTrue(task.hasInput());
    var input = task.getInput().get(0);
    Assertions.assertEquals("collect-information", input.getType().getCoding().get(0).getCode());
    Assertions.assertEquals("http://example.org/sdh/dtr/aslp/Questionnaire/ASLPA1",
        ((CanonicalType) input.getValue()).getValueAsString());
    var input2 = task.getInput().get(1);
    Assertions.assertEquals("http://example.org/sdh/dtr/aslp/Questionnaire/ASLPA2",
        ((CanonicalType) input2.getValue()).getValueAsString());
  }
}
