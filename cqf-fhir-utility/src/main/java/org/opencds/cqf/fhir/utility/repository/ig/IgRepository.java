package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.util.BundleBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior.PreserveEncoding;
import org.opencds.cqf.fhir.utility.repository.operations.IRepositoryOperationProvider;

/**
 * Provides access to FHIR resources stored in a directory structure following
 * Implementation Guide (IG) conventions or KALM (Knowledge Artifact Lifecycle Management) conventions.
 * Supports CRUD operations and resource management based on configurable directory and
 * file naming conventions.
 *
 * <p>
 * <strong>Directory Structure Overview (based on conventions):</strong>
 * </p>
 *
 * <pre>
 * Standard IG Layout:
 * /path/to/ig/root/          (CategoryLayout.FLAT)
 * ├── input/
 * │   ├── [resources/]               (CategoryLayout.DIRECTORY_PER_CATEGORY)
 * │   │   ├── Patient-789.json       (FhirTypeLayout.FLAT)
 * │   │   ├── or
 * │   │   ├── [patient/]             (FhirTypeLayout.DIRECTORY_PER_TYPE)
 * │   │   │   ├── Patient-123.json   (FilenameMode.TYPE_AND_ID)
 * │   │   │   ├── or
 * │   │   │   ├── 456.json           (FilenameMode.ID_ONLY)
 * │   │   │   └── ...
 * │   │   └── ...
 * │   └── vocabulary/                (CategoryLayout.DIRECTORY_PER_CATEGORY)
 * │       ├── ValueSet-ABC.json      (FilenameMode.TYPE_AND_ID)
 * |       ├── or
 * │       ├── DEF.json               (FilenameMode.ID_ONLY)
 * │       ├── input/tests/vocabulary/valueset/ (Test-only terminology fallback)
 * │       └── external/              (External Resources - Read-only, Terminology-only)
 * │           └── CodeSystem-external.json
 * └── ...
 *
 * KALM Project Layout:
 * /path/to/kalm/root/        (CategoryLayout.DEFINITIONAL_AND_DATA)
 * ├── src/
 * │   └── fhir/              (Definitional Resources)
 * │       └── ...
 * └── tests/
 *     └── data/
 *         └── fhir/          (Test Data Resources)
 *           ├── shared/        (Non-compartmental data and overrides)
 *           │   └── valueset/Example.json
 *           ├── [patient/]     (CompartmentMode.PATIENT)
 *           │   └── Patient/123/
 *           │       └── Observation-456.json
 *           └── ...
 * </pre>
 *
 * <p>
 * <strong>Compartment Support:</strong>
 * </p>
 * <p>
 * The repository supports FHIR compartment contexts using directory conventions.
 * When using a {@code CompartmentMode} other than {@code CompartmentMode.NONE}, resources are organized by
 * compartment type and ID (e.g., {@code Patient/123/}).
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Supports CRUD operations on FHIR resources.</li>
 * <li>Handles multiple directory layouts: standard IG conventions and KALM project conventions.</li>
 * <li>Supports compartment-based resource organization for patient-centric data management.</li>
 * <li>Handles different filename conventions (TYPE_AND_ID vs ID_ONLY).</li>
 * <li>Annotates resources with metadata like source path and external designation.</li>
 * <li>Supports invoking FHIR operations through an {@link IRepositoryOperationProvider}.</li>
 * <li>Utilizes caching for efficient resource access.</li>
 * <li>Auto-detects project conventions based on directory structure.</li>
 * </ul>
 */
public class IgRepository implements IRepository {
    private final FhirContext fhirContext;
    private final Path root;
    private final IgConventions conventions;
    private final ResourceMatcher resourceMatcher;
    private IRepositoryOperationProvider operationProvider;

    private final Cache<Path, Optional<IBaseResource>> resourceCache =
            CacheBuilder.newBuilder().maximumSize(5000).build();
    private final ResourcePathResolver pathResolver;

