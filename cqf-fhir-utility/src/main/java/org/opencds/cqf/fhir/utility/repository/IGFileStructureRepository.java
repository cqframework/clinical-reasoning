package org.opencds.cqf.fhir.utility.repository;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.util.BundleBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;

/**
 * This class implements the Repository interface on onto a directory structure
 * that matches the
 * standard IG layout.
 */
public class IGFileStructureRepository implements Repository {

    // Potential metadata fields:
    // file dateTime
    // file extension (json, xml, rdf)
    // pretty print
    // resource type in file name
    // directory structure for data vs content vs terminology
    // directory structure for resource type (or lack thereof)

    private final FhirContext fhirContext;
    private final String root;
    private final IGLayoutMode layoutMode;
    private final EncodingEnum encodingEnum;
    private final IParser parser;
    private final ResourceMatcher resourceMatcher;

    private final Map<String, IBaseResource> resourceCache = new HashMap<>();

    private static final Map<ResourceCategory, String> categoryDirectories = new ImmutableMap.Builder<
                    ResourceCategory, String>()
            .put(ResourceCategory.CONTENT, "resources")
            .put(ResourceCategory.DATA, "tests")
            .put(ResourceCategory.TERMINOLOGY, "vocabulary")
            .build();

    private static final Map<EncodingEnum, String> fileExtensions = new ImmutableMap.Builder<EncodingEnum, String>()
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

    public IGFileStructureRepository(
            FhirContext fhirContext, String root, IGLayoutMode layoutMode, EncodingEnum encodingEnum) {
        this.fhirContext = fhirContext;
        this.root = root;
        this.layoutMode = layoutMode;
        this.encodingEnum = encodingEnum;
        this.parser = parserForEncoding(fhirContext, encodingEnum);
        this.resourceMatcher = Repositories.getResourceMatcher(this.fhirContext);
    }

    public void clearCache() {
        this.resourceCache.clear();
    }

    protected <T extends IBaseResource, I extends IIdType> String locationForResource(Class<T> resourceType, I id) {
        var directory = directoryForType(resourceType);
        return directory + "/" + fileNameForLayoutAndEncoding(resourceType.getSimpleName(), id.getIdPart());
    }

