package org.opencds.cqf.fhir.test.r4;

import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.fhir.api.Repository;

import com.google.common.collect.Lists;

public class TestRepository {
  public TestRepository() {}

  public Repository createRepository() {
    var data = new FhirRepository(this.getClass(), Lists.newArrayList("tests"), true);
    var content = new FhirRepository(this.getClass(), Lists.newArrayList("content"), true);
    var terminology =
        new FhirRepository(this.getClass(), Lists.newArrayList("vocabulary"), true);

    return Repositories.proxy(data, content, terminology);
  }

  public Repository createRepositoryForPath(String path) {
    var data = new FhirRepository(this.getClass(), Lists.newArrayList(path + "/tests"), true);
    var content = new FhirRepository(this.getClass(), Lists.newArrayList(path + "/content"), true);
    var terminology =
        new FhirRepository(this.getClass(), Lists.newArrayList(path + "/vocabulary"), true);

    return Repositories.proxy(data, content, terminology);
  }
}
