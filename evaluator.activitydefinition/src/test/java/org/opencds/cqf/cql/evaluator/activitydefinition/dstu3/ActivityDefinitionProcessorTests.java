package org.opencds.cqf.cql.evaluator.activitydefinition.dstu3;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Endpoint.EndpointStatus;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class ActivityDefinitionProcessorTests {
  private static FhirContext fhirContext;
  private ActivityDefinitionProcessor activityDefinitionProcessor;

  @BeforeClass
  public void setup() {
    fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
    FhirDal fhirDal = new MockFhirDal();

    activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, fhirDal);
  }

  @Test
  public void testActivityDefinitionApply() throws FHIRException {
    Endpoint contentEndpoint = new Endpoint().setStatus(EndpointStatus.ACTIVE)
        .setAddress("bundle-activityDefinitionTest.json")
        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    Endpoint terminologyEndpoint = new Endpoint().setStatus(EndpointStatus.ACTIVE)
        .setAddress("bundle-activityDefinitionTest.json")
        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    Endpoint dataEndpoint = new Endpoint().setStatus(EndpointStatus.ACTIVE)
        .setAddress("bundle-activityDefinitionTest.json")
        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    var result = this.activityDefinitionProcessor.apply(new IdType("activityDefinition-test"),
        "patient-1", null, null, null, null, null, null, null, null, null, contentEndpoint,
        terminologyEndpoint, dataEndpoint);
    Assert.assertTrue(result instanceof ProcedureRequest);
    var request = (ProcedureRequest) result;
    Assert.assertTrue(request.getDoNotPerform());
  }

}
