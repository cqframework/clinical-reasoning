package org.opencds.cqf.cql.evaluator.activitydefinition.r4;

import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
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
    fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    FhirDal fhirDal = new MockFhirDal();
    activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, fhirDal);
  }

  @Test
  public void testActivityDefinitionApply() throws FHIRException {
    // Endpoint contentEndpoint = new Endpoint().setStatus(EndpointStatus.ACTIVE)
    // .setAddress("bundle-activityDefinitionTest.json")
    // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    // Endpoint terminologyEndpoint = new Endpoint().setStatus(EndpointStatus.ACTIVE)
    // .setAddress("bundle-activityDefinitionTest.json")
    // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    // Endpoint dataEndpoint = new Endpoint().setStatus(EndpointStatus.ACTIVE)
    // .setAddress("bundle-activityDefinitionTest.json")
    // .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

    // Object result = this.activityDefinitionProcessor.apply(new IdType("activityDefinition-test"),
    // "patient-1", null, null, null, null, null, null, null, null, null, contentEndpoint,
    // terminologyEndpoint, dataEndpoint);

    FhirRepository data = new FhirRepository(this.getClass(), List.of("res/tests"), false);
    FhirRepository content = new FhirRepository(this.getClass(), List.of("res/content/"), false);
    FhirRepository terminology = new FhirRepository(this.getClass(),
        List.of("res/vocabulary/CodeSystem/", "res/vocabulary/ValueSet/"), false);

    var repository = Repositories.proxy(data, content, terminology);
    var libraryEngine = new LibraryEngine(fhirContext, repository);

    Object result = this.activityDefinitionProcessor.apply(new IdType("activityDefinition-test"),
        "patient-1", null, null, null, null, null, null, null, null, null, libraryEngine);
    Assert.assertTrue(result instanceof MedicationRequest);
    MedicationRequest request = (MedicationRequest) result;
    Assert.assertTrue(request.getDoNotPerform());
  }

}
