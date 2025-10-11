package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FhirTypeLayout;

/**
 * Resolves directories for reading and writing resources according to repository conventions.
 */
class ResourcePathResolver {

    private final Path root;
    private final IgConventions conventions;
    private final FhirContext fhirContext;

    ResourcePathResolver(Path root, IgConventions conventions, FhirContext fhirContext) {
        this.root = Objects.requireNonNull(root, "root cannot be null");
        this.conventions = Objects.requireNonNull(conventions, "conventions cannot be null");
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
    }

    Path preferredDirectory(
            Class<? extends IBaseResource> resourceType, java.util.Optional<CompartmentAssignment> assignment) {
        return directoriesForResource(resourceType, assignment).stream()
                .findFirst()
                .orElse(root);
    }

    List<Path> directoriesForResource(
            Class<? extends IBaseResource> resourceType, java.util.Optional<CompartmentAssignment> assignment) {
        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var basePaths = categoryDirectories(category);
        var resolved = new ArrayList<Path>();

        for (var base : basePaths) {
            var withAssignment = applyAssignment(base, category, assignment);
            resolved.addAll(applyTypeLayout(withAssignment, category, resourceType.getSimpleName()));
        }

        return distinct(resolved);
    }

    List<Path> searchDirectories(Class<? extends IBaseResource> resourceType) {
        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var basePaths = categoryDirectories(category);
        var result = new ArrayList<Path>();

        if (conventions.typeLayout() == FhirTypeLayout.DIRECTORY_PER_TYPE) {
            for (var base : basePaths) {
                result.add(base.resolve(resourceType.getSimpleName().toLowerCase()));
            }
        }

        if (category == ResourceCategory.TERMINOLOGY
                && conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
            for (var base : basePaths) {
                result.add(base.resolve(IgRepository.EXTERNAL_DIRECTORY));
                if (conventions.typeLayout() == FhirTypeLayout.DIRECTORY_PER_TYPE) {
                    result.add(base
                            .resolve(resourceType.getSimpleName().toLowerCase())
                            .resolve(IgRepository.EXTERNAL_DIRECTORY));
                }
            }
        }

        return distinct(result);
    }

    List<Path> candidateDirectoriesForId(Class<? extends IBaseResource> resourceType, String idPart) {
        var category = ResourceCategory.forType(resourceType.getSimpleName());
        var basePaths = categoryDirectories(category);
        var result = new ArrayList<Path>();

        var compartmentMode = conventions.compartmentMode();
        var belongsToCompartment = compartmentMode != CompartmentMode.NONE
                && compartmentMode.resourceBelongsToCompartment(fhirContext, resourceType.getSimpleName());

        for (var base : basePaths) {
            if (belongsToCompartment) {
                var compartmentRoot = base.resolve(compartmentMode.type().toLowerCase());
                var idRoot = compartmentRoot.resolve(idPart);
                result.addAll(applyTypeLayout(idRoot, category, resourceType.getSimpleName()));
            }

            if (category == ResourceCategory.DATA
                    && conventions.categoryLayout() == CategoryLayout.DEFINITIONAL_AND_DATA) {
                var shared = base.resolve(CompartmentAssignment.SHARED_SEGMENT);
                result.addAll(applyTypeLayout(shared, category, resourceType.getSimpleName()));
            }

            result.addAll(applyTypeLayout(base, category, resourceType.getSimpleName()));
        }

        return distinct(result);
    }

    private List<Path> categoryDirectories(ResourceCategory category) {
        Map<ResourceCategory, IgRepository.Directories> categoryMap =
                IgRepository.TYPE_DIRECTORIES.rowMap().get(conventions.categoryLayout());

        if (categoryMap == null || !categoryMap.containsKey(category)) {
            return List.of(root);
        }

        return categoryMap.get(category).stream().map(root::resolve).collect(Collectors.toList());
    }

    private Path applyAssignment(
            Path base, ResourceCategory category, java.util.Optional<CompartmentAssignment> assignment) {
        if (category != ResourceCategory.DATA || assignment.isEmpty()) {
            return base;
        }

        var resolved = base;
        var details = assignment.get();

        if (details.compartmentSegment() != null && !details.compartmentSegment().isBlank()) {
            resolved = resolved.resolve(details.compartmentSegment());
        }

        if (details.hasContextId()) {
            resolved = resolved.resolve(details.contextId());
        }

        return resolved;
    }

    private List<Path> applyTypeLayout(Path base, ResourceCategory category, String resourceTypeName) {
        var paths = new ArrayList<Path>();

        if (conventions.typeLayout() == FhirTypeLayout.DIRECTORY_PER_TYPE) {
            var typeDir = base.resolve(resourceTypeName.toLowerCase());
            paths.add(typeDir);

            if (category == ResourceCategory.TERMINOLOGY
                    && conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
                paths.add(typeDir.resolve(IgRepository.EXTERNAL_DIRECTORY));
            }
        } else {
            paths.add(base);

            if (category == ResourceCategory.TERMINOLOGY
                    && conventions.categoryLayout() != CategoryLayout.DEFINITIONAL_AND_DATA) {
                paths.add(base.resolve(IgRepository.EXTERNAL_DIRECTORY));
            }
        }

        return paths;
    }

    private List<Path> distinct(List<Path> paths) {
        return new ArrayList<>(new LinkedHashSet<>(paths));
    }
}
