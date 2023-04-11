package org.opencds.cqf.fhir.test.r4;

import java.util.List;

import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.fhir.api.Repository;

public class TestRepository {
  public TestRepository() {}

  public Repository createRepository() {
    var data = new FhirRepository(this.getClass(), List.of("tests"), true);
    var content = new FhirRepository(this.getClass(), List.of("content"), true);
    var terminology =
        new FhirRepository(this.getClass(), List.of("vocabulary"), true);

    return Repositories.proxy(data, content, terminology);
  }

  public Repository createRepositoryForPath(String path) {
    var data = new FhirRepository(this.getClass(), List.of(path + "/tests"), true);
    var content = new FhirRepository(this.getClass(), List.of(path + "/content"), true);
    var terminology =
        new FhirRepository(this.getClass(), List.of(path + "/vocabulary"), true);

    return Repositories.proxy(data, content, terminology);
  }
}
