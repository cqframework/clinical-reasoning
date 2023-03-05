package org.opencds.cqf.cql.evaluator.activitydefinition.dstu3;

import java.util.List;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.evaluator.fhir.repository.dstu3.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class ActivityDefinitionProcessorTests {
  private static FhirContext fhirContext;
  private Repository repository;
  private ActivityDefinitionProcessor activityDefinitionProcessor;

  @BeforeClass
  public void setup() {
    fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);

    var data = new FhirRepository(this.getClass(), List.of("tests"), false);
    var content = new FhirRepository(this.getClass(), List.of("content/"), false);
    var terminology = new FhirRepository(this.getClass(),
        List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);

    repository = Repositories.proxy(data, content, terminology);
    activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, repository);
  }

  @Test
  public void testActivityDefinitionApply() throws FHIRException {
    var libraryEngine = new LibraryEngine(fhirContext, repository);

    var result = this.activityDefinitionProcessor.apply(new IdType("activityDefinition-test"),
        "patient-1", null, null, null, null, null, null, null, null, null, libraryEngine);
    Assert.assertTrue(result instanceof ProcedureRequest);
    var request = (ProcedureRequest) result;
    Assert.assertTrue(request.getDoNotPerform());
  }

}
