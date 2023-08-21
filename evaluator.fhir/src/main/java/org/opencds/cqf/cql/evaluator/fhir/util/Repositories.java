package org.opencds.cqf.cql.evaluator.fhir.util;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;

public class Repositories {

  private Repositories() {}

  public static <T extends IBaseResource> IBaseBundle searchRepositoryWithPaging(
      FhirContext fhirContext,
      Repository repository, Class<T> resourceType,
      Map<String, List<IQueryParameterType>> searchParameters,
      Map<String, String> headers) {
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return org.opencds.cqf.cql.evaluator.fhir.util.dstu3.SearchHelper
            .searchRepositoryWithPaging(repository, resourceType, searchParameters, headers);
      case R4:
        return org.opencds.cqf.cql.evaluator.fhir.util.r4.SearchHelper
            .searchRepositoryWithPaging(repository, resourceType, searchParameters, headers);
      case R5:
        return org.opencds.cqf.cql.evaluator.fhir.util.r5.SearchHelper
            .searchRepositoryWithPaging(repository, resourceType, searchParameters, headers);

      default:
        return null;
    }
  }
}
