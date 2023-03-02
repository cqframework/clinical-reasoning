package org.opencds.cqf.cql.evaluator.fhir.util;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.ProxyRepository;
import org.opencds.cqf.fhir.utility.RestRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class Repositories {

  private Repositories() {}

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

  // private static Repository getDalRepository(FhirContext fhirContext, FhirDal fhirDal) {
  // switch (fhirContext.getVersion().getVersion()) {
  // case DSTU3:
  // return new org.opencds.cqf.cql.evaluator.fhir.repository.dstu3.DalRepository(fhirDal);
  // case R4:
  // return new org.opencds.cqf.cql.evaluator.fhir.repository.r4.DalRepository(fhirDal);
  // case R5:
  // return new org.opencds.cqf.cql.evaluator.fhir.repository.r5.DalRepository(fhirDal);
  // default:
  // throw new IllegalArgumentException(
  // String.format("unsupported FHIR version: %s", fhirContext));
  // }
  // }

  public static Repository proxy(FhirContext fhirContext, Repository localRepository,
      IBaseResource dataEndpoint, IBaseResource contentEndpoint,
      IBaseResource terminologyEndpoint) {
    // var fhirDalRepository = getDalRepository(fhirContext, fhirDal);
    Repository data =
        dataEndpoint == null ? null : new RestRepository(getClient(fhirContext, dataEndpoint));
    Repository content = contentEndpoint == null ? null
        : new RestRepository(getClient(fhirContext, contentEndpoint));
    Repository terminology = terminologyEndpoint == null ? null
        : new RestRepository(getClient(fhirContext, terminologyEndpoint));

    return new ProxyRepository(localRepository, data, content, terminology);
  }
}
