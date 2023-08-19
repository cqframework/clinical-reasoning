package org.opencds.cqf.fhir.utility.repository;

import org.opencds.cqf.fhir.api.Repository;

public class Repositories {

  private Repositories() {
    // intentionally empty
  }

  public static ProxyRepository proxy(Repository data, Repository content, Repository terminology) {
    return new ProxyRepository(data, content, terminology);
  }
}
