package org.opencds.cqf.fhir.test;

import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;

public class TestRepositoryFactory {
  private TestRepositoryFactory() {
    // intentionally empty
  }

  public static Repository createRepository(FhirContext fhirContext, Class<?> clazz) {
    var data = new InMemoryFhirRepository(fhirContext, clazz, Lists.newArrayList("tests"), true);
    var content =
        new InMemoryFhirRepository(fhirContext, clazz, Lists.newArrayList("resources"), true);
    var terminology =
        new InMemoryFhirRepository(fhirContext, clazz, Lists.newArrayList("vocabulary"), true);

    return org.opencds.cqf.fhir.utility.repository.Repositories.proxy(data, content, terminology);
  }

  public static Repository createRepository(FhirContext fhirContext, Class<?> clazz,
      String path) {
    var data =
        new InMemoryFhirRepository(fhirContext, clazz, Lists.newArrayList(path + "/tests"), true);
    var content =
        new InMemoryFhirRepository(fhirContext, clazz, Lists.newArrayList(path + "/resources"),
            true);
    var terminology =
        new InMemoryFhirRepository(fhirContext, clazz, Lists.newArrayList(path + "/vocabulary"),
            true);

    return org.opencds.cqf.fhir.utility.repository.Repositories.proxy(data, content, terminology);
  }
}
