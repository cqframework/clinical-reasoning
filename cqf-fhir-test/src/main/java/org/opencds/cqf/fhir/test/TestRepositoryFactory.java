package org.opencds.cqf.fhir.test;

import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;

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
    return new IGFileStructureRepository(fhirContext,
        clazz.getProtectionDomain().getCodeSource().getLocation().getPath() + path,
        IGLayoutMode.TYPE_PREFIX, EncodingEnum.JSON);
  }
}
