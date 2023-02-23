package org.opencds.cqf.fhir.utility;

import org.opencds.cqf.fhir.api.Repository;

public class Repositories {

  public static ProxyRepository proxy(Repository data, Repository content, Repository terminology) {
    return new ProxyRepository(data, content, terminology);
  }
}
