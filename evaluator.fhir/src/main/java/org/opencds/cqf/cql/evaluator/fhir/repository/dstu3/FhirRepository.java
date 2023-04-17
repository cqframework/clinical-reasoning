package org.opencds.cqf.cql.evaluator.fhir.repository.dstu3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirResourceLoader;
import org.opencds.cqf.cql.evaluator.fhir.util.Resources;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleUtil;


public class FhirRepository implements Repository {

  FhirContext context = FhirContext.forCached(FhirVersionEnum.DSTU3);
  IFhirPath fhirPath = FhirPathCache.cachedForContext(context);

  private Map<IdType, IBaseResource> resourceMap;

  private Random random;

  public FhirRepository(Class<?> clazz, List<String> directoryList, boolean recursive) {
    FhirResourceLoader resourceLoader =
        new FhirResourceLoader(context, clazz, directoryList, recursive);
    List<IBaseResource> list = resourceLoader.getResources();

    resourceMap = new LinkedHashMap<>();
    random = new Random();

    list.forEach(resource -> resourceMap.put(new IdType(resource.getIdElement().getResourceType(),
        resource.getIdElement().getIdPart()), resource));
  }

  public FhirRepository(Bundle bundle) {
    resourceMap = new LinkedHashMap<>();
    random = new Random();

    BundleUtil.toListOfResources(this.context, bundle).forEach(resource -> resourceMap.put(
        new IdType(resource.getIdElement().getResourceType(), resource.getIdElement().getIdPart()),
        resource));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
      Map<String, String> headers) {
    IdType theId = new IdType(resourceType.getSimpleName(), id.getIdPart());
    if (resourceMap.containsKey(theId)) {
      return (T) resourceMap.get(theId);
    }
    throw new ResourceNotFoundException("Resource not found with id " + theId);
  }

  @Override
  public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
    MethodOutcome methodOutcome = new MethodOutcome();

    if (IBaseResource.class.isAssignableFrom(resource.getClass())) {

      var iBaseResource = (IBaseResource) resource;

      var theId = new IdType(iBaseResource.getIdElement().getResourceType(),
          iBaseResource.getIdElement().getIdPart());
      if (!resourceMap.containsKey(theId)) {
        resourceMap.put(theId, iBaseResource);
        methodOutcome.setCreated(true);
      } else {
        Long nextLong = random.nextLong();
        theId = new IdType(iBaseResource.getIdElement().getResourceType(),
            iBaseResource.getIdElement().getIdPart(), nextLong.toString());
        resourceMap.put(theId, iBaseResource);
        methodOutcome.setCreated(true);
      }

    } else {
      // IBaseResource.class is not super class of resource
      methodOutcome.setStatusCode(400);
    }
    return methodOutcome;
  }

  @Override
  public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters,
      Map<String, String> headers) {
    return null;
  }

  @Override
  public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
    MethodOutcome methodOutcome = new MethodOutcome();

    if (IBaseResource.class.isAssignableFrom(resource.getClass())) {

      var iBaseResource = (IBaseResource) resource;

      var theId = new IdType(iBaseResource.getIdElement().getResourceType(),
          iBaseResource.getIdElement().getIdPart());
      if (resourceMap.containsKey(theId)) {
        resourceMap.put(theId, iBaseResource);
      } else {
        resourceMap.put(theId, iBaseResource);
        methodOutcome.setCreated(true);
      }
    } else {
      // IBaseResource.class is not super class of resource
      methodOutcome.setStatusCode(400);
    }
    return methodOutcome;
  }

  @Override
  public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType,
      I id, Map<String, String> headers) {
    MethodOutcome methodOutcome = new MethodOutcome();

    IdType theId = new IdType(resourceType.getSimpleName(), id.getIdPart());

    if (resourceMap.containsKey(theId)) {
      resourceMap.remove(theId);
    } else {
      throw new ResourceNotFoundException("Resource not found with id " + theId);
    }
    return methodOutcome;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType,
      Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters,
      Map<String, String> headers) {

    Bundle bundle = Resources.newResource(Bundle.class, UUID.randomUUID().toString());
    bundle.setType(Bundle.BundleType.SEARCHSET);

    List<IBaseResource> resourceList = new ArrayList<>();
    for (IBaseResource resource : resourceMap.values()) {
      if (resource.getIdElement() != null
          && resource.getIdElement().getResourceType().equals(resourceType.getSimpleName())) {
        resourceList.add(resource);
      }
    }

    if (searchParameters != null && searchParameters.containsKey("url")) {
      Iterable<IBaseResource> bundleResources = searchByUrl(resourceList,
          searchParameters.get("url").get(0).getValueAsQueryToken(context));
      bundleResources.forEach(resource -> bundle
          .addEntry(new Bundle.BundleEntryComponent().setResource((Resource) resource)));
    } else {
      resourceList.forEach(resource -> bundle
          .addEntry(new Bundle.BundleEntryComponent().setResource((Resource) resource)));
    }

    return (B) bundle;
  }

  private Iterable<IBaseResource> searchByUrl(List<IBaseResource> resources, String url) {

    List<IBaseResource> returnList = new ArrayList<>();
    for (IBaseResource resource : resources) {
      switch (this.context.getVersion().getVersion()) {
        case DSTU3:
          var dstu3String =
              this.fhirPath.evaluateFirst(resource, "url", org.hl7.fhir.dstu3.model.UriType.class);
          if (dstu3String.isPresent() && dstu3String.get().getValue().equals(url)) {
            returnList.add(resource);
          }
          break;

        case R4:
          var r4String =
              this.fhirPath.evaluateFirst(resource, "url", org.hl7.fhir.r4.model.UriType.class);
          if (r4String.isPresent() && r4String.get().getValue().equals(url)) {
            returnList.add(resource);
          }
          break;

        default:
          throw new IllegalArgumentException(
              String.format("Unsupported FHIR version %s", this.context.getVersion().getVersion()));
      }
    }

    return returnList;
  }

  @Override
  public <B extends IBaseBundle> B link(Class<B> bundleType, String url,
      Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
    return null;
  }

  @Override
  public <C extends IBaseConformance> C capabilities(Class<C> resourceType,
      Map<String, String> headers) {
    return null;
  }


  @Override
  public <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters,
      Class<R> returnType, Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters,
      Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
      Class<T> resourceType, String name, P parameters, Class<R> returnType,
      Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
      Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id,
      String name, P parameters, Class<R> returnType, Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id, String name,
      P parameters, Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <B extends IBaseBundle, P extends IBaseParameters> B history(P parameters,
      Class<B> returnType, Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
      Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(I id,
      P parameters, Class<B> returnType, Map<String, String> headers) {
    throw new NotImplementedException();
  }

  @Override
  public FhirContext fhirContext() {
    return context;
  }
}
