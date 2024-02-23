package org.opencds.cqf.fhir.utility.repository.ig;

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
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.operations.IRepositoryOperationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the Repository interface on onto a directory structure
 * that matches some common IG layouts.
 */
public class IgRepository implements Repository {

    private static final Logger LOG = LoggerFactory.getLogger(IgRepository.class);

    // Potential metadata fields:
    // file dateTime
    // file extension (json, xml, rdf)
    // pretty print
    // resource type in file name
    // directory structure for data vs content vs terminology
    // directory structure for resource type (or lack thereof)

    private final FhirContext fhirContext;
    private final String root;
    private final IgConventions conventions;
    private final EncodingEnum encodingEnum;
    private final IParser parser;
    private final ResourceMatcher resourceMatcher;
    private IRepositoryOperationProvider operationProvider;

    private final Map<String, IBaseResource> resourceCache = new HashMap<>();

    static final Map<ResourceCategory, String> CATEGORY_DIRECTORIES = new ImmutableMap.Builder<
                    ResourceCategory, String>()
            .put(ResourceCategory.CONTENT, "resources")
            .put(ResourceCategory.DATA, "tests")
            .put(ResourceCategory.TERMINOLOGY, "vocabulary")
            .build();

    static final Map<EncodingEnum, String> FILE_EXTENSIONS = new ImmutableMap.Builder<EncodingEnum, String>()
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

    /**
     * Create a new IGRepository instance. The repository configuration is auto-detected, and the encoding is set to JSON.
     *
     * @param fhirContext
     * @param root
     */
    public IgRepository(FhirContext fhirContext, String root) {
        this(fhirContext, root, IgConventions.autoDetect(Paths.get(root)), EncodingEnum.JSON, null);
    }

    private static String ensureTrailingSlash(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    /**
     * Create a new IGRepository instance.
     *
     * @param fhirContext The FHIR context to use for parsing and encoding resources.
     * @param root The root directory of the IG
     * @param conventions The conventions for the IG
     * @param encodingEnum The encoding to use for parsing and encoding resources.
     * @param operationProvider The operation provider to use for invoking operations.
     */
    public IgRepository(
            FhirContext fhirContext,
            String root,
            IgConventions conventions,
            EncodingEnum encodingEnum,
            IRepositoryOperationProvider operationProvider) {
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.root = ensureTrailingSlash(requireNonNull(root, "root can not be null"));
        this.conventions = requireNonNull(conventions, "conventions is required");
        this.encodingEnum = requireNonNull(encodingEnum, "encodingEnum can not be null");
        this.parser = parserForEncoding(fhirContext, encodingEnum);
        this.resourceMatcher = Repositories.getResourceMatcher(this.fhirContext);
        this.operationProvider = operationProvider;
    }

    public void setOperationProvider(IRepositoryOperationProvider operationProvider) {
        this.operationProvider = operationProvider;
    }

    public void clearCache() {
        this.resourceCache.clear();
    }

    protected <T extends IBaseResource, I extends IIdType> String locationForResource(Class<T> resourceType, I id) {
        var directory = directoryForResource(resourceType);
        return directory + fileNameForResource(resourceType.getSimpleName(), id.getIdPart());
    }

    protected String fileNameForResource(String resourceType, String resourceId) {
        var name = resourceId + FILE_EXTENSIONS.get(this.encodingEnum);
        if (FilenameMode.ID_ONLY.equals(conventions.filenameMode())) {
            return name;
        } else {
            return resourceType + "-" + name;
        }
    }

    protected <T extends IBaseResource> String directoryForCategory(Class<T> resourceType) {
        if (this.conventions.categoryLayout() == CategoryLayout.FLAT) {
            return this.root;
        }

        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var directory = CATEGORY_DIRECTORIES.get(category);
        return root + directory + File.separator;
    }

    protected <T extends IBaseResource> String directoryForResource(Class<T> resourceType) {
        var directory = directoryForCategory(resourceType);
        if (this.conventions.typeLayout() == FhirTypeLayout.FLAT) {
            return directory;
        }

        return directory + resourceType.getSimpleName().toLowerCase() + File.separator;
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
        if ("Library".equals(resource.fhirType())) {
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

    @SuppressWarnings("null")
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

        FilenameFilter resourceFileFilter;
        var filenameMode = this.conventions.filenameMode();
        if (filenameMode.equals(FilenameMode.ID_ONLY)) {
            resourceFileFilter = (dir, name) -> name.toLowerCase().endsWith(FILE_EXTENSIONS.get(this.encodingEnum));
        } else {
            resourceFileFilter = (dir, name) ->
                    name.toLowerCase().startsWith(resourceClass.getSimpleName().toLowerCase() + "-")
                            && name.toLowerCase().endsWith(FILE_EXTENSIONS.get(this.encodingEnum));
        }

        for (var file : inputDir.listFiles(resourceFileFilter)) {
            try {
                var r = this.readLocation(file.getPath());
                T t = validateResource(resourceClass, r, r.getIdElement(), file.getPath());
                resources.put(r.getIdElement().toUnqualifiedVersionless(), t);
            } catch (RuntimeException e) {
                // intentionally empty
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

        return validateResource(resourceType, r, id, location);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        requireNonNull(resource, "resource can not be null");
        requireNonNull(resource.getIdElement(), "resource id can not be null");

        var location = this.locationForResource(resource.getClass(), resource.getIdElement());
        return writeLocation(resource, location);
    }

    private <T extends IBaseResource> T validateResource(
            Class<T> resourceType, IBaseResource r, IIdType id, String location) {
        if (r == null) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at location: %s. Found empty or invalid content instead.",
                    id.toUnqualifiedVersionless(), location));
        }

        if (!resourceType.getSimpleName().equals(r.fhirType())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with type: %s at location: %s. Found resource with type %s instead.",
                    resourceType.getSimpleName(), location, r.fhirType()));
        }

        if (!r.getIdElement().hasIdPart()) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at location: %s. Found resource without an id instead.",
                    id.toUnqualifiedVersionless(), location));
        }

        if (!id.getIdPart().equals(r.getIdElement().getIdPart())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at location: %s. Found resource with an id %s instead.",
                    id.getIdPart(), location, r.getIdElement().getIdPart()));
        }

        if (id.hasVersionIdPart()
                && !id.getVersionIdPart().equals(r.getIdElement().getVersionIdPart())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with version: %s at location: %s. Found resource with version %s instead.",
                    id.getVersionIdPart(), location, r.getIdElement().getVersionIdPart()));
        }

        return resourceType.cast(r);
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
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(
            I id, P patchParameters, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patch'");
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
        return invokeOperation(null, resourceType.getSimpleName(), name, parameters, headers);
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
        return invokeOperation(id, id.getResourceType(), name, parameters, headers);
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(
            I id, String name, P parameters, Map<String, String> headers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invoke'");
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeOperation(
            IIdType id,
            String resourceType,
            String operationName,
            IBaseParameters parameters,
            Map<String, String> headers) {
        if (operationProvider == null) {
            throw new IllegalArgumentException("No operation provider found.  Unable to invoke operations.");
        }
        return operationProvider.invokeOperation(this, id, resourceType, operationName, parameters);
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
