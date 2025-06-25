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
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior.PreserveEncoding;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FhirTypeLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FilenameMode;
import org.opencds.cqf.fhir.utility.repository.operations.IRepositoryOperationProvider;

/**
 * Provides access to FHIR resources stored in a directory structure following
 * Implementation Guide (IG) conventions.
 * Supports CRUD operations and resource management based on IG directory and
 * file naming conventions.
 *
 * <p>
 * <strong>Directory Structure Overview (based on conventions):</strong>
 * </p>
 *
 * <pre>
 * /path/to/ig/root/          (CategoryLayout.FLAT)
 * ├── Patient-001.json
 * ├── Observation-002.json
 * ├── or
 * ├── [resources/]             (CategoryLayout.DIRECTORY_PER_CATEGORY)
 * │   ├── Patient-789.json       (FhirTypeLayout.FLAT)
 * │   ├── or
 * │   ├── [patient/]           (FhirTypeLayout.DIRECTORY_PER_TYPE)
 * │   │   ├── Patient-123.json   (FilenameMode.TYPE_AND_ID)
 * │   │   ├── or
 * │   │   ├── 456.json           (FilenameMode.ID_ONLY)
 * │   │   └── ...
 * │   └── ...
 * └── vocabulary/              (CategoryLayout.DIRECTORY_PER_CATEGORY)
 *     ├── ValueSet-abc.json
 *     ├── def.json
 *     └── external/            (External Resources - Read-only, Terminology-only)
 *         └── CodeSystem-external.json
 * </pre>
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Supports CRUD operations on FHIR resources.</li>
 * <li>Handles different directory layouts and filename conventions based on IG
 * conventions.</li>
 * <li>Annotates resources with metadata like source path and external
 * designation.</li>
 * <li>Supports invoking FHIR operations through an
 * {@link IRepositoryOperationProvider}.</li>
 * <li>Utilizes caching for efficient resource access.</li>
 * </ul>
 */
public class IgRepository implements IRepository {

    private final FhirContext fhirContext;
    private final Path root;
    private final IgConventions conventions;
    private final EncodingBehavior encodingBehavior;
    private final ResourceMatcher resourceMatcher;
    private IRepositoryOperationProvider operationProvider;

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

