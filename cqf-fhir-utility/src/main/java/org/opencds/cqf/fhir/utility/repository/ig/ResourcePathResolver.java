package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;

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
                            List.of("src/fhir", "tests/data/fhir/shared"))
                    .put(
                            CategoryLayout.DEFINITIONAL_AND_DATA,
                            ResourceCategory.DATA,
                            List.of("tests/data/fhir", "src/fhir"))
                    .build();

    private final Path root;
    private final IgConventions conventions;
    private final FhirContext fhirContext;

    ResourcePathResolver(Path root, IgConventions conventions, FhirContext fhirContext) {
        this.root = Objects.requireNonNull(root, "root cannot be null");
        this.conventions = Objects.requireNonNull(conventions, "conventions cannot be null");
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
    }

    Path preferredDirectory(Class<? extends IBaseResource> resourceType, CompartmentAssignment assignment) {
        // In the "preferred" case, we remap an 'unknown' assignment to 'shared'
        if (assignment.isUnknown()) {
            assignment = CompartmentAssignment.shared();
        }

        return directories(resourceType, assignment).stream().findFirst().orElseThrow();
    }

    Path preferredPath(Class<? extends IBaseResource> resourceType, String idPart, CompartmentAssignment assignment) {
        return preferredDirectory(resourceType, assignment).resolve(preferredFilename(resourceType, idPart));
    }

    String preferredFilename(Class<? extends IBaseResource> resourceType, String idPart) {
        return buildFilename(
                resourceType.getSimpleName(),
                idPart,
                conventions.encodingBehavior().preferredEncoding());
    }

    // Flesh this out, ensure the the first directory is the most preferred
    List<Path> directories(Class<? extends IBaseResource> resourceType, CompartmentAssignment assignment) {
        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var bases = categoryDirectories(category);

        // TODO: Apply the compartment assignment to the base directories, "unknown" means
        // all possible. Then apply the type layout.
        return bases.stream().map(x -> root.resolve(x)).toList();
    }

    List<Path> candidates(
            Class<? extends IBaseResource> resourceType, String idPart, CompartmentAssignment assignment) {
        var directories = directories(resourceType, assignment);
        return combine(directories, candidateFilenames(resourceType, idPart));
    }

    List<String> candidateFilenames(Class<? extends IBaseResource> resourceType, String idPart) {
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
            case TYPE_AND_ID -> path -> {
                var name = path.getFileName().toString().toLowerCase();
                return name.startsWith(typeName + "-") && hasKnownExtension(path);
            };
        };
    }

    boolean isExternalPath(Path path) {
        return path.getParent() != null
                && path.getParent().toString().toLowerCase().endsWith(EXTERNAL_DIRECTORY);
    }

    private List<String> categoryDirectories(ResourceCategory category) {
        var map = BASE_DIRECTORIES.rowMap().get(conventions.categoryLayout());
        if (map == null || !map.containsKey(category)) {
            return List.of(".");
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
}
