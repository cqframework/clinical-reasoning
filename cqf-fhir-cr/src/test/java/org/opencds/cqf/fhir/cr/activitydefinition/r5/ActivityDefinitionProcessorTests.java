package org.opencds.cqf.fhir.cr.activitydefinition.r5;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.MedicationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.activitydefinition.r5.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;

@TestInstance(Lifecycle.PER_CLASS)
public class ActivityDefinitionProcessorTests {
  private final FhirContext fhirContext = FhirContext.forR5Cached();

  private Repository repository;
  private ActivityDefinitionProcessor activityDefinitionProcessor;


  @BeforeAll
  public void setup() {
    repository = new IGFileStructureRepository(this.fhirContext,
        this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
            + "org/opencds/cqf/fhir/cr/activitydefinition/r5",
        IGLayoutMode.TYPE_PREFIX, EncodingEnum.JSON);
    activityDefinitionProcessor =
        new ActivityDefinitionProcessor(repository);
  }

  @Test
  @Disabled // Unable to load R5 packages and run CQL
  public void testActivityDefinitionApply() throws FHIRException {
    var result = this.activityDefinitionProcessor.apply(
        new IdType("ActivityDefinition", "activityDefinition-test"), null,
        null, "patient-1", null, null, null, null, null, null, null, null);
    Assertions.assertTrue(result instanceof MedicationRequest);
    MedicationRequest request = (MedicationRequest) result;
    Assertions.assertTrue(request.getDoNotPerform());
  }

}
