package org.opencds.cqf.cql.evaluator.fhir.test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.fhir.util.Ids;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;


public class TestRepository implements Repository {

  private final Map<IIdType, IBaseResource> resourceMap;

  protected final FhirContext context;
  protected final IFhirPath fhirPath;

  private final Random random;

  private TestRepository(FhirContext fhirContext) {
    this.context = fhirContext;
    this.fhirPath = FhirPathCache.cachedForContext(context);
    resourceMap = new LinkedHashMap<>();
    random = new Random();
  }

  public TestRepository(FhirContext fhirContext, Class<?> clazz, List<String> directoryList,
      boolean recursive) {
    this(fhirContext);


    FhirResourceLoader resourceLoader =
        new FhirResourceLoader(context, clazz, directoryList, recursive);
    resourceLoader.getResources().forEach(resource -> resourceMap.put(
        resource.getIdElement().toUnqualifiedVersionless(),
        resource));
  }

  public TestRepository(FhirContext fhirContext, IBaseBundle bundle) {
    this(fhirContext);
    BundleUtil.toListOfResources(this.context, bundle).forEach(resource -> resourceMap.put(
        resource.getIdElement().toUnqualifiedVersionless(),
        resource));

  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
      Map<String, String> headers) {

    if (!id.hasResourceType()) {
      throw new IllegalArgumentException(
          "The TestRepository requires all ids to have a resource type set.");
    }

    var r = resourceMap.get(id.toUnqualifiedVersionless());
    if (r == null) {
      throw new ResourceNotFoundException("Resource not found with id " + id.toString());
    }

    return (T) r;
  }

  @Override
  public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
    MethodOutcome methodOutcome = new MethodOutcome();

    if (IBaseResource.class.isAssignableFrom(resource.getClass())) {

      IBaseResource iBaseResource = resource;

      var theId = Ids.newId(context, iBaseResource.getIdElement().getResourceType(),
          iBaseResource.getIdElement().getIdPart());
      if (!resourceMap.containsKey(theId)) {
        resourceMap.put(theId, iBaseResource);
        methodOutcome.setCreated(true);
      } else {
        Long nextLong = random.nextLong();
        theId = Ids.newId(context, iBaseResource.getIdElement().getResourceType(),
            iBaseResource.getIdElement().getIdPart() + nextLong.toString());
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

      IBaseResource iBaseResource = resource;

      var theId = Ids.newId(context, iBaseResource.getIdElement().getResourceType(),
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

    var theId = Ids.newId(context, resourceType.getSimpleName(), id.getIdPart());

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

    List<IBaseResource> resourceList = new ArrayList<>();
    for (IBaseResource resource : resourceMap.values()) {
      if (resource.getIdElement() != null
          && resource.getIdElement().getResourceType().equals(resourceType.getSimpleName())) {
        resourceList.add(resource);
      }
    }

    BundleBuilder b = new BundleBuilder(context);



    if (searchParameters != null && searchParameters.containsKey("url")) {
      Iterable<IBaseResource> bundleResources = searchByUrl(resourceList,
          searchParameters.get("url").get(0).getValueAsQueryToken(context));
      bundleResources.forEach(b::addCollectionEntry);
    } else {
      resourceList.forEach(b::addCollectionEntry);
    }

    b.setType(BundleTypeEnum.SEARCHSET.getCode());

    return (B) b.getBundle();
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

        case R5:
          var r5String =
              this.fhirPath.evaluateFirst(resource, "url", org.hl7.fhir.r5.model.UriType.class);
          if (r5String.isPresent() && r5String.get().getValue().equals(url)) {
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
    return this.context;
  }
}
