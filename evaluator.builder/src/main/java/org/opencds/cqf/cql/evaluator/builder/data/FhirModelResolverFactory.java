package org.opencds.cqf.cql.evaluator.builder.data;

import static java.util.Objects.requireNonNull;

import javax.inject.Named;

import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.util.VersionUtilities;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;

import ca.uhn.fhir.context.FhirVersionEnum;

@Named
public class FhirModelResolverFactory
    implements org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory {
  @Override
  public ModelResolver create(String version) {
    requireNonNull(version, "version can not be null");

    FhirVersionEnum fhirVersionEnum = VersionUtilities.enumForVersion(version);
    return this.fhirModelResolverForVersion(fhirVersionEnum);
  }

  protected ModelResolver fhirModelResolverForVersion(FhirVersionEnum fhirVersionEnum) {
    return FhirModelResolverCache.resolverForVersion(fhirVersionEnum);
  }

  @Override
  public String getModelUri() {
    return Constants.FHIR_MODEL_URI;
  }
}
