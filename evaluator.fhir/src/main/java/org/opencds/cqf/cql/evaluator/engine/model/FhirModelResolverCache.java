package org.opencds.cqf.cql.evaluator.engine.model;

import static java.util.Objects.requireNonNull;

import java.util.EnumMap;
import java.util.Map;

import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.fhir.helper.r5.R5FhirModelResolver;

import ca.uhn.fhir.context.FhirVersionEnum;

public class FhirModelResolverCache {

  private FhirModelResolverCache() {
    // intentionally empty
  }

  private static Map<FhirVersionEnum, ModelResolver> cache =
      new EnumMap<>(FhirVersionEnum.class);


  public static ModelResolver resolverForVersion(FhirVersionEnum fhirVersionEnum) {
    requireNonNull(fhirVersionEnum, "fhirVersionEnum can not be null");
    if (!cache.containsKey(fhirVersionEnum)) {
      ModelResolver resolver = null;
      switch (fhirVersionEnum) {
        case DSTU2:
          resolver = new DynamicModelResolver(new Dstu2FhirModelResolver());
          break;
        case DSTU3:
          resolver = new DynamicModelResolver(new Dstu3FhirModelResolver());
          break;
        case R4:
          resolver = new DynamicModelResolver(new R4FhirModelResolver());
          break;
        case R5:
          resolver = new DynamicModelResolver(new R5FhirModelResolver());
          break;
        default:
          throw new IllegalArgumentException("unknown or unsupported FHIR version");
      }

      cache.put(fhirVersionEnum, resolver);
    }

    return cache.get(fhirVersionEnum);
  }
}
