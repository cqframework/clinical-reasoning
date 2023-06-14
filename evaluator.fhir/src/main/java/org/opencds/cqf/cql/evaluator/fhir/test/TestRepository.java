package org.opencds.cqf.cql.evaluator.fhir.test;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.evaluator.fhir.repository.InMemoryFhirRepository;

import ca.uhn.fhir.context.FhirContext;

/**
 * This class has been deprecated. Use InMemoryFhirRepository instead.
 */
public class TestRepository extends InMemoryFhirRepository {

  public TestRepository(FhirContext context) {
    super(context);
  }

  public TestRepository(FhirContext context, Class<?> clazz,
      List<String> directoryList, boolean recursive) {
    super(context, clazz, directoryList, recursive);
  }

  public TestRepository(FhirContext fhirContext, IBaseBundle bundle) {
    super(fhirContext, bundle);
  }
}
