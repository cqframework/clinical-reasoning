package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.util.BundleBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.operation.OperationRegistry;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior.PreserveEncoding;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FhirTypeLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FilenameMode;

/**
 * This class implements the Repository interface on onto a directory structure
 * that matches some common IG layouts. Resources read by the class will be tagged
 * with metadata that indicates the source encoding and whether the resource is an
 * external resource. External resources are read-only.
 */
public class IgRepository implements Repository {
    private final FhirContext fhirContext;
    private final Path root;
    private final IgConventions conventions;
    private final EncodingBehavior encodingBehavior;
    private final ResourceMatcher resourceMatcher;
    private final OperationRegistry operationRegistry;

    private final Map<Path, Optional<IBaseResource>> resourceCache = new HashMap<>();

    // Metadata fields attached to resources that are read from the repository
    // This fields are used to determine if a resource is external, and to
    // maintain the original encoding of the resource.
    static final String SOURCE_PATH_TAG = "sourcePath"; // Path

    // Directory names
    static final String EXTERNAL_DIRECTORY = "external";
    static final Map<ResourceCategory, String> CATEGORY_DIRECTORIES = new ImmutableMap.Builder<
                    ResourceCategory, String>()
            .put(ResourceCategory.CONTENT, "resources")
            .put(ResourceCategory.DATA, "tests")
            .put(ResourceCategory.TERMINOLOGY, "vocabulary")
            .build();

    static final BiMap<EncodingEnum, String> FILE_EXTENSIONS = new ImmutableBiMap.Builder<EncodingEnum, String>()
            .put(EncodingEnum.JSON, "json")
            .put(EncodingEnum.XML, "xml")
            .put(EncodingEnum.RDF, "rdf")
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
     * Create a new IGRepository instance. The repository configuration is
     * auto-detected, and the encoding is set to JSON.
     *
     * @param fhirContext
     * @param root
     */
    public IgRepository(FhirContext fhirContext, Path root) {
        this(fhirContext, root, IgConventions.autoDetect(root), EncodingBehavior.DEFAULT, null);
    }

    /**
     * Create a new IGRepository instance.
     *
     * @param fhirContext       The FHIR context to use for parsing and encoding
     *                          resources.
     * @param root              The root directory of the IG
     * @param conventions       The conventions for the IG
     * @param encodingBehavior   The encoding behavior to use for parsing and encoding
     *                          resources.
     * @param operationRegistry The operation registry to use for invoking
     *                          operations.
     */
    public IgRepository(
            FhirContext fhirContext,
            Path root,
            IgConventions conventions,
            EncodingBehavior encodingBehavior,
            OperationRegistry operationRegistry) {
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.root = requireNonNull(root, "root can not be null");
        this.conventions = requireNonNull(conventions, "conventions is required");
        this.encodingBehavior = requireNonNull(encodingBehavior, "encodingBehavior is required");
        this.resourceMatcher = Repositories.getResourceMatcher(this.fhirContext);
        this.operationRegistry = operationRegistry;
    }

    public void clearCache() {
        this.resourceCache.clear();
    }

    private static boolean isExternaPath(Path path) {
        return path.getParent() != null
                && path.getParent().toString().toLowerCase().endsWith(EXTERNAL_DIRECTORY);
    }

    // This method is used to determine the preferred path for a resource.
    protected <T extends IBaseResource, I extends IIdType> Path preferredPathForResource(Class<T> resourceType, I id) {
        var directory = directoryForResource(resourceType);
        var fileName = fileNameForResource(
                resourceType.getSimpleName(), id.getIdPart(), this.encodingBehavior.preferredEncoding());
        return directory.resolve(fileName);
    }

    // Based on the current IG layout, there are a few potential path for a resource
    // For example, a resource could be in the "external" directory, or it may be json or xml.
    <T extends IBaseResource, I extends IIdType> List<Path> potentialPathsForResource(Class<T> resourceType, I id) {

        var potentialDirectories = new ArrayList<Path>();
        var directory = directoryForResource(resourceType);
        potentialDirectories.add(directory);

        // Currently, only terminology resources are allowed to be external
        if (ResourceCategory.forType(resourceType.getSimpleName()) == ResourceCategory.TERMINOLOGY) {
            var externalDirectory = directory.resolve(EXTERNAL_DIRECTORY);
            potentialDirectories.add(externalDirectory);
        }

        var potentialPaths = new ArrayList<Path>();

        // Cross product of directories and encodings gives us all potential paths
        for (var d : potentialDirectories) {
            for (var e : FILE_EXTENSIONS.keySet()) {
                potentialPaths.add(d.resolve(fileNameForResource(resourceType.getSimpleName(), id.getIdPart(), e)));
            }
        }

        return potentialPaths;
    }

