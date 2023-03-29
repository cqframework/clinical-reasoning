package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.List;

import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Repositories;

public abstract class BaseMeasureProcessorTestB {

  protected final Repository repository;

  protected BaseMeasureProcessorTestB(String path) {
    this.repository = createRepositoryForPath(path);
  }

  private Repository createRepositoryForPath(String path) {
    var data = new FhirRepository(this.getClass(), List.of(path + "/tests"), true);
    var content = new FhirRepository(this.getClass(), List.of(path + "/resources"), true);
    var terminology =
        new FhirRepository(this.getClass(), List.of(path + "/vocabulary/valueset"), false);

    return Repositories.proxy(data, content, terminology);
  }
}
