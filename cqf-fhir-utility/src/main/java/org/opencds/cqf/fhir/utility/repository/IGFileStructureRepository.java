package org.opencds.cqf.fhir.utility.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.util.BundleBuilder;

import static java.util.Objects.requireNonNull;

/**
 * This class implements the Repository interface on onto a directory structure that matches the
 * standard IG layout.
 */
public class IGFileStructureRepository implements Repository {

  private final FhirContext fhirContext;
  private final String root;
  private final IGLayoutMode layoutMode;
  private final EncodingEnum encodingEnum;
  private final IParser parser;

  private static final Map<ResourceCategory, String> categoryDirectories =
      new ImmutableMap.Builder<ResourceCategory, String>()
          .put(ResourceCategory.CONTENT, "resources")
          .put(ResourceCategory.DATA, "tests")
          .put(ResourceCategory.TERMINOLOGY, "vocabulary")
          .build();

  private static final Map<EncodingEnum, String> fileExtensions =
      new ImmutableMap.Builder<EncodingEnum, String>()
          .put(EncodingEnum.JSON, ".json")
          .put(EncodingEnum.XML, ".xml")
          .put(EncodingEnum.RDF, ".rdf")
          .build();

  private static IParser parserForEncoding(FhirContext fhirContext, EncodingEnum encodingEnum) {
    switch (encodingEnum) {
      case JSON:
        return fhirContext.newJsonParser();
      case XML:
        return fhirContext.newXmlParser();
      case RDF:
        return fhirContext.newRDFParser();
      case NDJSON:
      default:
        throw new IllegalArgumentException("NDJSON is not supported");
    }
  }

  public IGFileStructureRepository(FhirContext fhirContext, String root) {
    this(fhirContext, root, IGLayoutMode.DIRECTORY, EncodingEnum.JSON);
  }

  public IGFileStructureRepository(FhirContext fhirContext, String root, IGLayoutMode layoutMode,
      EncodingEnum encodingEnum) {
    this.fhirContext = fhirContext;
    this.root = root;
    this.layoutMode = layoutMode;
    this.encodingEnum = encodingEnum;
    this.parser = parserForEncoding(fhirContext, encodingEnum);
  }

  protected <T extends IBaseResource, I extends IIdType> String locationForResource(
      Class<T> resourceType, I id) {
    var directory = directoryForType(resourceType);
    return directory + "/"
        + fileNameForLayoutAndEncoding(resourceType.getSimpleName(), id.getIdPart());
  }

  protected String fileNameForLayoutAndEncoding(
      String resourceType, String resourceId) {
    var name = resourceId + fileExtensions.get(this.encodingEnum);
    if (layoutMode == IGLayoutMode.DIRECTORY) {
      // TODO: case sensitivity!!
      return resourceType.toLowerCase() + "/" + name;
    } else {
      return resourceType + "-" + name;
    }
  }

  protected <T extends IBaseResource> String directoryForType(Class<T> resourceType) {
    var category = ResourceCategory.forType(resourceType.getSimpleName());
    var directory = categoryDirectories.get(category);

    // TODO: what the heck is the path separator?
    return (root.endsWith("/") ? root : root + "/") + directory;
  }

  protected <T extends IBaseResource> String directoryForResource(Class<T> resourceType) {
    var directory = directoryForType(resourceType);
    if (layoutMode == IGLayoutMode.DIRECTORY) {
      return directory + "/" + resourceType.getSimpleName().toLowerCase();
    } else {
      return directory;
    }
  }

  protected <T extends IBaseResource, I extends IIdType> T readLocation(
      Class<T> resourceClass, String location, I idType) {
    T r = null;
    try {
      r = parser.parseResource(resourceClass, new FileInputStream(location));
    } catch (FileNotFoundException e) {
      throw new ResourceNotFoundException(idType);
    }

    if (!r.getIdElement().hasIdPart() || !r.getIdElement().getIdPart().equals(idType.getIdPart())) {
      throw new InternalErrorException(String.format(
          "Expected to read Resource with id %s, but found resource %s at location %s. The IG structure may not be consistent",
          idType.toString(), r.getIdElement().toString(), location));
    }

    return handleLibrary(r, location);
  }

