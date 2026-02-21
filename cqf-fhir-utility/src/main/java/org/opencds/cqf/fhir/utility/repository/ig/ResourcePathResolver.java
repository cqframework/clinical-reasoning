package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.api.EncodingEnum;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentIsolation;

/**
 * Resolves filesystem locations for resources according to IG/KALM conventions.
 */
class ResourcePathResolver {

    static final String EXTERNAL_DIRECTORY = "external";

    private static final BiMap<EncodingEnum, String> FILE_EXTENSIONS = new ImmutableBiMap.Builder<
                    EncodingEnum, String>()
            .put(EncodingEnum.JSON, "json")
            .put(EncodingEnum.XML, "xml")
            .put(EncodingEnum.RDF, "rdf")
            .put(EncodingEnum.NDJSON, "ndjson")
            .build();

    // This table defines the base directories for each combination of category layout and resource category.
    // The paths are relative to the IG root, and will be further modified based on compartment assignment and type
    // layout.
    // The order of the lists defines the search/read/write priority (index 0 is the preferred directory).
    private static final Table<CategoryLayout, ResourceCategory, List<String>> BASE_DIRECTORIES =
            new ImmutableTable.Builder<CategoryLayout, ResourceCategory, List<String>>()
                    .put(CategoryLayout.FLAT, ResourceCategory.CONTENT, List.of("input"))
                    .put(CategoryLayout.FLAT, ResourceCategory.TERMINOLOGY, List.of("input"))
                    .put(CategoryLayout.FLAT, ResourceCategory.DATA, List.of("input"))
                    .put(
                            CategoryLayout.DIRECTORY_PER_CATEGORY,
                            ResourceCategory.CONTENT,
                            List.of("input/resources", "input/tests"))
                    .put(
                            CategoryLayout.DIRECTORY_PER_CATEGORY,
                            ResourceCategory.TERMINOLOGY,
                            List.of("input/vocabulary", "input/tests/vocabulary"))
                    .put(
                            CategoryLayout.DIRECTORY_PER_CATEGORY,
                            ResourceCategory.DATA,
                            List.of("input/tests", "input/resources"))
                    .put(
                            CategoryLayout.DEFINITIONAL_AND_DATA,
                            ResourceCategory.CONTENT,
                            List.of("src/fhir", "tests/data/fhir"))
                    .put(
                            CategoryLayout.DEFINITIONAL_AND_DATA,
                            ResourceCategory.TERMINOLOGY,
                            List.of("src/fhir", "tests/data/fhir"))
                    .put(
                            CategoryLayout.DEFINITIONAL_AND_DATA,
                            ResourceCategory.DATA,
                            List.of("tests/data/fhir", "src/fhir"))
                    .build();

    private final Path root;
    private final IgConventions conventions;

    ResourcePathResolver(Path root, IgConventions conventions) {
        this.root = Objects.requireNonNull(root, "root cannot be null");
        this.conventions = Objects.requireNonNull(conventions, "conventions cannot be null");
    }

    Path preferredDirectory(Class<? extends IBaseResource> resourceType, CompartmentAssignment assignment) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(assignment, "assignment cannot be null");
        validateAssignment(resourceType, assignment);