    protected String fileNameForLayoutAndEncoding(String resourceType, String resourceId) {
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

    protected IBaseResource readLocation(String location) {
        return this.resourceCache.computeIfAbsent(location, l -> {
            try (var is = new FileInputStream(l)) {
                var x = parser.parseResource(is);
                return handleLibrary(x, l);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected <T extends IBaseResource> T handleLibrary(T resource, String location) {
        if (resource.fhirType() != null && resource.fhirType().equals("Library")) {
            String cqlLocation;
            switch (fhirContext.getVersion().getVersion()) {
                case DSTU3:
                    cqlLocation = org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil.getCqlLocation(resource);
                    if (cqlLocation != null) {
                        resource = (T) org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil.addData(
                                resource, getCqlContent(location, cqlLocation));
                    }
                    break;
                case R4:
                    cqlLocation = org.opencds.cqf.fhir.utility.r4.AttachmentUtil.getCqlLocation(resource);
                    if (cqlLocation != null) {
                        resource = (T) org.opencds.cqf.fhir.utility.r4.AttachmentUtil.addData(
                                resource, getCqlContent(location, cqlLocation));
                    }
                    break;
                case R5:
                    cqlLocation = org.opencds.cqf.fhir.utility.r5.AttachmentUtil.getCqlLocation(resource);
                    if (cqlLocation != null) {
                        resource = (T) org.opencds.cqf.fhir.utility.r5.AttachmentUtil.addData(
                                resource, getCqlContent(location, cqlLocation));
                    }
                    break;
                default:
                    throw new IllegalArgumentException(String.format("unsupported FHIR version: %s", fhirContext));
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

    protected <T extends IBaseResource> MethodOutcome writeLocation(T resource, String location) {
        try (var os = new FileOutputStream(location)) {
            String result = parser.setPrettyPrint(true).encodeResourceToString(resource);
            os.write(result.getBytes());
            this.resourceCache.put(location, resource);
        } catch (IOException e) {
            throw new UnclassifiedServerFailureException(
                    500, String.format("unable to write resource to location %s", location));
        }

        return new MethodOutcome(resource.getIdElement());
    }

    protected <T extends IBaseResource> Map<IIdType, T> readLocation(Class<T> resourceClass) {
        var location = this.directoryForResource(resourceClass);
        var resources = new HashMap<IIdType, T>();
        var inputDir = new File(location);
        if (!inputDir.exists()) {
            return resources;
        }

        FilenameFilter resourceFileFilter =
                (dir, name) -> name.toLowerCase().endsWith(fileExtensions.get(this.encodingEnum));

        for (var file : inputDir.listFiles(resourceFileFilter)) {
            if ((this.layoutMode.equals(IGLayoutMode.DIRECTORY))
                    || (this.layoutMode.equals(IGLayoutMode.TYPE_PREFIX)
                            && file.getName().startsWith(resourceClass.getSimpleName() + "-"))) {
                try {
                    var r = this.readLocation(file.getPath());
                    T t = validateResource(resourceClass, r, file.getPath(), r.getIdElement());
                    resources.put(r.getIdElement().toUnqualifiedVersionless(), t);
                } catch (RuntimeException e) {
                    // intentionally empty
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
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        requireNonNull(resourceType, "resourceType can not be null");
        requireNonNull(id, "id can not be null");

        var location = this.locationForResource(resourceType, id);
        IBaseResource r = null;
        try {
            r = readLocation(location);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                throw new ResourceNotFoundException(id);
            }
        }

        return validateResource(resourceType, r, location, id);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        requireNonNull(resource, "resource can not be null");
        requireNonNull(resource.getIdElement(), "resource id can not be null");

        var location = this.locationForResource(resource.getClass(), resource.getIdElement());
        return writeLocation(resource, location);
    }

    private <T extends IBaseResource> T validateResource(
            Class<T> resourceType, IBaseResource r, String location, IIdType id) {
        if (r == null) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at location: %s. Found empty or invalid content instead.",
                    id.toUnqualifiedVersionless(), location));
        }

        if (r.getIdElement() == null || !r.getIdElement().hasIdPart()) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at location: %s. Found resource without an id instead.",
                    id.toUnqualifiedVersionless(), location));
        }

        if (!r.fhirType().equals(resourceType.getSimpleName())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with type: %s at location: %s. Found resource with type %s instead.",
                    resourceType.getSimpleName(), location, r.fhirType()));
        }

        if (!r.getIdElement().getIdPart().equals(id.getIdPart())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at location: %s. Found resource with an id %s instead.",
                    id.getIdPart(), location, r.getIdElement().getIdPart()));
        }

        if (id.hasVersionIdPart()
                && r.getIdElement().hasVersionIdPart()
                && !r.getIdElement().getVersionIdPart().equals(id.getVersionIdPart())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with version: %s at location: %s. Found resource with version %s instead.",
                    id.getVersionIdPart(), location, r.getIdElement().getVersionIdPart()));
        }

        return resourceType.cast(r);
    }

    @Override
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(
            I id, P patchParameters, Map<String, String> headers) {
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
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        requireNonNull(resourceType, "resourceType can not be null");
        requireNonNull(id, "id can not be null");

        var location = this.locationForResource(resourceType, id);

        try {
            var deleted = java.nio.file.Files.deleteIfExists(new File(location).toPath());
            if (!deleted) {
                throw new ResourceNotFoundException(id);
            }
        } catch (IOException e) {
            throw new UnclassifiedServerFailureException(500, String.format("Couldn't delete %s", location));
        }

        return new MethodOutcome(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        BundleBuilder builder = new BundleBuilder(this.fhirContext);

        var resourceIdMap = readLocation(resourceType);
        if (searchParameters == null || searchParameters.isEmpty()) {
            resourceIdMap.values().forEach(builder::addCollectionEntry);
            builder.setType("searchset");
            return (B) builder.getBundle();
        }

        Collection<T> candidates;
        if (searchParameters.containsKey("_id")) {
            // We are consuming the _id parameter in this if statement
            var idQueries = searchParameters.get("_id");
            searchParameters.remove("_id");

            var idResources = new ArrayList<T>(idQueries.size());
            for (var idQuery : idQueries) {
                var idToken = (TokenParam) idQuery;
                // Need to construct the equivalent "UnqualifiedVersionless" id that the map is
                // indexed by. If an id has a version it won't match. Need apples-to-apples Ids
                // types
                var id = Ids.newId(fhirContext, resourceType.getSimpleName(), idToken.getValue());
                var r = resourceIdMap.get(id);
                if (r != null) {
                    idResources.add(r);
                }
            }

            candidates = idResources;
        } else {
            candidates = resourceIdMap.values();
        }

        for (var resource : candidates) {
            boolean include = true;
            for (var nextEntry : searchParameters.entrySet()) {
                var paramName = nextEntry.getKey();
                if (!resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
                    include = false;
                    break;
                }
            }

            if (include) {
                builder.addCollectionEntry(resource);
            }
        }

        builder.setType("searchset");
        return (B) builder.getBundle();
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'link'");
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'capabilities'");
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transaction'");
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(
            String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invoke'");
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invoke'");
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
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
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invoke'");
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(
            I id, String name, P parameters, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invoke'");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(
            P parameters, Class<B> returnType, Map<String, String> headers) {
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
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(
            I id, P parameters, Class<B> returnType, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'history'");
    }
}