  @SuppressWarnings("unchecked")
  protected <T extends IBaseResource> T handleLibrary(T resource, String location) {
    if (resource.fhirType().equals("Library")) {
      String cqlLocation;
      switch (fhirContext.getVersion().getVersion()) {
        case DSTU3:
          cqlLocation =
              org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil.getCqlLocation(resource);
          if (cqlLocation != null) {
            resource = (T) org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil
                .addData(resource, getCqlContent(location, cqlLocation));
          }
          break;
        case R4:
          cqlLocation =
              org.opencds.cqf.fhir.utility.r4.AttachmentUtil.getCqlLocation(resource);
          if (cqlLocation != null) {
            resource = (T) org.opencds.cqf.fhir.utility.r4.AttachmentUtil.addData(resource,
                getCqlContent(location, cqlLocation));
          }
          break;
        case R5:
          cqlLocation =
              org.opencds.cqf.fhir.utility.r5.AttachmentUtil.getCqlLocation(resource);
          if (cqlLocation != null) {
            resource = (T) org.opencds.cqf.fhir.utility.r5.AttachmentUtil.addData(resource,
                getCqlContent(location, cqlLocation));
          }
          break;
        default:
          throw new IllegalArgumentException(
              String.format("unsupported FHIR version: %s", fhirContext));
      }
    }
    return resource;
  }

  protected String getCqlContent(String rootPath, String relativePath) {
    var p = Paths.get(rootPath).getParent().resolve(relativePath).normalize();
    try {
      return Files.asCharSource(p.toFile(), StandardCharsets.UTF_8).read();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected <T extends IBaseResource> MethodOutcome writeLocation(
      T resource, String location) {
    try (var os = new FileOutputStream(location)) {
      String result = parser.encodeResourceToString(resource);
      os.write(result.getBytes());
    } catch (IOException e) {
      throw new UnclassifiedServerFailureException(500,
          String.format("unable to write resource to location %s", location));
    }

    return new MethodOutcome(resource.getIdElement());
  }

  protected <T extends IBaseResource> List<T> readLocation(
      Class<T> resourceClass) {
    var location = this.directoryForResource(resourceClass);
    List<T> resources = new ArrayList<>();
    var inputDir = new File(location);
    if (inputDir.isDirectory()) {
      for (var file : inputDir.listFiles()) {
        if ((this.layoutMode.equals(IGLayoutMode.DIRECTORY)) ||
            (this.layoutMode.equals(IGLayoutMode.TYPE_PREFIX)
                && file.getName().startsWith(resourceClass.getSimpleName() + "-"))) {
          try {
            var r = parser.parseResource(resourceClass, new FileInputStream(file));
            resources.add(handleLibrary(r, file.getPath()));
          } catch (FileNotFoundException e) {
          }
        }
      }
    }

    return resources;
  }

  @Override
  public FhirContext fhirContext() {
    return this.fhirContext;
  }

  @Override
  public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
      Map<String, String> headers) {
    requireNonNull(resourceType, "resourceType can not be null");
    requireNonNull(id, "id can not be null");

    var location = this.locationForResource(resourceType, id);
    return readLocation(resourceType, location, id);
  }

  @Override
  public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
    requireNonNull(resource, "resource can not be null");
    requireNonNull(resource.getIdElement(), "resource id can not be null");

    var location = this.locationForResource(resource.getClass(), resource.getIdElement());
    return writeLocation(resource, location);
  }

  @Override
  public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters,
      Map<String, String> headers) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'patch'");
  }

  @Override
  public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
    requireNonNull(resource, "resource can not be null");
    requireNonNull(resource.getIdElement(), "resource id can not be null");

    var location = this.locationForResource(resource.getClass(), resource.getIdElement());
    return writeLocation(resource, location);
  }

  @Override
  public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType,
      I id, Map<String, String> headers) {
    requireNonNull(resourceType, "resourceType can not be null");
    requireNonNull(id, "id can not be null");

    var location = this.locationForResource(resourceType, id);

    try {
      new File(location).delete();
    } catch (Exception e) {
      throw new UnclassifiedServerFailureException(500,
          String.format("Couldn't delete %s", location));
    }

    return new MethodOutcome(id);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType,
      Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters,
      Map<String, String> headers) {
    BundleBuilder builder = new BundleBuilder(this.fhirContext);

    var resourceList = readLocation(resourceType);

    List<IBaseResource> filteredResources = new ArrayList<>();
    if (searchParameters != null && !searchParameters.isEmpty()) {
      var resourceMatcher = Repositories.getResourceMatcher(this.fhirContext);
      for (var resource : resourceList) {
        boolean include = false;
        for (var nextEntry : searchParameters.entrySet()) {
          var paramName = nextEntry.getKey();
          if (resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
            include = true;
          } else {
            include = false;
            break;
          }
        }
        if (include) {
          filteredResources.add(resource);
        }
      }
      filteredResources.forEach(builder::addCollectionEntry);
    } else {
      resourceList.forEach(builder::addCollectionEntry);
    }

    builder.setType("searchset");
    return (B) builder.getBundle();
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
