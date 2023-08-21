package org.opencds.cqf.fhir.utility.repository;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.client.Clients;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class Repositories {

  private Repositories() {
    // intentionally empty
  }

  public static ProxyRepository proxy(Repository data, Repository content, Repository terminology) {
    return new ProxyRepository(data, content, terminology);
  }

  private static IGenericClient getClient(FhirContext fhirContext, IBaseResource endpoint) {
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return Clients.forEndpoint(fhirContext, (org.hl7.fhir.dstu3.model.Endpoint) endpoint);
      case R4:
        return Clients.forEndpoint(fhirContext, (org.hl7.fhir.r4.model.Endpoint) endpoint);
      case R5:
        return Clients.forEndpoint(fhirContext, (org.hl7.fhir.r5.model.Endpoint) endpoint);
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", fhirContext));
    }
  }

  public static Repository proxy(Repository localRepository, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    Repository data = dataEndpoint == null ? null
        : new RestRepository(getClient(localRepository.fhirContext(), dataEndpoint));
    Repository content = contentEndpoint == null ? null
        : new RestRepository(getClient(localRepository.fhirContext(), contentEndpoint));
    Repository terminology = terminologyEndpoint == null ? null
        : new RestRepository(getClient(localRepository.fhirContext(), terminologyEndpoint));

    return new ProxyRepository(localRepository, data, content, terminology);
  }

  public static <T extends IBaseResource> IBaseBundle searchRepositoryWithPaging(
      FhirContext fhirContext,
      Repository repository, Class<T> resourceType,
      Map<String, List<IQueryParameterType>> searchParameters,
      Map<String, String> headers) {
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return org.opencds.cqf.fhir.utility.dstu3.SearchHelper
            .searchRepositoryWithPaging(repository, resourceType, searchParameters, headers);
      case R4:
        return org.opencds.cqf.fhir.utility.r4.SearchHelper
            .searchRepositoryWithPaging(repository, resourceType, searchParameters, headers);
      case R5:
        return org.opencds.cqf.fhir.utility.r5.SearchHelper
            .searchRepositoryWithPaging(repository, resourceType, searchParameters, headers);

      default:
        return null;
    }
  }
}