    // Metadata fields attached to resources that are read from the repository
    // This fields are used to determine if a resource is external, and to
    // maintain the original encoding of the resource.
    static final String SOURCE_PATH_TAG = "sourcePath"; // Path

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static IParser parserForEncoding(FhirContext fhirContext, EncodingEnum encodingEnum) {
        return switch (encodingEnum) {
            case JSON -> fhirContext.newJsonParser();
            case XML -> fhirContext.newXmlParser();
            case RDF -> fhirContext.newRDFParser();
            case NDJSON -> fhirContext.newNDJsonParser();
        };
    }

    /**
     * Creates a new {@code IgRepository} with auto-detected conventions and default
     * encoding behavior.
     * The repository configuration is determined based on the directory structure.
     *
     * @param fhirContext The FHIR context to use for parsing and encoding
     *                    resources.
     * @param root        The root directory of the IG.
     * @see IgConventions#autoDetect(Path)
     */
    public IgRepository(FhirContext fhirContext, Path root) {
        this(fhirContext, root, IgConventions.autoDetect(root), null);
    }

    /**
     * Creates a new {@code IgRepository} with specified conventions and encoding
     * behavior.
     *
     * @param fhirContext       The FHIR context to use.
     * @param root              The root directory of the IG.
     * @param conventions       The conventions defining directory and filename
     *                          structures.
     * @param operationProvider The operation provider for invoking FHIR operations.
     */
    public IgRepository(
            FhirContext fhirContext,
            Path root,
            IgConventions conventions,
            IRepositoryOperationProvider operationProvider) {
        this.fhirContext = requireNonNull(fhirContext, "fhirContext cannot be null");
        this.root = requireNonNull(root, "root cannot be null");
        this.conventions = requireNonNull(conventions, "conventions cannot be null");
        this.resourceMatcher = Repositories.getResourceMatcher(this.fhirContext);
        this.operationProvider = operationProvider;
        this.pathResolver = new ResourcePathResolver(this.root, this.conventions, this.fhirContext);
    }

    /**
     * Assigns the operation provider used to execute custom FHIR operations against this repository.
     *
     * @param operationProvider the provider responsible for handling operation invocations
     */
    public void setOperationProvider(IRepositoryOperationProvider operationProvider) {
        this.operationProvider = operationProvider;
    }

    /**
     * Clears all cached resources and path indexes maintained by the repository.
     */
    public void clearCache() {
        this.resourceCache.invalidateAll();
    }

    /**
     * Removes cached entries for the specified file system paths from both the resource cache and
     * the internal path index.
     *
     * @param paths the set of paths to evict from the cache
     */
    public void clearCache(Iterable<Path> paths) {
        this.resourceCache.invalidate(paths);
    }

    /**
     * Reads a resource from the given file path.
     *
     * @param path The path to the resource file.
     * @return An {@code Optional} containing the resource if found; otherwise,
     *         empty.
     */
    protected Optional<IBaseResource> readResource(Path path) {
        var file = path.toFile();
        if (!file.exists()) {
            return Optional.empty();
        }

        var encoding = this.pathResolver.encodingForPath(path);

        String content;
        try {
            content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UnclassifiedServerFailureException(500, "Unable to read resource from path %s".formatted(path));
        }

        try {
            var resource = parserForEncoding(fhirContext, encoding).parseResource(content);

            resource.setUserData(SOURCE_PATH_TAG, path);
            CqlContent.loadCqlContent(resource, path.getParent());

            return Optional.of(resource);
        } catch (DataFormatException e) {
            var fallback = tryParseEmbeddedResource(path, encoding, content);
            if (fallback.isPresent()) {
                return fallback;
            }
            throw new ResourceNotFoundException("Found empty or invalid content at path %s".formatted(path));
        }
    }

