package org.opencds.cqf.cql.evaluator.activitydefinition.r5;

import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.testng.Assert;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class ActivityDefinitionProcessorTests {
  private static FhirContext fhirContext;
  private ActivityDefinitionProcessor activityDefinitionProcessor;

  /* Commenting this out until we have a ModelResolver for R5 */
  // @BeforeClass
  public void setup() {
    fhirContext = FhirContext.forCached(FhirVersionEnum.R5);
    FhirDal fhirDal = new MockFhirDal();
    activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, fhirDal);
  }

  @Test
  public void testActivityDefinitionApply() throws FHIRException {
    Assert.assertTrue(true);
    /* Commenting this out until we have a ModelResolver for R5 */
    // Endpoint contentEndpoint = new
    // Endpoint().setStatus(EndpointStatus.ACTIVE).setAddress("bundle-activityDefinitionTest.json")
    // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    // Endpoint terminologyEndpoint = new
    // Endpoint().setStatus(EndpointStatus.ACTIVE).setAddress("bundle-activityDefinitionTest.json")
    // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    // Endpoint dataEndpoint = new
    // Endpoint().setStatus(EndpointStatus.ACTIVE).setAddress("bundle-activityDefinitionTest.json")
    // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    // Object result = this.activityDefinitionProcessor.apply(new
    // IdType("activityDefinition-test"), "patient-1", null, null, null, null, null,
    // null, null, null, null, contentEndpoint, terminologyEndpoint, dataEndpoint);
    // Assert.assertTrue(result instanceof MedicationRequest);
    // MedicationRequest request = (MedicationRequest) result;
    // Assert.assertTrue(request.getDoNotPerform());
  }

}
