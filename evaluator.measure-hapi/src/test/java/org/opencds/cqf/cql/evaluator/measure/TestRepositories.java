package org.opencds.cqf.cql.evaluator.measure;

import java.util.List;

import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Repositories;

public class TestRepositories {

  private TestRepositories() {

  }

  public static Repository createRepositoryForPath(String path) {
    var data = new FhirRepository(TestRepositories.class, List.of(path + "/tests"), true);
    var content = new FhirRepository(TestRepositories.class, List.of(path + "/resources"), true);
    var terminology =
        new FhirRepository(TestRepositories.class, List.of(path + "/vocabulary/valueset"), false);

    return Repositories.proxy(data, content, terminology);
  }

}
