package org.opencds.cqf.cql.evaluator.activitydefinition.r5;

import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.MedicationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

import ca.uhn.fhir.context.FhirContext;

public class ActivityDefinitionProcessorTests {
  private static final FhirContext fhirContext = FhirContext.forR5Cached();

  private static final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

  private Repository repository;
  private ActivityDefinitionProcessor activityDefinitionProcessor;


  @BeforeAll
  public void setup() {
    var data = new InMemoryFhirRepository(fhirContext, this.getClass(), List.of("tests"), false);
    var content =
        new InMemoryFhirRepository(fhirContext, this.getClass(), List.of("resources/"), false);
    var terminology = new InMemoryFhirRepository(fhirContext, this.getClass(),
        List.of("vocabulary/CodeSystem/", "vocabulary/ValueSet/"), false);

    repository = Repositories.proxy(data, content, terminology);
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

}
