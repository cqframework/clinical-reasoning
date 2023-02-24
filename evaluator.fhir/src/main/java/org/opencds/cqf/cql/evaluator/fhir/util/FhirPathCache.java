package org.opencds.cqf.cql.evaluator.fhir.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;

public class FhirPathCache {

  private FhirPathCache() {}

  private static final Map<FhirVersionEnum, IFhirPath> CACHE = new ConcurrentHashMap<>();

  public static IFhirPath cachedForContext(FhirContext fhirContext) {
    return CACHE.computeIfAbsent(fhirContext.getVersion().getVersion(),
        x -> fhirContext.newFhirPath());
  }

  public static IFhirPath cachedForVersion(FhirVersionEnum fhirVersionEnum) {
    return CACHE.computeIfAbsent(fhirVersionEnum, x -> x.newContext().newFhirPath());
  }

}
