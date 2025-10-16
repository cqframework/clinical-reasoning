package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FhirTypeLayout;

/**
 * Resolves directories for reading and writing resources according to repository conventions.
 */
class ResourcePathResolver {

    static final String EXTERNAL_DIRECTORY = "external";
    private static final String KALM_DATA_ROOT = "tests/data/fhir";

    private static final BiMap<EncodingEnum, String> FILE_EXTENSIONS = new ImmutableBiMap.Builder<
                    EncodingEnum, String>()
            .put(EncodingEnum.JSON, "json")
            .put(EncodingEnum.XML, "xml")
            .put(EncodingEnum.RDF, "rdf")
            .put(EncodingEnum.NDJSON, "ndjson")
            .build();

    private record Directories(List<String> paths) {}

    private enum DirectoryIntent {
        CANDIDATE,
        SEARCH
    }

    private static final Table<CategoryLayout, ResourceCategory, Directories> TYPE_DIRECTORIES =
            new ImmutableTable.Builder<CategoryLayout, ResourceCategory, Directories>()
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
                            new Directories(List.of("input/vocabulary", "input/tests/vocabulary")))
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
                            new Directories(List.of("src/fhir", "tests/data/fhir/shared")))
                    .put(
                            CategoryLayout.DEFINITIONAL_AND_DATA,
                            ResourceCategory.DATA,
                            new Directories(List.of("tests/data/fhir", "src/fhir")))
                    .build();

    private final Path root;
    private final IgConventions conventions;
    private final FhirContext fhirContext;
    private final CompartmentAssigner compartmentAssigner;

    /**
     * Creates a resolver that understands the directory structure described by the supplied conventions.
     *
     * @param root the filesystem root used for all resolved paths
     * @param conventions the IG conventions controlling layout semantics
     * @param fhirContext the FHIR context used to determine compartment membership heuristics
     */
    ResourcePathResolver(Path root, IgConventions conventions, FhirContext fhirContext) {
        this.root = Objects.requireNonNull(root, "root cannot be null");
        this.conventions = Objects.requireNonNull(conventions, "conventions cannot be null");
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
        this.compartmentAssigner = new CompartmentAssigner(
                this.fhirContext, this.conventions.compartmentMode(), this.conventions.categoryLayout());
    }

    /**
     * Returns the highest-priority directory for the supplied resource type and compartment assignment.
     */
    Path preferredDirectory(Class<? extends IBaseResource> resourceType, CompartmentAssignment assignment) {
        return directoriesForResource(resourceType, assignment).stream()
                .findFirst()
                .orElse(root);
    }

    /**
     * Computes the preferred directory for a concrete resource instance.
     */
    Path preferredDirectory(IBaseResource resource) {
        var assignment = this.compartmentAssigner.assign(resource);
        return preferredDirectory(resource.getClass(), assignment);
    }

    /**
     * Resolves the full preferred file path for the supplied resource instance.
     */
    Path preferredPath(IBaseResource resource) {
        return preferredDirectory(resource).resolve(preferredFilename(resource));
    }

    /**
     * Resolves the full preferred file path for the supplied resource metadata.
     */
    Path preferredPath(Class<? extends IBaseResource> resourceType, String idPart, CompartmentAssignment assignment) {
        return preferredDirectory(resourceType, assignment).resolve(preferredFilename(resourceType, idPart));
    }

    /**
     * Computes the preferred filename for the supplied resource instance.
     */
    String preferredFilename(IBaseResource resource) {
        return preferredFilename(resource.getClass(), resource.getIdElement().getIdPart());
    }

    /**
     * Computes the preferred filename for the supplied resource metadata.
     */
    String preferredFilename(Class<? extends IBaseResource> resourceType, String idPart) {
        return buildFilename(
                resourceType.getSimpleName(),
                idPart,
                conventions.encodingBehavior().preferredEncoding());
    }

    /**
     * Produces all directories that should be considered for storing the specified resource type, in priority order.
     *
     * @param resourceType the resource type being resolved
     * @param resolution compartment assignment influencing the directory path
     * @return ordered directories suitable for storing the resource
     */
    List<Path> directoriesForResource(Class<? extends IBaseResource> resourceType, CompartmentAssignment assignment) {
        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var basePaths = categoryDirectories(category);
        var resolved = new ArrayList<Path>();

        for (var base : basePaths) {
            var withAssignment = applyAssignment(base, category, assignment);
            resolved.addAll(applyTypeLayout(withAssignment, category, resourceType.getSimpleName()));
        }

        return distinct(resolved);
    }

    /**
     * Enumerates directories that must be scanned to satisfy a search for the provided resource type.
     * The directories are restricted to shallow traversal semantics defined by the conventions.
     *
     * @param resourceType the resource type being searched
     * @return ordered directories that should be enumerated during search
     */
    List<Path> searchDirectories(Class<? extends IBaseResource> resourceType) {
        return collectDirectories(resourceType, DirectoryIntent.SEARCH);
    }

    /**
     * Provides candidate directories that might contain a resource with the given identifier. This list is used for
     * targeted lookups prior to falling back on broader scans.
     *
     * @param resourceType the resource type being resolved
     * @return ordered directories that may contain the resource
     */
    List<Path> candidateDirectoriesForResource(Class<? extends IBaseResource> resourceType) {
        return collectDirectories(resourceType, DirectoryIntent.CANDIDATE);
    }

    /**
     * Returns candidate file paths for a resource identifier using the configured directory priorities.
     */
    List<Path> candidateFiles(Class<? extends IBaseResource> resourceType, String idPart) {
        return combine(candidateDirectoriesForResource(resourceType), candidateFilenames(resourceType, idPart));
    }

    /**
     * Returns search paths for a resource identifier by expanding all search directories and filename permutations.
     */
    List<Path> searchFiles(Class<? extends IBaseResource> resourceType, String idPart) {
        return combine(searchDirectories(resourceType), candidateFilenames(resourceType, idPart));
    }

    /**
     * Produces file name permutations for all enabled encodings.
     */
    List<String> candidateFilenames(Class<? extends IBaseResource> resourceType, String idPart) {
        return conventions.encodingBehavior().enabledEncodings().stream()
                .map(encoding -> buildFilename(resourceType.getSimpleName(), idPart, encoding))
                .toList();
    }

    /**
     * Produces a filename using the specified encoding.
     */
    String filenameForEncoding(IBaseResource resource, EncodingEnum encoding) {
        return filenameForEncoding(
                resource.getClass(),
                resource.getIdElement().toUnqualifiedVersionless().getIdPart(),
                encoding);
    }

    /**
     * Produces a filename using the specified encoding for a resource type.
     */
    String filenameForEncoding(Class<? extends IBaseResource> resourceType, String idPart, EncodingEnum encoding) {
        return buildFilename(resourceType.getSimpleName(), idPart, encoding);
    }

    /**
     * Resolves the encoding associated with a path based on its extension.
     */
    EncodingEnum encodingForPath(Path path) {
        return FILE_EXTENSIONS.inverse().get(fileExtension(path));
    }

    /**
     * Builds a predicate that matches filenames for the given resource type according to repository conventions.
     */
    Predicate<Path> fileMatcher(Class<? extends IBaseResource> resourceType) {
        var typeName = resourceType.getSimpleName().toLowerCase();
        return switch (conventions.filenameMode()) {
            case ID_ONLY -> path -> hasKnownExtension(path);
            case TYPE_AND_ID -> path -> {
                var name = path.getFileName().toString().toLowerCase();
                return name.startsWith(typeName + "-") && hasKnownExtension(path);
            };
        };
    }

    private List<Path> collectDirectories(Class<? extends IBaseResource> resourceType, DirectoryIntent intent) {
        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var basePaths = categoryDirectories(category);
        var result = new ArrayList<Path>();

        var compartmentMode = conventions.compartmentMode();
        var resourceTypeName = resourceType.getSimpleName();
        var belongsToCompartment = compartmentMode != CompartmentMode.NONE
                && compartmentMode.resourceBelongsToCompartment(fhirContext, resourceTypeName);

        for (var base : basePaths) {
            if (category == ResourceCategory.DATA
                    && conventions.categoryLayout() == CategoryLayout.DEFINITIONAL_AND_DATA) {
                var includeRoot = intent == DirectoryIntent.SEARCH || !belongsToCompartment;
                addDefinitionalDataDirectories(base, resourceTypeName, belongsToCompartment, includeRoot, result);
                continue;
            }

            if (belongsToCompartment) {
                var compartmentRoot = base.resolve(compartmentMode.type().toLowerCase());
                result.addAll(applyTypeLayoutToChildren(compartmentRoot, category, resourceTypeName));
            }

            result.addAll(applyTypeLayout(base, category, resourceTypeName));
        }

        return distinct(result);
    }

    private Path applyAssignment(Path base, ResourceCategory category, CompartmentAssignment assignment) {
        if (category != ResourceCategory.DATA || assignment == null || !assignment.isPresent()) {
            return base;
        }

        var resolved = base;
        if (assignment.compartmentType() != null
                && !assignment.compartmentType().isBlank()) {
            resolved = resolved.resolve(assignment.compartmentType());
        }

        if (assignment.hasCompartmentId()) {
            resolved = resolved.resolve(assignment.compartmentId());
        }

        return resolved;
    }

    private List<Path> categoryDirectories(ResourceCategory category) {
        Map<ResourceCategory, Directories> categoryMap =
                TYPE_DIRECTORIES.rowMap().get(conventions.categoryLayout());

        if (categoryMap == null || !categoryMap.containsKey(category)) {
            return List.of(root);
        }

        return categoryMap.get(category).paths().stream().map(root::resolve).collect(Collectors.toList());
    }

    private List<Path> applyTypeLayout(Path base, ResourceCategory category, String resourceTypeName) {
        var paths = new ArrayList<Path>();

        if (conventions.typeLayout() == FhirTypeLayout.DIRECTORY_PER_TYPE) {
            var typeDir = base.resolve(resourceTypeName.toLowerCase());
            paths.add(typeDir);

            if (category == ResourceCategory.TERMINOLOGY
                    && conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
                paths.add(typeDir.resolve(EXTERNAL_DIRECTORY));
            }
        } else {
            paths.add(base);

            if (category == ResourceCategory.TERMINOLOGY
                    && conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
                paths.add(base.resolve(EXTERNAL_DIRECTORY));
            }
        }

        return paths;
    }

    private List<Path> applyTypeLayoutToChildren(Path parent, ResourceCategory category, String resourceTypeName) {
        if (parent == null || !Files.exists(parent) || !Files.isDirectory(parent)) {
            return List.of();
        }

        var paths = new ArrayList<Path>();
        try (var stream = Files.list(parent)) {
            stream.filter(Files::isDirectory)
                    .forEach(child -> paths.addAll(applyTypeLayout(child, category, resourceTypeName)));
        } catch (IOException ignored) {
            // ignore directories we can't read
        }
        return paths;
    }

    private List<Path> distinct(List<Path> paths) {
        return new ArrayList<>(new LinkedHashSet<>(paths));
    }

    private void addDefinitionalDataDirectories(
            Path base, String resourceTypeName, boolean belongsToCompartment, boolean includeRoot, List<Path> result) {
        var relative = relativePath(base);
        var isDataRoot = KALM_DATA_ROOT.equals(relative);

        if (isDataRoot && belongsToCompartment) {
            var compartmentRoot =
                    base.resolve(conventions.compartmentMode().type().toLowerCase());
            result.addAll(applyTypeLayoutToChildren(compartmentRoot, ResourceCategory.DATA, resourceTypeName));
        }

        if (isDataRoot) {
            var shared = base.resolve(CompartmentAssignment.SHARED_COMPARTMENT);
            result.addAll(applyTypeLayout(shared, ResourceCategory.DATA, resourceTypeName));
            if (includeRoot) {
                result.addAll(applyTypeLayout(base, ResourceCategory.DATA, resourceTypeName));
            }
        } else {
            result.addAll(applyTypeLayout(base, ResourceCategory.DATA, resourceTypeName));
        }
    }

    private String relativePath(Path absolute) {
        try {
            var relative = root.relativize(absolute).toString();
            return relative.replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return absolute.toString().replace('\\', '/');
        }
    }

    private boolean hasKnownExtension(Path path) {
        var extension = fileExtension(path);
        return FILE_EXTENSIONS.inverse().containsKey(extension);
    }

    private String fileExtension(Path path) {
        var name = path.getFileName().toString();
        var dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private List<Path> combine(List<Path> directories, List<String> filenames) {
        var paths = new ArrayList<Path>();
        for (var directory : directories) {
            for (var name : filenames) {
                paths.add(directory.resolve(name));
            }
        }
        return distinct(paths);
    }

    private String buildFilename(String resourceTypeName, String resourceId, EncodingEnum encoding) {
        var extension = FILE_EXTENSIONS.get(encoding);
        var idComponent = resourceId + "." + extension;

        return switch (conventions.filenameMode()) {
            case ID_ONLY -> idComponent;
            case TYPE_AND_ID -> resourceTypeName + "-" + idComponent;
        };
    }

    /**
     * Returns {@code true} when the supplied path resides within an {@code external/} directory.
     */
    public boolean isExternalPath(Path path) {
        return path.getParent() != null
                && path.getParent().toString().toLowerCase().endsWith(EXTERNAL_DIRECTORY);
    }

    /**
     * Generates all possible file paths where a resource might be found,
     * considering different encoding formats and directory structures.
     *
     * @param <T>                     The type of the FHIR resource.
     * @param <I>                     The type of the resource identifier.
     * @param resourceType            The class representing the FHIR resource type.
     * @param id                      The identifier of the resource.
     * @return A stream of potential paths for the resource.
     */
    public <T extends IBaseResource, I extends IIdType> Stream<Path> potentialPathsForResource(
            Class<T> resourceType, I id) {
        var results = new LinkedHashSet<Path>();

        var candidateFiles = this.candidateFiles(resourceType, id.getIdPart());
        for (var candidate : candidateFiles) {
            try {
                if (Files.exists(candidate)) {
                    results.add(candidate);
                }
            } catch (SecurityException ignored) {
                // ignore directories we cannot inspect
            }
        }

        if (!results.isEmpty()) {
            return results.stream();
        }

        var searchFiles = this.searchFiles(resourceType, id.getIdPart());
        for (var file : searchFiles) {
            try {
                if (Files.exists(file)) {
                    results.add(file);
                }
            } catch (SecurityException ignored) {
                // ignore directories we cannot inspect
            }
        }

        return results.stream();
    }
}
