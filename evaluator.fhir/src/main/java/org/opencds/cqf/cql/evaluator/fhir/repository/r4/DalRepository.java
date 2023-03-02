package org.opencds.cqf.cql.evaluator.fhir.repository.r4;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.util.Resources;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;

public class DalRepository implements Repository {

  FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);
  private FhirDal fhirDal;

  public DalRepository(FhirDal fhirDal) {
    this.fhirDal = fhirDal;
  }


  @Override
  @SuppressWarnings("unchecked")
  public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
      Map<String, String> headers) {
    return (T) this.fhirDal.read(new IdType(resourceType.getSimpleName(), id.getIdPart()));
  }

  @Override
  public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'create'");
  }

  @Override
  public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters,
      Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'patch'");
  }

  @Override
  public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'update'");
  }

  @Override
  public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType,
      I id, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'delete'");
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType,
      Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters,
      Map<String, String> headers) {
    Bundle bundle = Resources.newResource(Bundle.class, UUID.randomUUID().toString());
    bundle.setType(Bundle.BundleType.SEARCHSET);

    if (searchParameters != null && searchParameters.containsKey("url")) {
      Iterable<IBaseResource> bundleResources =
          this.fhirDal.searchByUrl(resourceType.getSimpleName(),
              searchParameters.get("url").get(0).getValueAsQueryToken(context));
      bundleResources.forEach(resource -> bundle
          .addEntry(new Bundle.BundleEntryComponent().setResource((Resource) resource)));
    } else {
      Iterable<IBaseResource> bundleResources = this.fhirDal.search(resourceType.getSimpleName());
      bundleResources.forEach(resource -> bundle
          .addEntry(new Bundle.BundleEntryComponent().setResource((Resource) resource)));
    }

    return (B) bundle;
  }

  @Override
  public <B extends IBaseBundle> B link(Class<B> bundleType, String url,
      Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'link'");
  }

  @Override
  public <C extends IBaseConformance> C capabilities(Class<C> resourceType,
      Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'capabilities'");
  }

  @Override
  public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'transaction'");
  }

  @Override
  public <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters,
      Class<R> returnType, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'invoke'");
  }

  @Override
  public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters,
      Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'invoke'");
  }

  @Override
  public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
      Class<T> resourceType, String name, P parameters, Class<R> returnType,
      Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'invoke'");
  }

  @Override
  public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
      Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'invoke'");
  }

  @Override
  public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id,
      String name, P parameters, Class<R> returnType, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'invoke'");
  }

  @Override
  public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id, String name,
      P parameters, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'invoke'");
  }

  @Override
  public <B extends IBaseBundle, P extends IBaseParameters> B history(P parameters,
      Class<B> returnType, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'history'");
  }

  @Override
  public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
      Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'history'");
  }

  @Override
  public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(I id,
      P parameters, Class<B> returnType, Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'history'");
  }

}