    // This header to used so that the user can pass current compartment context
    // to the repository. Basically, this will effect how the repository will do reads/writes
    // The expected format for this header is: ResourceType/Id (e.g. Patient/123)
    public static final String FHIR_COMPARTMENT_HEADER = "X-FHIR-Compartment";

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
        this(fhirContext, root, IgConventions.autoDetect(root), EncodingBehavior.DEFAULT, null);
    }

    /**
     * Creates a new {@code IgRepository} with specified conventions and encoding
     * behavior.
     *
     * @param fhirContext       The FHIR context to use.
     * @param root              The root directory of the IG.
     * @param conventions       The conventions defining directory and filename
     *                          structures.
     * @param encodingBehavior  The encoding behavior for parsing and encoding
     *                          resources.
     * @param operationProvider The operation provider for invoking FHIR operations.
     */
    public IgRepository(
            FhirContext fhirContext,
            Path root,
            IgConventions conventions,
            EncodingBehavior encodingBehavior,
            IRepositoryOperationProvider operationProvider) {
        this.fhirContext = requireNonNull(fhirContext, "fhirContext cannot be null");
        this.root = requireNonNull(root, "root cannot be null");
        this.conventions = requireNonNull(conventions, "conventions is required");
        this.encodingBehavior = requireNonNull(encodingBehavior, "encodingBehavior is required");
        this.resourceMatcher = Repositories.getResourceMatcher(this.fhirContext);
        this.operationProvider = operationProvider;
    }

    public void setOperationProvider(IRepositoryOperationProvider operationProvider) {
        this.operationProvider = operationProvider;
    }

    public void clearCache() {
        this.resourceCache.clear();
    }

    private boolean isExternalPath(Path path) {
        return path.getParent() != null
                && path.getParent().toString().toLowerCase().endsWith(EXTERNAL_DIRECTORY);
    }

    /**
     * Determines the preferred file system path for storing or retrieving a FHIR
     * resource based on its resource type and identifier.
     *
     * <p>
     * Example (based on conventions):
     * </p>
     *
     * <pre>
     * /path/to/ig/root/[[resources/]][[patient/]]Patient-123.json
     * </pre>
     *
     * - The presence of `resources/` depends on
     * `CategoryLayout.DIRECTORY_PER_CATEGORY`.
     * - The presence of `patient/` depends on `FhirTypeLayout.DIRECTORY_PER_TYPE`.
     * - The filename format depends on `FilenameMode`:
     * - `TYPE_AND_ID`: `Patient-123.json`
     * - `ID_ONLY`: `123.json`
     *
     * @param <T>          The type of the FHIR resource.
     * @param <I>          The type of the resource identifier.
     * @param resourceType The class representing the FHIR resource type.
     * @param id           The identifier of the resource.
     * @return The {@code Path} representing the preferred location for the
     *         resource.
     */
    protected <T extends IBaseResource, I extends IIdType> Path preferredPathForResource(
            Class<T> resourceType, I id, IgRepositoryCompartment igRepositoryCompartment) {
        var directory = directoryForResource(resourceType, igRepositoryCompartment);
        var fileName = fileNameForResource(
                resourceType.getSimpleName(), id.getIdPart(), this.encodingBehavior.preferredEncoding());
        return directory.resolve(fileName);
    }

    /**
     * Generates all possible file paths where a resource might be found.
     *
     * @param <T>          The type of the FHIR resource.
     * @param <I>          The type of the resource identifier.
     * @param resourceType The class representing the FHIR resource type.
     * @param id           The identifier of the resource.
     * @param igRepositoryCompartment  The compartment context to use
     * @return A list of potential paths for the resource.
     */
    protected <T extends IBaseResource, I extends IIdType> List<Path> potentialPathsForResource(
            Class<T> resourceType, I id, IgRepositoryCompartment igRepositoryCompartment) {

        var potentialDirectories = new ArrayList<Path>();
        var directory = directoryForResource(resourceType, igRepositoryCompartment);
        potentialDirectories.add(directory);

        // Currently, only terminology resources are allowed to be external
        if (ResourceCategory.forType(resourceType.getSimpleName()) == ResourceCategory.TERMINOLOGY) {
            var externalDirectory = directory.resolve(EXTERNAL_DIRECTORY);
            potentialDirectories.add(externalDirectory);
        }

        var potentialPaths = new ArrayList<Path>();

        for (var dir : potentialDirectories) {
            for (var encoding : FILE_EXTENSIONS.keySet()) {
                potentialPaths.add(
                        dir.resolve(fileNameForResource(resourceType.getSimpleName(), id.getIdPart(), encoding)));
            }
        }

        return potentialPaths;
    }

    /**
     * Constructs the filename based on conventions:
     * - ID_ONLY: "123.json"
     * - TYPE_AND_ID: "Patient-123.json"
     *
     * @param resourceType The resource type (e.g., "Patient").
     * @param resourceId   The resource ID (e.g., "123").
     * @param encoding     The encoding (e.g., JSON).
     * @return The filename.
     */
    protected String fileNameForResource(String resourceType, String resourceId, EncodingEnum encoding) {
        var name = resourceId + "." + FILE_EXTENSIONS.get(encoding);
        if (FilenameMode.ID_ONLY.equals(conventions.filenameMode())) {
            return name;
        } else {
            return resourceType + "-" + name;
        }
    }

    /**
     * Determines the directory path for a resource category.
     *
     * - `CategoryLayout.FLAT`: Returns the root directory.
     * - `CategoryLayout.DIRECTORY_PER_CATEGORY`: Returns the category-specific
     * subdirectory (e.g., `/resources/`).
     *
     * @param <T>          The type of the FHIR resource.
     * @param resourceType The class representing the FHIR resource type.
     * @param igRepositoryCompartment The compartment context to use
     * @return The path representing the directory for the resource category.
     */
    protected <T extends IBaseResource> Path directoryForCategory(Class<T> resourceType, IgRepositoryCompartment igRepositoryCompartment) {
        if (this.conventions.categoryLayout() == CategoryLayout.FLAT) {
            return this.root;
        }

        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var directory = CATEGORY_DIRECTORIES.get(category);
        var path = root.resolve(directory);
        if (category == ResourceCategory.DATA
                && this.conventions.compartmentLayout() == CompartmentLayout.DIRECTORY_PER_COMPARTMENT) {
            path = path.resolve(pathForCompartment(igRepositoryCompartment));
        }

        return path;
    }

    /**
     * Determines the directory path for a resource type.
     *
     * - If `FhirTypeLayout.FLAT`, returns the base directory (could be root or
     * category directory).
     * - If `FhirTypeLayout.DIRECTORY_PER_TYPE`, returns the type-specific
     * subdirectory within the base directory.
     *
     * <p>
     * Example (based on `FhirTypeLayout`):
     * </p>
     *
     * <pre>
     * /path/to/ig/root/[[patient/]]
     * </pre>
     *
     * - `[[patient/]]` is present if `FhirTypeLayout.DIRECTORY_PER_TYPE` is used.
     *
     * @param <T>          The type of the FHIR resource.
     * @param resourceType The class representing the FHIR resource type.
     * @param igRepositoryCompartment The compartment context to use
     * @return The path representing the directory for the resource type.
     */
    protected <T extends IBaseResource> Path directoryForResource(Class<T> resourceType, IgRepositoryCompartment igRepositoryCompartment) {
        var directory = directoryForCategory(resourceType, igRepositoryCompartment);
        if (this.conventions.typeLayout() == FhirTypeLayout.FLAT) {
            return directory;
        }

        return directory.resolve(resourceType.getSimpleName().toLowerCase());
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

        var extension = fileExtension(path);
        if (extension == null) {
            return Optional.empty();
        }

        var encoding = FILE_EXTENSIONS.inverse().get(extension);

        try (var stream = new FileInputStream(file)) {
            var resource = parserForEncoding(fhirContext, encoding).parseResource(stream);

            resource.setUserData(SOURCE_PATH_TAG, path);
            CqlContent.loadCqlContent(resource, path.getParent());

            return Optional.of(resource);
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (DataFormatException e) {
            throw new ResourceNotFoundException("Found empty or invalid content at path %s".formatted(path));
        } catch (IOException e) {
            throw new UnclassifiedServerFailureException(500, "Unable to read resource from path %s".formatted(path));
        }
    }

    protected Optional<IBaseResource> cachedReadResource(Path path) {
        return this.resourceCache.computeIfAbsent(path, this::readResource);
    }

    protected EncodingEnum encodingForPath(Path path) {
        var extension = fileExtension(path);
        return FILE_EXTENSIONS.inverse().get(extension);
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
                String result = parserForEncoding(fhirContext, encodingForPath(path))
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

    /**
     * Reads all resources of a given type from the directory.
     *
     * Directory structure depends on conventions:
     * - Flat layout: resources are located in the root directory (e.g.,
     * "/path/to/ig/root/")
     * - Directory for category: resources are in subdirectories (e.g.,
     * "/resources/patient/")
     *
     * Filenames depend on conventions:
     * - ID_ONLY: "123.json"
     * - TYPE_AND_ID: "Patient-123.json"
     *
     * @param <T>           The resource type.
     * @param resourceClass The resource class.
     * @param igRepositoryCompartment The compartment context to use
     * @return Map of resource IDs to resources.
     */
    protected <T extends IBaseResource> Map<IIdType, T> readDirectoryForResourceType(
            Class<T> resourceClass, IgRepositoryCompartment igRepositoryCompartment) {
        var path = this.directoryForResource(resourceClass, igRepositoryCompartment);
        if (!path.toFile().exists()) {
            return Collections.emptyMap();
        }

        var resources = new HashMap<IIdType, T>();
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
            paths.filter(resourceFileFilter)
                    .sorted()
                    .map(this::cachedReadResource)
                    .flatMap(Optional::stream)
                    .forEach(r -> {
                        if (!r.fhirType().equals(resourceClass.getSimpleName())) {
                            return;
                        }

                        T validatedResource = validateResource(resourceClass, r, r.getIdElement());
                        resources.put(r.getIdElement().toUnqualifiedVersionless(), validatedResource);
                    });

        } catch (IOException e) {
            throw new UnclassifiedServerFailureException(500, "Unable to read resources from path: %s".formatted(path));
        }

        return resources;
    }

    @Override
    public FhirContext fhirContext() {
        return this.fhirContext;
    }

    /**
     * Reads a resource from the repository.
     *
     * Locates files like:
     * - ID_ONLY: "123.json" (in the appropriate directory based on layout)
     * - TYPE_AND_ID: "Patient-123.json"
     *
     * Utilizes cache to improve performance.
     *
     * <p>
     * <strong>Example Usage:</strong>
     * </p>
     *
     * <pre>{@code
     * IIdType resourceId = new IdType("Patient", "12345");
     * Map<String, String> headers = new HashMap<>();
     * Patient patient = repository.read(Patient.class, resourceId, headers);
     * }</pre>
     *
     * @param <T>          The type of the FHIR resource.
     * @param <I>          The type of the resource identifier.
     * @param resourceType The class representing the FHIR resource type.
     * @param id           The identifier of the resource.
     * @param headers      Additional headers (not used in this implementation).
     * @return The resource if found.
     * @throws ResourceNotFoundException if the resource is not found.
     */
    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(id, "id cannot be null");

        var compartment = compartmentFrom(headers);

        var paths = this.potentialPathsForResource(resourceType, id, compartment);
        for (var path : paths) {
            if (!path.toFile().exists()) {
                continue;
            }

            var optionalResource = cachedReadResource(path);
            if (optionalResource.isPresent()) {
                var resource = optionalResource.get();
                return validateResource(resourceType, resource, id);
            }
        }

        throw new ResourceNotFoundException(id);
    }

    /**
     * Creates a new resource in the repository.
     *
     * <p>
     * <strong>Example Usage:</strong>
     * </p>
     *
     * <pre>{@code
     * Patient newPatient = new Patient();
     * newPatient.setId("67890");
     * newPatient.addName().setFamily("Doe").addGiven("John");
     * Map<String, String> headers = new HashMap<>();
     * MethodOutcome outcome = repository.create(newPatient, headers);
     * }</pre>
     *
     * @param <T>      The type of the FHIR resource.
     * @param resource The resource to create.
     * @param headers  Additional headers (not used in this implementation).
     * @return A {@link MethodOutcome} containing the outcome of the create
     *         operation.
     */
    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        requireNonNull(resource, "resource cannot be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id cannot be null");

        var compartment = compartmentFrom(headers);

        var path = this.preferredPathForResource(resource.getClass(), resource.getIdElement(), compartment);
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

        var compartment = compartmentFrom(headers);

        var preferred = this.preferredPathForResource(resource.getClass(), resource.getIdElement(), compartment);
        var actual = (Path) resource.getUserData(SOURCE_PATH_TAG);
        if (actual == null) {
            actual = preferred;
        }

        if (isExternalPath(actual)) {
            throw new ForbiddenOperationException(
                    "Unable to create or update: %s. Resource is marked as external, and external resources are read-only."
                            .formatted(resource.getIdElement().toUnqualifiedVersionless()));
        }

        // If the preferred path and the actual path are different, and the encoding
        // behavior is set to overwrite,
        // move the resource to the preferred path and delete the old one.
        if (!preferred.equals(actual)
                && this.encodingBehavior.preserveEncoding() == PreserveEncoding.OVERWRITE_WITH_PREFERRED_ENCODING) {
            try {
                Files.deleteIfExists(actual);
            } catch (IOException e) {
                throw new UnclassifiedServerFailureException(500, "Couldn't change encoding for %s".formatted(actual));
            }

            actual = preferred;
        }

        writeResource(resource, actual);

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

        var compartment = compartmentFrom(headers);
        var paths = this.potentialPathsForResource(resourceType, id, compartment);
        boolean deleted = false;
        for (var path : paths) {
            try {
                deleted = Files.deleteIfExists(path);
                if (deleted) {
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
        BundleBuilder builder = new BundleBuilder(this.fhirContext);
        builder.setType("searchset");

        var compartment = compartmentFrom(headers);

        var resourceIdMap = readDirectoryForResourceType(resourceType, compartment);
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

    protected IgRepositoryCompartment compartmentFrom(Map<String, String> headers) {
        if (headers == null) {
            return new IgRepositoryCompartment();
        }

        var compartmentHeader = headers.get(FHIR_COMPARTMENT_HEADER);
        return compartmentHeader == null ? new IgRepositoryCompartment() : new IgRepositoryCompartment(compartmentHeader);
    }

    // Patient context is a special-case. We don't tack the compartment context on
    // the end of the path. We just use the id as the directory.
    protected String pathForCompartment(IgRepositoryCompartment igRepositoryCompartment) {
        if (igRepositoryCompartment.isEmpty()) {
            return "";
        }

        return igRepositoryCompartment.getType() + "/" + igRepositoryCompartment.getId();
    }
}
