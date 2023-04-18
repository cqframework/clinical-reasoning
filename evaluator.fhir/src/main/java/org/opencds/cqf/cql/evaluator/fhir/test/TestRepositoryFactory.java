package org.opencds.cqf.cql.evaluator.fhir.test;

import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.fhir.api.Repository;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;

public class TestRepositoryFactory {
  private TestRepositoryFactory() {
    // intentionally empty
  }

  public static Repository createRepository(FhirContext fhirContext, Class<?> clazz) {
    var data = new TestRepository(fhirContext, clazz, Lists.newArrayList("tests"), true);
    var content = new TestRepository(fhirContext, clazz, Lists.newArrayList("resources"), true);
    var terminology =
        new TestRepository(fhirContext, clazz, Lists.newArrayList("vocabulary"), true);

    return Repositories.proxy(data, content, terminology);
  }

  public static Repository createRepository(FhirContext fhirContext, Class<?> clazz,
      String path) {
    var data =
        new TestRepository(fhirContext, clazz, Lists.newArrayList(path + "/tests"), true);
    var content =
        new TestRepository(fhirContext, clazz, Lists.newArrayList(path + "/resources"), true);
    var terminology =
        new TestRepository(fhirContext, clazz, Lists.newArrayList(path + "/vocabulary"),
            true);

    return Repositories.proxy(data, content, terminology);
  }
}
