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
    return createRepository(fhirContext, clazz, "");
  }

  public static Repository createRepository(FhirContext fhirContext, Class<?> clazz,
      String path) {
    return createRepository(fhirContext, clazz, path, IGLayoutMode.TYPE_PREFIX);
  }

  public static Repository createRepository(FhirContext fhirContext, Class<?> clazz,
      String path, IGLayoutMode layoutMode) {
    return new IGFileStructureRepository(fhirContext,
        clazz.getProtectionDomain().getCodeSource().getLocation().getPath() + path,
        layoutMode, EncodingEnum.JSON);
  }
}