    protected String fileNameForResource(String resourceType, String resourceId, EncodingEnum encoding) {
        var name = resourceId + "." + FILE_EXTENSIONS.get(encoding);
        if (FilenameMode.ID_ONLY.equals(conventions.filenameMode())) {
            return name;
        } else {
            return resourceType + "-" + name;
        }
    }

    protected <T extends IBaseResource> Path directoryForCategory(Class<T> resourceType) {
        if (this.conventions.categoryLayout() == CategoryLayout.FLAT) {
            return this.root;
        }

        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var directory = CATEGORY_DIRECTORIES.get(category);
        return root.resolve(directory);
    }

    protected <T extends IBaseResource> Path directoryForResource(Class<T> resourceType) {
        var directory = directoryForCategory(resourceType);
        if (this.conventions.typeLayout() == FhirTypeLayout.FLAT) {
            return directory;
        }

        return directory.resolve(resourceType.getSimpleName().toLowerCase());
    }

    protected Optional<IBaseResource> readResource(Path path) {
        var file = path.toFile();
        if (!file.exists()) {
            return Optional.empty();
        }

        var extension = fileExtension(path);
        if (extension == null) {
            // No file extension means not a possible resource file
            return Optional.empty();
        }

        var encoding = FILE_EXTENSIONS.inverse().get(extension);

        try (var stream = new FileInputStream(file)) {
            var resource = parserForEncoding(fhirContext, encoding).parseResource(stream);

            // Attach metadata to the resource
            resource.setUserData(SOURCE_PATH_TAG, path);
            CqlContent.loadCqlContent(resource, path.getParent()); // Use the directory as the root, not the filename

            return Optional.of(resource);
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (DataFormatException e) {
            throw new ResourceNotFoundException(String.format("Found empty or invalid content at path %s", path));
        } catch (IOException e) {
            throw new UnclassifiedServerFailureException(
                    500, String.format("Unable to read resource from path %s", path));
        }
    }

    protected Optional<IBaseResource> cachedReadResource(Path path) {
        return this.resourceCache.computeIfAbsent(path, this::readResource);
    }

    protected EncodingEnum encodingForPath(Path path) {
        var extension = fileExtension(path);
        return FILE_EXTENSIONS.inverse().get(extension);
    }

    protected <T extends IBaseResource> void writeResource(T resource, Path path) {
        try {
            if (path.getParent() != null) {
                path.getParent().toFile().mkdirs();
            }

            try (var stream = new FileOutputStream(path.toFile())) {
                String result = parserForEncoding(fhirContext, encodingForPath(path))
                        .setPrettyPrint(true)
                        .encodeResourceToString(resource);
                stream.write(result.getBytes());
                resource.setUserData(SOURCE_PATH_TAG, path);
                this.resourceCache.put(path, Optional.of(resource));
            }
        } catch (IOException | SecurityException e) {
            throw new UnclassifiedServerFailureException(
                    500, String.format("Unable to write resource to path %s", path));
        }
    }

    private String fileExtension(Path path) {
        var name = path.getFileName().toString();
        var lastPeriod = name.lastIndexOf(".");
        if (lastPeriod == -1) {
            return null;
        }

        return name.substring(lastPeriod + 1).toLowerCase();
    }

    // True if the file extension is one of the supported file extensions
    private boolean acceptByFileExtension(Path path) {
        var extension = fileExtension(path);
        if (extension == null) {
            return false;
        }

        return FILE_EXTENSIONS.containsValue(extension);
    }

    // True if the file extension is one of the supported file extensions
    // and the file name starts with the given prefix (resource type name)
    private boolean acceptByFileExtensionAndPrefix(Path path, String prefix) {
        var extensionAccepted = this.acceptByFileExtension(path);
        if (!extensionAccepted) {
            return false;
        }

        return path.getFileName().toString().toLowerCase().startsWith(prefix.toLowerCase() + "-");
    }

    protected <T extends IBaseResource> Map<IIdType, T> readDirectoryForResourceType(Class<T> resourceClass) {
        var path = this.directoryForResource(resourceClass);
        var resources = new HashMap<IIdType, T>();
        if (!path.toFile().exists()) {
            return resources;
        }

        Predicate<Path> resourceFileFilter;
        switch (this.conventions.filenameMode()) {
            case ID_ONLY:
                resourceFileFilter = this::acceptByFileExtension;
                break;
            case TYPE_AND_ID:
            default:
                resourceFileFilter = p -> this.acceptByFileExtensionAndPrefix(p, resourceClass.getSimpleName());
                break;
        }

        try (var paths = Files.walk(path)) {
            var recursiveResources = paths.filter(resourceFileFilter)
                    .map(this::cachedReadResource)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            for (var r : recursiveResources) {
                if (!r.fhirType().equals(resourceClass.getSimpleName())) {
                    continue;
                }

                T t = validateResource(resourceClass, r, r.getIdElement());
                resources.put(r.getIdElement().toUnqualifiedVersionless(), t);
            }
        } catch (IOException e) {
            throw new UnclassifiedServerFailureException(
                    500, String.format("Unable to read resources from path: %s", path));
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

        var paths = this.potentialPathsForResource(resourceType, id);
        for (var path : paths) {
            if (!path.toFile().exists()) {
                continue;
            }

            var optionalResource = cachedReadResource(path);
            if (optionalResource.isPresent()) {
                var r = optionalResource.get();
                return validateResource(resourceType, r, id);
            }
        }

        throw new ResourceNotFoundException(id);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        requireNonNull(resource, "resource can not be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id can not be null");

        var path = this.preferredPathForResource(resource.getClass(), resource.getIdElement());
        writeResource(resource, path);

        return new MethodOutcome(resource.getIdElement(), true);
    }

    private <T extends IBaseResource> T validateResource(Class<T> resourceType, IBaseResource r, IIdType id) {
        // All freshly read resources are tagged with their source path
        var path = (Path) r.getUserData(SOURCE_PATH_TAG);

        if (!resourceType.getSimpleName().equals(r.fhirType())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with type: %s at path: %s. Found resource with type %s instead.",
                    resourceType.getSimpleName(), path, r.fhirType()));
        }

        if (!r.getIdElement().hasIdPart()) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at path: %s. Found resource without an id instead.",
                    id.toUnqualifiedVersionless(), path));
        }

        if (!id.getIdPart().equals(r.getIdElement().getIdPart())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with id: %s at path: %s. Found resource with an id %s instead.",
                    id.getIdPart(), path, r.getIdElement().getIdPart()));
        }

        if (id.hasVersionIdPart()
                && !id.getVersionIdPart().equals(r.getIdElement().getVersionIdPart())) {
            throw new ResourceNotFoundException(String.format(
                    "Expected to find a resource with version: %s at path: %s. Found resource with version %s instead.",
                    id.getVersionIdPart(), path, r.getIdElement().getVersionIdPart()));
        }

        return resourceType.cast(r);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        requireNonNull(resource, "resource can not be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id can not be null");

        var preferred = this.preferredPathForResource(resource.getClass(), resource.getIdElement());
        var actual = (Path) resource.getUserData(SOURCE_PATH_TAG);
        if (actual == null) {
            actual = preferred;
        }

        if (isExternaPath(actual)) {
            throw new ForbiddenOperationException(String.format(
                    "Unable to create or update: %s. Resource is marked as external, and external resources are read-only.",
                    resource.getIdElement().toUnqualifiedVersionless()));
        }

        // If the preferred path and the actual path are different, and the encoding behavior is set to overwrite,
        // move the resource to the preferred path and delete the old one.
        if (!preferred.equals(actual)
                && this.encodingBehavior.preserveEncoding() == PreserveEncoding.OVERWRITE_WITH_PREFERRED_ENCODING) {
            try {
                java.nio.file.Files.deleteIfExists(actual);
            } catch (IOException e) {
                throw new UnclassifiedServerFailureException(
                        500, String.format("Couldn't change encoding for %s", actual));
            }

            actual = preferred;
        }

        writeResource(resource, actual);

        return new MethodOutcome(resource.getIdElement(), false);
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        requireNonNull(resourceType, "resourceType can not be null");
        requireNonNull(id, "id can not be null");

        var paths = this.potentialPathsForResource(resourceType, id);

        boolean deleted = false;
        for (var path : paths) {
            try {
                deleted = Files.deleteIfExists(path);
                if (deleted) {
                    break;
                }
            } catch (IOException e) {
                throw new UnclassifiedServerFailureException(500, String.format("Couldn't delete %s", path));
            }
        }

        if (!deleted) {
            throw new ResourceNotFoundException(id);
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
        builder.setType("searchset");

        var resourceIdMap = readDirectoryForResourceType(resourceType);
        if (searchParameters == null || searchParameters.isEmpty()) {
            resourceIdMap.values().forEach(builder::addCollectionEntry);
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
                // indexed by. If an id has a version it won't match. Need apples-to-apples Id
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
            if (allParametersMatch(searchParameters, resource)) {
                builder.addCollectionEntry(resource);
            }
        }

        return (B) builder.getBundle();
    }

    private boolean allParametersMatch(
            Map<String, List<IQueryParameterType>> searchParameters, IBaseResource resource) {
        for (var nextEntry : searchParameters.entrySet()) {
            var paramName = nextEntry.getKey();
            if (!resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
                return false;
            }
        }

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return (R) this.operationRegistry.execute(this, name, resourceType, null, parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return (R) this.operationRegistry.execute(this, name, null, id, parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(
            String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return (R) this.operationRegistry.execute(this, name, null, null, parameters);
    }
}
