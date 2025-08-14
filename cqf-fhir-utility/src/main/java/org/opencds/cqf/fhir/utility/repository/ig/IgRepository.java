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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
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
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoaderInMemory;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior.PreserveEncoding;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FhirTypeLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FilenameMode;
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
 * ├── Patient-001.json
 * ├── Observation-002.json
 * ├── or
 * ├── input/
 * │   ├── [resources/]       (CategoryLayout.DIRECTORY_PER_CATEGORY)
 * │   │   ├── Patient-789.json   (FhirTypeLayout.FLAT)
 * │   │   ├── or
 * │   │   ├── [patient/]     (FhirTypeLayout.DIRECTORY_PER_TYPE)
 * │   │   │   ├── Patient-123.json   (FilenameMode.TYPE_AND_ID)
 * │   │   │   ├── or
 * │   │   │   ├── 456.json       (FilenameMode.ID_ONLY)
 * │   │   │   └── ...
 * │   │   └── ...
 * │   └── vocabulary/        (CategoryLayout.DIRECTORY_PER_CATEGORY)
 * │       ├── ValueSet-abc.json
 * │       ├── def.json
 * │       └── external/      (External Resources - Read-only, Terminology-only)
 * │           └── CodeSystem-external.json
 * └── ...
 *
 * KALM Project Layout:
 * /path/to/kalm/root/        (CategoryLayout.DEFINITIONAL_AND_DATA)
 * ├── src/
 * │   └── fhir/              (Definitional Resources)
 * │       ├── [patient/]     (CompartmentLayout.DIRECTORY_PER_COMPARTMENT)
 * │       │   └── Patient/123/
 * │       │       └── Observation-456.json
 * │       └── ...
 * └── tests/
 *     └── data/
 *         └── fhir/          (Test Data Resources)
 *             └── ...
 * </pre>
 *
 * <p>
 * <strong>Compartment Support:</strong>
 * </p>
 * <p>
 * The repository supports FHIR compartment contexts through the {@code X-FHIR-Compartment} header.
 * When using {@code CompartmentLayout.DIRECTORY_PER_COMPARTMENT}, resources are organized by
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
    private final NpmPackageLoader npmPackageLoader;

    private IRepositoryOperationProvider operationProvider;

    private final Cache<Path, Optional<IBaseResource>> resourceCache =
            CacheBuilder.newBuilder().maximumSize(5000).build();

    // Metadata fields attached to resources that are read from the repository
    // This fields are used to determine if a resource is external, and to
    // maintain the original encoding of the resource.
    static final String SOURCE_PATH_TAG = "sourcePath"; // Path

    static final String EXTERNAL_DIRECTORY = "external"; // Directory name for external resources

    // Set of directories that are used to store resources of a given category.
    // The first path is the primary directory for the category, and any additional
    // paths are considered secondary or fallback directories.
    record Directories(List<String> paths) {
        public int count() {
            return paths.size();
        }

        public Stream<String> stream() {
            return paths.stream();
        }
    }

    // Mapping of category layouts to resource categories and their corresponding directories.
    // These are relative to the root directory of the IG or the root directory of the KALM project
    static final Table<CategoryLayout, ResourceCategory, Directories> TYPE_DIRECTORIES = new ImmutableTable.Builder<
                    CategoryLayout, ResourceCategory, Directories>()
            .put(CategoryLayout.FLAT, ResourceCategory.CONTENT, new Directories(List.of("input")))
            .put(CategoryLayout.FLAT, ResourceCategory.TERMINOLOGY, new Directories(List.of("input")))
            .put(CategoryLayout.FLAT, ResourceCategory.DATA, new Directories(List.of("input")))
            .put(
                    CategoryLayout.DIRECTORY_PER_CATEGORY,
                    ResourceCategory.CONTENT,
                    new Directories(List.of("input/resources", "input/tests")))
            .put(
                    CategoryLayout.DIRECTORY_PER_CATEGORY,
                    ResourceCategory.TERMINOLOGY,
                    new Directories(List.of("input/vocabulary")))
            .put(
                    CategoryLayout.DIRECTORY_PER_CATEGORY,
                    ResourceCategory.DATA,
                    new Directories(List.of("input/tests", "input/resources")))
            .put(
                    CategoryLayout.DEFINITIONAL_AND_DATA,
                    ResourceCategory.CONTENT,
                    new Directories(List.of("src/fhir", "tests/data/fhir")))
            .put(
                    CategoryLayout.DEFINITIONAL_AND_DATA,
                    ResourceCategory.TERMINOLOGY,
                    new Directories(List.of("src/fhir", "tests/data/fhir")))
            .put(
                    CategoryLayout.DEFINITIONAL_AND_DATA,
                    ResourceCategory.DATA,
                    new Directories(List.of("tests/data/fhir", "src/fhir")))
            .build();

    static final BiMap<EncodingEnum, String> FILE_EXTENSIONS = new ImmutableBiMap.Builder<EncodingEnum, String>()
            .put(EncodingEnum.JSON, "json")
            .put(EncodingEnum.XML, "xml")
            .put(EncodingEnum.RDF, "rdf")
            .put(EncodingEnum.NDJSON, "ndjson")
            .build();

    // This header to used so that the user can pass current compartment context
    // to the repository. Basically, this will effect how the repository will do reads/writes
    // The expected format for this header is: ResourceType/Id (e.g. Patient/123)
    public static final String FHIR_COMPARTMENT_HEADER = "X-FHIR-Compartment";

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
        this.npmPackageLoader = buildNpmPackageLoader();
    }

    public NpmPackageLoader getNpmPackageLoader() {
        return npmPackageLoader;
    }

    private NpmPackageLoader buildNpmPackageLoader() {
        return NpmPackageLoaderInMemory.fromNpmPackageClasspath(getClass(), getNpmTgzPaths());
    }

    private List<Path> getNpmTgzPaths() {
        final Path npmDir = root.resolve("input/npm");

        // More often than not, the npm directory will not exist in an IgRepository
        if (!Files.exists(npmDir) || !Files.isDirectory(npmDir)) {
            return List.of();
        }

        try (Stream<Path> npmSubPaths = Files.list(npmDir)) {
            // LUKETODO: assume that all tgz files are NPM packages for now
            return npmSubPaths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".tgz"))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not resolve NPM files", exception);
        }
    }

    public void setOperationProvider(IRepositoryOperationProvider operationProvider) {
        this.operationProvider = operationProvider;
    }

    public void clearCache() {
        this.resourceCache.invalidateAll();
    }

    public void clearCache(Iterable<Path> paths) {
        this.resourceCache.invalidate(paths);
    }

    private boolean isExternalPath(Path path) {
        return path.getParent() != null
                && path.getParent().toString().toLowerCase().endsWith(EXTERNAL_DIRECTORY);
    }

    /**
     * Determines the preferred file system path for storing or retrieving a FHIR
     * resource based on its resource type, identifier, and compartment context.
     *
     * <p>
     * Example paths (based on conventions and compartment):
     * </p>
     *
     * <pre>
     * Standard IG Layout:
     * /path/to/ig/root/input/[[resources/]][[patient/]]Patient-123.json
     *
     * KALM Layout with Compartment:
     * /path/to/kalm/root/src/fhir/[[Patient/123/]]Observation-456.json
     * </pre>
     *
     * Path components depend on configuration:
     * - Category directory (e.g., `input/resources/`, `src/fhir/`) depends on `CategoryLayout`
     * - Type directory (e.g., `patient/`) depends on `FhirTypeLayout.DIRECTORY_PER_TYPE`
     * - Compartment directory (e.g., `Patient/123/`) depends on `CompartmentLayout.DIRECTORY_PER_COMPARTMENT`
     * - Filename format depends on `FilenameMode`:
     *   - `TYPE_AND_ID`: `Patient-123.json`
     *   - `ID_ONLY`: `123.json`
     *
     * @param <T>                     The type of the FHIR resource.
     * @param <I>                     The type of the resource identifier.
     * @param resourceType            The class representing the FHIR resource type.
     * @param id                      The identifier of the resource.
     * @param igRepositoryCompartment The compartment context for organizing resources.
     * @return The {@code Path} representing the preferred location for the
     *         resource.
     */
    protected <T extends IBaseResource, I extends IIdType> Path preferredPathForResource(
            Class<T> resourceType, I id, IgRepositoryCompartment igRepositoryCompartment) {
        var directory = directoryForResource(resourceType, igRepositoryCompartment);
        var fileName = fileNameForResource(
                resourceType.getSimpleName(),
                id.getIdPart(),
                this.conventions.encodingBehavior().preferredEncoding());
        return directory.findFirst().get().resolve(fileName);
    }

    /**
     * Generates all possible file paths where a resource might be found,
     * considering different encoding formats and directory structures.
     *
     * @param <T>                     The type of the FHIR resource.
     * @param <I>                     The type of the resource identifier.
     * @param resourceType            The class representing the FHIR resource type.
     * @param id                      The identifier of the resource.
     * @param igRepositoryCompartment The compartment context to use for path resolution.
     * @return A stream of potential paths for the resource.
     */
    protected <T extends IBaseResource, I extends IIdType> Stream<Path> potentialPathsForResource(
            Class<T> resourceType, I id, IgRepositoryCompartment igRepositoryCompartment) {

        var directories = directoryForResource(resourceType, igRepositoryCompartment);
        var encodings = this.conventions.encodingBehavior().enabledEncodings();
        return directories.flatMap(d -> encodings.stream()
                .map(ext -> d.resolve(fileNameForResource(resourceType.getSimpleName(), id.getIdPart(), ext))));
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
     * Determines the directory paths for a resource category, considering compartment layout.
     *
     * <p>Directory selection based on layout:</p>
     * <ul>
     * <li>{@code CategoryLayout.FLAT}: Returns the root directory (e.g., `input/`)</li>
     * <li>{@code CategoryLayout.DIRECTORY_PER_CATEGORY}: Returns category-specific
     *     subdirectories (e.g., `input/resources/`, `input/vocabulary/`)</li>
     * <li>{@code CategoryLayout.DEFINITIONAL_AND_DATA}: Returns KALM project directories
     *     (e.g., `src/fhir/`, `tests/data/fhir/`)</li>
     * </ul>
     *
     * <p>When {@code CompartmentLayout.DIRECTORY_PER_COMPARTMENT} is used with DATA resources,
     * compartment path is appended (e.g., `tests/data/fhir/Patient/123/`).</p>
     *
     * @param <T>                     The type of the FHIR resource.
     * @param resourceType            The class representing the FHIR resource type.
     * @param igRepositoryCompartment The compartment context for path resolution.
     * @return A stream of directory paths for the resource category.
     */
    protected <T extends IBaseResource> Stream<Path> directoriesForCategory(
            Class<T> resourceType, IgRepositoryCompartment igRepositoryCompartment) {
        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var categoryPaths = TYPE_DIRECTORIES.rowMap().get(this.conventions.categoryLayout()).get(category).stream()
                .map(this.root::resolve);
        if (category == ResourceCategory.DATA
                && this.conventions.compartmentLayout() == CompartmentLayout.DIRECTORY_PER_COMPARTMENT) {
            var compartmentPath = pathForCompartment(resourceType, this.fhirContext, igRepositoryCompartment);
            return categoryPaths.map(path -> path.resolve(compartmentPath));
        }

        return categoryPaths;
    }

    /**
     * Determines the directory paths for a specific resource type, including external directories
     * for terminology resources when applicable.
     *
     * <p>Directory selection based on type layout:</p>
     * <ul>
     * <li>{@code FhirTypeLayout.FLAT}: Returns the base category directory</li>
     * <li>{@code FhirTypeLayout.DIRECTORY_PER_TYPE}: Returns type-specific
     *     subdirectories within the base directory (e.g., `patient/`, `observation/`)</li>
     * </ul>
     *
     * <p>
     * Example paths (based on {@code FhirTypeLayout}):
     * </p>
     *
     * <pre>
     * Standard IG: /path/to/ig/root/input/resources/[[patient/]]
     * KALM:        /path/to/kalm/root/src/fhir/[[patient/]]
     * </pre>
     *
     * <p>Special handling for terminology resources in non-KALM projects:</p>
     * <ul>
     * <li>Includes an additional `external/` directory for read-only terminology resources</li>
     * <li>KALM projects use separate `src/` and `tests/` directories instead</li>
     * </ul>
     *
     * @param <T>                     The type of the FHIR resource.
     * @param resourceType            The class representing the FHIR resource type.
     * @param igRepositoryCompartment The compartment context for path resolution.
     * @return A stream of directory paths for the resource type.
     */
    protected <T extends IBaseResource> Stream<Path> directoryForResource(
            Class<T> resourceType, IgRepositoryCompartment igRepositoryCompartment) {
        var directories = directoriesForCategory(resourceType, igRepositoryCompartment);
        if (this.conventions.typeLayout() == FhirTypeLayout.FLAT) {
            return directories;
        }

        var resourceDirectories =
                directories.map(dir -> dir.resolve(resourceType.getSimpleName().toLowerCase()));

        var category = ResourceCategory.forType(resourceType.getSimpleName());
        if (category == ResourceCategory.TERMINOLOGY
                && this.conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
            // Non-KALM projects support "external" directory for terminology resources data
            // that is defined outside of the main IG structure, but included for convenience.
            // KALM projects separate this into "src" and "test" directories, so the "external" directory is not used.
            return resourceDirectories.flatMap(dir -> Stream.of(dir, dir.resolve(EXTERNAL_DIRECTORY)));
        }

        return resourceDirectories;
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

        var encoding = encodingForPath(path);

        try {
            String s = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            var resource = parserForEncoding(fhirContext, encoding).parseResource(s);

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
        try {
            return this.resourceCache.get(path, () -> this.readResource(path));
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }

            throw new UnclassifiedServerFailureException(500, "Unable to read resource from path %s".formatted(path));
        }
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

        return this.conventions.encodingBehavior().enabledEncodings().stream()
                .map(e -> FILE_EXTENSIONS.get(e))
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
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
    protected <T extends IBaseResource> Map<IIdType, T> readDirectoryForResourceType(
            Class<T> resourceClass, IgRepositoryCompartment igRepositoryCompartment) {
        var paths = this.directoryForResource(resourceClass, igRepositoryCompartment);

        var resources = new ConcurrentHashMap<IIdType, T>();
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

        for (var dir : paths.toList()) {
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
     * <p>Compartment context can be passed via the {@code X-FHIR-Compartment} header
     * in the format "ResourceType/Id" (e.g., "Patient/123").</p>
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
     * @param headers      Request headers, may include compartment context via {@code X-FHIR-Compartment}.
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
     * Map<String, String> headers = new HashMap<>();
     * headers.put(FHIR_COMPARTMENT_HEADER, "Patient/patient-123");
     * MethodOutcome outcome = repository.create(newObservation, headers);
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
                && this.conventions.encodingBehavior().preserveEncoding()
                        == PreserveEncoding.OVERWRITE_WITH_PREFERRED_ENCODING) {
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
        for (var path : paths.toList()) {
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
        var compartment = compartmentFrom(headers);
        var resourceIdMap = readDirectoryForResourceType(resourceType, compartment);

        BundleBuilder builder = new BundleBuilder(this.fhirContext);
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

    protected IgRepositoryCompartment compartmentFrom(Map<String, String> headers) {
        if (headers == null) {
            return new IgRepositoryCompartment();
        }

        var compartmentHeader = headers.get(FHIR_COMPARTMENT_HEADER);
        return compartmentHeader == null
                ? new IgRepositoryCompartment()
                : new IgRepositoryCompartment(compartmentHeader);
    }

    protected String pathForCompartment(
            Class<? extends IBaseResource> resourceType,
            FhirContext fhirContext,
            IgRepositoryCompartment igRepositoryCompartment) {
        if (igRepositoryCompartment.isEmpty()
                || !igRepositoryCompartment.resourceBelongsToCompartment(fhirContext, resourceType.getSimpleName())) {
            if (this.conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
                return "";
            } else {
                return "shared";
            }
        }

        // resource names are lowercase as directories
        return igRepositoryCompartment.getType().toLowerCase() + "/" + igRepositoryCompartment.getId();
    }
}