        // In the "preferred" case, we remap an 'unknown' assignment to 'shared'
        var effectiveAssignment = assignment.isUnknown() ? CompartmentAssignment.shared() : assignment;
        return directories(resourceType, effectiveAssignment).stream()
                .findFirst()
                .orElseThrow();
    }

    Path preferredPath(Class<? extends IBaseResource> resourceType, String idPart, CompartmentAssignment assignment) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(idPart, "idPart cannot be null");
        requireNonNull(assignment, "assignment cannot be null");
        validateAssignment(resourceType, assignment);

        return preferredDirectory(resourceType, assignment).resolve(preferredFilename(resourceType, idPart));
    }

    private String preferredFilename(Class<? extends IBaseResource> resourceType, String idPart) {
        return buildFilename(
                resourceType.getSimpleName(),
                idPart,
                conventions.encodingBehavior().preferredEncoding());
    }

    /**
     * Get the list of directories to search for resources of the given type and compartment assignment.
     *
     * The order of the returned list reflects the search priority order, with first being highest priority.
     * (i.e. the preferred directory is the first element of the list).
     * @param resourceType the FHIR resource type class
     * @param assignment the compartment assignment
     * @return a list of directories to search
     */
    List<Path> directories(Class<? extends IBaseResource> resourceType, CompartmentAssignment assignment) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(assignment, "assignment cannot be null");
        validateAssignment(resourceType, assignment);

        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var bases = categoryDirectories(category);

        var typeSegment = conventions.typeLayout() == IgConventions.FhirTypeLayout.DIRECTORY_PER_TYPE
                ? resourceType.getSimpleName().toLowerCase()
                : null;

        // LinkedHashSet to preserve order while ensuring uniqueness
        var paths = new LinkedHashSet<Path>();
        for (var base : bases) {
            var basePath = root.resolve(base);
            var expanded = applyCompartmentAssignment(basePath, assignment);
            for (var path : expanded) {
                var typedPath = typeSegment != null ? path.resolve(typeSegment) : path;
                paths.add(typedPath);
                // Terminology resources may also be found in an 'external' directory for some conventions
                if (category == ResourceCategory.TERMINOLOGY
                        && conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
                    paths.add(typedPath.resolve(EXTERNAL_DIRECTORY));
                }
            }
        }

        return List.copyOf(paths);
    }

    private void validateAssignment(Class<? extends IBaseResource> resourceType, CompartmentAssignment assignment) {
        if (assignment.isNone() && conventions.compartmentMode() != CompartmentMode.NONE) {
            throw new IllegalArgumentException(
                    "CompartmentAssignment cannot be 'none' when conventions specify compartments");
        } else if (conventions.compartmentMode() == CompartmentMode.NONE && !assignment.isNone()) {
            throw new IllegalArgumentException(
                    "CompartmentAssignment must be 'none' when conventions specify no compartments");
        }

        var category = ResourceCategory.forType(resourceType.getSimpleName());
        if (category == ResourceCategory.DATA
                && (assignment.isShared())
                && conventions.compartmentIsolation() == CompartmentIsolation.FULL) {
            throw new IllegalArgumentException(
                    "Data resources cannot be assigned to a shared compartment when compartment isolation is FULL.");
        }
    }

    List<Path> candidates(
            Class<? extends IBaseResource> resourceType, String idPart, CompartmentAssignment assignment) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(idPart, "idPart cannot be null");
        requireNonNull(assignment, "assignment cannot be null");
        validateAssignment(resourceType, assignment);

        var directories = directories(resourceType, assignment);
        return combine(directories, candidateFilenames(resourceType, idPart));
    }

    private List<String> candidateFilenames(Class<? extends IBaseResource> resourceType, String idPart) {
        return conventions.encodingBehavior().enabledEncodings().stream()
                .map(encoding -> buildFilename(resourceType.getSimpleName(), idPart, encoding))
                .toList();
    }

    String filenameForEncoding(Class<? extends IBaseResource> resourceType, String idPart, EncodingEnum encoding) {
        return buildFilename(resourceType.getSimpleName(), idPart, encoding);
    }

    EncodingEnum encodingForPath(Path path) {
        return FILE_EXTENSIONS.inverse().get(fileExtension(path));
    }

    Predicate<Path> fileMatcher(Class<? extends IBaseResource> resourceType) {
        var typeName = resourceType.getSimpleName().toLowerCase();
        return switch (conventions.filenameMode()) {
            case ID_ONLY -> path -> hasKnownExtension(path);
            case TYPE_AND_ID ->
                path -> {
                    var name = path.getFileName().toString().toLowerCase();
                    return name.startsWith(typeName + "-") && hasKnownExtension(path);
                };
        };
    }

    boolean isExternalPath(Path path) {
        return path.getParent() != null
                && path.getParent().toString().toLowerCase().endsWith(EXTERNAL_DIRECTORY);
    }

    // Helper to get the base directories for a given category and the current layout conventions.
    // relative to the root.
    private List<String> categoryDirectories(ResourceCategory category) {
        var map = BASE_DIRECTORIES.rowMap().get(conventions.categoryLayout());
        if (map == null || !map.containsKey(category)) {
            throw new IllegalStateException("No base directories configured for category " + category + " with layout "
                    + conventions.categoryLayout());
        }
        return map.get(category);
    }

    private List<Path> combine(List<Path> directories, List<String> filenames) {
        var paths = new ArrayList<Path>();
        for (var directory : directories) {
            for (var name : filenames) {
                paths.add(directory.resolve(name));
            }
        }
        return paths.stream().distinct().toList();
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

    private String buildFilename(String resourceTypeName, String resourceId, EncodingEnum encoding) {
        var extension = FILE_EXTENSIONS.get(encoding);
        var idComponent = resourceId + "." + extension;
        return switch (conventions.filenameMode()) {
            case ID_ONLY -> idComponent;
            case TYPE_AND_ID -> resourceTypeName + "-" + idComponent;
        };
    }

    private List<Path> applyCompartmentAssignment(Path base, CompartmentAssignment assignment) {
        requireNonNull(base, "base cannot be null");
        requireNonNull(assignment, "assignment cannot be null");

        if (!supportsCompartments(base) || assignment.isNone()) {
            return List.of(base);
        }

        if (assignment.isShared()) {
            return List.of(base.resolve(CompartmentAssignment.SHARED_COMPARTMENT));
        }

        if (assignment.hasCompartmentId()) {
            return List.of(base.resolve(assignment.compartmentType()).resolve(assignment.compartmentId()));
        }

        if (assignment.isUnknown()) {
            return enumerateCompartments(
                    base, conventions.compartmentMode().type().toLowerCase());
        }

        throw new IllegalStateException("Unhandled compartment assignment: " + assignment);
    }

    private boolean supportsCompartments(Path base) {
        // Swapping backslashes to handle Windows paths
        var normalized = base.toString().toLowerCase().replace('\\', '/');
        return normalized.contains("tests/data/fhir") || normalized.contains("input/tests");
    }

    private List<Path> enumerateCompartments(Path base, String compartmentType) {
        var paths = new ArrayList<Path>();
        var compartmentRoot = base.resolve(compartmentType);
        if (Files.isDirectory(compartmentRoot)) {
            try (var stream = Files.list(compartmentRoot)) {
                stream.filter(Files::isDirectory).forEach(paths::add);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to enumerate compartments", e);
            }
        }

        paths.add(base.resolve(CompartmentAssignment.SHARED_COMPARTMENT));

        return paths;
    }
}