    private Optional<IBaseResource> tryParseEmbeddedResource(Path path, EncodingEnum encoding, String content) {
        try {
            var raw = content.trim();
            if (!raw.startsWith("\"resource\"") && !raw.startsWith("{\"resource\"")) {
                return Optional.empty();
            }

            var wrapped = raw.startsWith("{") ? raw : "{%s}".formatted(raw);
            JsonNode node = OBJECT_MAPPER.readTree(wrapped);
            if (node == null || !node.has("resource")) {
                return Optional.empty();
            }

            var resourceNode = node.get("resource");
            if (resourceNode == null || resourceNode.isMissingNode()) {
                return Optional.empty();
            }

            var resourceContent = OBJECT_MAPPER.writeValueAsString(resourceNode);
            var resource = parserForEncoding(fhirContext, encoding).parseResource(resourceContent);
            resource.setUserData(SOURCE_PATH_TAG, path);
            CqlContent.loadCqlContent(resource, path.getParent());
            return Optional.of(resource);
        } catch (IOException | DataFormatException ignored) {
            return Optional.empty();
        }
    }

    protected Optional<IBaseResource> cachedReadResource(Path path) {
        try {
            return this.resourceCache.get(path, () -> this.readResource(path));
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }

            throw new UnclassifiedServerFailureException(500, "Unable to read resource from path %s".formatted(path));
        }
    }

    /**
     * Writes a resource to the specified file path.
     *
     * @param <T>      The type of the FHIR resource.
     * @param resource The resource to write.
     * @param path     The file path to write the resource to.
     */
    protected <T extends IBaseResource> void writeResource(T resource, Path path) {
        try {
            if (path.getParent() != null) {
                path.getParent().toFile().mkdirs();
            }

            try (var stream = new FileOutputStream(path.toFile())) {
                var encoding = this.pathResolver.encodingForPath(path);
                String result = parserForEncoding(fhirContext, encoding)
                        .setPrettyPrint(true)
                        .encodeResourceToString(resource);
                stream.write(result.getBytes());
                resource.setUserData(SOURCE_PATH_TAG, path);
                this.resourceCache.put(path, Optional.of(resource));
            }
        } catch (IOException | SecurityException e) {
            throw new UnclassifiedServerFailureException(500, "Unable to write resource to path %s".formatted(path));
        }
    }

    /**
     * Reads all resources of a given type from the appropriate directories based on
     * repository conventions and compartment context.
     *
     * <p>Directory scanning behavior:</p>
     * <ul>
     * <li>Flat layout: Scans root/category directory for resources</li>
     * <li>Type-specific directories: Scans type-specific subdirectories</li>
     * <li>Compartment-aware: Includes compartment-specific paths when applicable</li>
     * </ul>
     *
     * <p>File filtering based on filename conventions:</p>
     * <ul>
     * <li>{@code FilenameMode.ID_ONLY}: Accepts any file with supported extension</li>
     * <li>{@code FilenameMode.TYPE_AND_ID}: Accepts files matching pattern "ResourceType-*"</li>
     * </ul>
     *
     * @param <T>                     The type of the FHIR resource.
     * @param resourceClass           The class representing the FHIR resource type.
     * @param igRepositoryCompartment The compartment context for directory resolution.
     * @return Map of resource IDs to resources found in the directories.
     */
    protected <T extends IBaseResource> Map<IIdType, T> readDirectoryForResourceType(Class<T> resourceClass) {
        var directories = this.pathResolver.searchDirectories(resourceClass);

        var resources = new ConcurrentHashMap<IIdType, T>();
        Predicate<Path> resourceFileFilter = this.pathResolver.fileMatcher(resourceClass);

        for (var dir : directories) {
            if (!Files.exists(dir)) {
                continue;
            }

            // Walk the directory and read all files that match the resource type
            // and file extension
            try (var pathsStream = Files.walk(dir)) {
                pathsStream
                        .filter(resourceFileFilter)
                        .parallel()
                        .map(this::cachedReadResource)
                        .filter(Optional::isPresent)
                        .forEach(r -> {
                            if (!r.get().fhirType().equals(resourceClass.getSimpleName())) {
                                return;
                            }

                            T validatedResource = validateResource(
                                    resourceClass, r.get(), r.get().getIdElement());
                            resources.put(r.get().getIdElement().toUnqualifiedVersionless(), validatedResource);
                        });
            } catch (IOException e) {
                throw new UnclassifiedServerFailureException(
                        500, "Unable to read resources from path: %s".formatted(dir));
            }
        }

        return resources;
    }

    @Override
    public FhirContext fhirContext() {
        return this.fhirContext;
    }

    /**
     * Reads a resource from the repository, considering compartment context.
     *
     * <p>File location resolution:</p>
     * <ul>
     * <li>ID_ONLY: "123.json" (in the appropriate directory based on layout and compartment)</li>
     * <li>TYPE_AND_ID: "Patient-123.json"</li>
     * </ul>
     *
     * <p>Compartment context is determined by repository conventions and resource references.</p>
     *
     * Utilizes cache to improve performance.
     *
     * <p>
     * <strong>Example Usage:</strong>
     * </p>
     *
     * <pre>{@code
     * IIdType resourceId = new IdType("Observation", "obs-123");
     * Map<String, String> headers = new HashMap<>();
     * headers.put(FHIR_COMPARTMENT_HEADER, "Patient/patient-456");
     * Observation observation = repository.read(Observation.class, resourceId, headers);
     * }</pre>
     *
     * @param <T>          The type of the FHIR resource.
     * @param <I>          The type of the resource identifier.
     * @param resourceType The class representing the FHIR resource type.
     * @param id           The identifier of the resource.
     * @param headers      Request headers
     * @return The resource if found.
     * @throws ResourceNotFoundException if the resource is not found.
     */
    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(id, "id cannot be null");

        var paths = this.pathResolver.potentialPathsForResource(resourceType, id);
        var resource = paths.map(this::cachedReadResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (resource.isPresent()) {
            return validateResource(resourceType, resource.get(), id);
        }

        throw new ResourceNotFoundException(id);
    }

    /**
     * Creates a new resource in the repository, considering compartment context for file placement.
     *
     * <p>The resource will be written to the appropriate directory based on repository conventions
     * and any compartment context provided in headers.</p>
     *
     * <p>
     * <strong>Example Usage:</strong>
     * </p>
     *
     * <pre>{@code
     * Observation newObservation = new Observation();
     * newObservation.setId("obs-789");
     * newObservation.setSubject(new Reference("Patient/patient-123"));
     * MethodOutcome outcome = repository.create(newObservation, null);
     * }</pre>
     *
     * @param <T>      The type of the FHIR resource.
     * @param resource The resource to create.
     * @param headers  Request headers, may include compartment context via {@code X-FHIR-Compartment}.
     * @return A {@link MethodOutcome} containing the outcome of the create
     *         operation.
     */
    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        requireNonNull(resource, "resource cannot be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id cannot be null");

        var path = this.pathResolver.preferredPath(resource);
        writeResource(resource, path);

        return new MethodOutcome(resource.getIdElement(), true);
    }

    private <T extends IBaseResource> T validateResource(Class<T> resourceType, IBaseResource resource, IIdType id) {
        // All freshly read resources are tagged with their source path
        var path = (Path) resource.getUserData(SOURCE_PATH_TAG);

        if (!resourceType.getSimpleName().equals(resource.fhirType())) {
            throw new ResourceNotFoundException(
                    "Expected to find a resource with type: %s at path: %s. Found resource with type %s instead."
                            .formatted(resourceType.getSimpleName(), path, resource.fhirType()));
        }

        if (!resource.getIdElement().hasIdPart()) {
            throw new ResourceNotFoundException(
                    "Expected to find a resource with id: %s at path: %s. Found resource without an id instead."
                            .formatted(id.toUnqualifiedVersionless(), path));
        }

        if (!id.getIdPart().equals(resource.getIdElement().getIdPart())) {
            throw new ResourceNotFoundException(
                    "Expected to find a resource with id: %s at path: %s. Found resource with an id %s instead."
                            .formatted(
                                    id.getIdPart(),
                                    path,
                                    resource.getIdElement().getIdPart()));
        }

        if (id.hasVersionIdPart()
                && !id.getVersionIdPart().equals(resource.getIdElement().getVersionIdPart())) {
            throw new ResourceNotFoundException(
                    "Expected to find a resource with version: %s at path: %s. Found resource with version %s instead."
                            .formatted(
                                    id.getVersionIdPart(),
                                    path,
                                    resource.getIdElement().getVersionIdPart()));
        }

        return resourceType.cast(resource);
    }

    /**
     * Updates an existing resource in the repository.
     *
     * <p>
     * <strong>Example Usage:</strong>
     * </p>
     *
     * <pre>{@code
     * Map<String, String> headers = new HashMap<>();
     * Patient existingPatient = repository.read(Patient.class, new IdType("Patient", "12345"), headers);
     * existingPatient.addAddress().setCity("New City");
     * MethodOutcome updateOutcome = repository.update(existingPatient, headers);
     * }</pre>
     *
     * @param <T>      The type of the FHIR resource.
     * @param resource The resource to update.
     * @param headers  Additional headers (not used in this implementation).
     * @return A {@link MethodOutcome} containing the outcome of the update
     *         operation.
     */
    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        requireNonNull(resource, "resource cannot be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id cannot be null");

        var existingPath = (Path) resource.getUserData(SOURCE_PATH_TAG);

        if (existingPath != null && this.pathResolver.isExternalPath(existingPath)) {
            throw new ForbiddenOperationException(
                    "Unable to create or update: %s. Resource is marked as external, and external resources are read-only."
                            .formatted(resource.getIdElement().toUnqualifiedVersionless()));
        }

        var targetPath = this.pathResolver.preferredPath(resource);
        if (existingPath != null
                && this.conventions.encodingBehavior().preserveEncoding()
                        == PreserveEncoding.PRESERVE_ORIGINAL_ENCODING) {
            var existingEncoding = this.pathResolver.encodingForPath(existingPath);
            if (existingEncoding != null) {
                var targetFilename = this.pathResolver.filenameForEncoding(resource, existingEncoding);
                targetPath = this.pathResolver.preferredDirectory(resource).resolve(targetFilename);
            }
        }

        if (existingPath != null && !existingPath.equals(targetPath)) {
            try {
                this.resourceCache.invalidate(existingPath);
                Files.deleteIfExists(existingPath);
            } catch (IOException e) {
                throw new UnclassifiedServerFailureException(
                        500, "Couldn't relocate resource from %s to %s".formatted(existingPath, targetPath));
            }
        }

        writeResource(resource, targetPath);

        return new MethodOutcome(resource.getIdElement(), false);
    }

    /**
     * Deletes a resource from the repository.
     *
     * <p>
     * <strong>Example Usage:</strong>
     * </p>
     *
     * <pre>{@code
     * IIdType deleteId = new IdType("Patient", "67890");
     * Map<String, String> headers = new HashMap<>();
     * MethodOutcome deleteOutcome = repository.delete(Patient.class, deleteId, headers);
     * }</pre>
     *
     * @param <T>          The type of the FHIR resource.
     * @param <I>          The type of the resource identifier.
     * @param resourceType The class representing the FHIR resource type.
     * @param id           The identifier of the resource to delete.
     * @param headers      Additional headers (not used in this implementation).
     * @return A {@link MethodOutcome} containing the outcome of the delete
     *         operation.
     */
    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(id, "id cannot be null");

        var pathCandidates =
                this.pathResolver.potentialPathsForResource(resourceType, id).toList();
        var deleted = false;
        for (var path : pathCandidates) {
            try {
                deleted = Files.deleteIfExists(path);
                if (deleted) {
                    this.resourceCache.invalidate(path);
                    break;
                }
            } catch (IOException e) {
                throw new UnclassifiedServerFailureException(500, "Couldn't delete %s".formatted(path));
            }
        }

        if (!deleted) {
            throw new ResourceNotFoundException(id);
        }

        return new MethodOutcome(id);
    }

    /**
     * Searches for resources matching the given search parameters.
     *
     * <p>
     * <strong>Example Usage:</strong>
     * </p>
     *
     * <pre>{@code
     * Map<String, List<IQueryParameterType>> searchParameters = new HashMap<>();
     * searchParameters.put("family", Arrays.asList(new StringParam("Doe")));
     * Map<String, String> headers = new HashMap<>();
     * IBaseBundle bundle = repository.search(Bundle.class, Patient.class, searchParameters, headers);
     * }</pre>
     *
     * @param <B>              The type of the bundle to return.
     * @param <T>              The type of the FHIR resource.
     * @param bundleType       The class representing the bundle type.
     * @param resourceType     The class representing the FHIR resource type.
     * @param searchParameters The search parameters.
     * @param headers          Additional headers (not used in this implementation).
     * @return A bundle containing the matching resources.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        requireNonNull(bundleType, "bundleType cannot be null");
        requireNonNull(resourceType, "resourceType cannot be null");

        var resourceIdMap = readDirectoryForResourceType(resourceType);

        var builder = new BundleBuilder(this.fhirContext);
        builder.setType("searchset");

        if (searchParameters == null || searchParameters.isEmpty()) {
            resourceIdMap.values().forEach(builder::addCollectionEntry);
            return (B) builder.getBundle();
        }

        Collection<T> candidates;
        if (searchParameters.containsKey("_id")) {
            // We are consuming the _id parameter in this if statement
            candidates = getIdCandidates(searchParameters.get("_id"), resourceIdMap, resourceType);
            searchParameters.removeAll("_id");
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

    private <T extends IBaseResource> List<T> getIdCandidates(
            Collection<List<IQueryParameterType>> idQueries, Map<IIdType, T> resourceIdMap, Class<T> resourceType) {
        var idResources = new ArrayList<T>();
        for (var idQuery : idQueries) {
            for (var query : idQuery) {
                if (query instanceof TokenParam idToken) {
                    // Need to construct the equivalent "UnqualifiedVersionless" id that the map is
                    // indexed by. If an id has a version it won't match. Need apples-to-apples Id
                    // types
                    var id = Ids.newId(fhirContext, resourceType.getSimpleName(), idToken.getValue());
                    var resource = resourceIdMap.get(id);
                    if (resource != null) {
                        idResources.add(resource);
                    }
                }
            }
        }
        return idResources;
    }

    private boolean allParametersMatch(
            Multimap<String, List<IQueryParameterType>> searchParameters, IBaseResource resource) {
        for (var nextEntry : searchParameters.entries()) {
            var paramName = nextEntry.getKey();
            if (!resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Invokes a FHIR operation on a resource type.
     *
     * @param <R>          The type of the resource returned by the operation.
     * @param <P>          The type of the parameters for the operation.
     * @param <T>          The type of the resource on which the operation is
     *                     invoked.
     * @param resourceType The class representing the FHIR resource type.
     * @param name         The name of the operation.
     * @param parameters   The operation parameters.
     * @param returnType   The expected return type.
     * @param headers      Additional headers (not used in this implementation).
     * @return The result of the operation.
     */
    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return invokeOperation(null, resourceType.getSimpleName(), name, parameters);
    }

    /**
     * Invokes a FHIR operation on a specific resource instance.
     *
     * @param <R>        The type of the resource returned by the operation.
     * @param <P>        The type of the parameters for the operation.
     * @param <I>        The type of the resource identifier.
     * @param id         The identifier of the resource.
     * @param name       The name of the operation.
     * @param parameters The operation parameters.
     * @param returnType The expected return type.
     * @param headers    Additional headers (not used in this implementation).
     * @return The result of the operation.
     */
    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        return invokeOperation(id, id.getResourceType(), name, parameters);
    }

    protected <R extends IBaseResource> R invokeOperation(
            IIdType id, String resourceType, String operationName, IBaseParameters parameters) {
        if (operationProvider == null) {
            throw new IllegalArgumentException("No operation provider found. Unable to invoke operations.");
        }
        return operationProvider.invokeOperation(this, id, resourceType, operationName, parameters);
    }
}
