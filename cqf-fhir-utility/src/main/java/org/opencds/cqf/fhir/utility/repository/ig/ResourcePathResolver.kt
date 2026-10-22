package org.opencds.cqf.fhir.utility.repository.ig

import ca.uhn.fhir.rest.api.EncodingEnum
import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Table
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.List
import java.util.function.Predicate
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.*

/** Resolves filesystem locations for resources according to IG/KALM conventions. */
class ResourcePathResolver(private val root: Path, private val conventions: IgConventions) {

    fun preferredDirectory(
        resourceType: Class<out IBaseResource?>?,
        assignment: CompartmentAssignment?,
    ): Path {
        Objects.requireNonNull(resourceType, "resourceType cannot be null")
        Objects.requireNonNull<CompartmentAssignment?>(assignment, "assignment cannot be null")
        validateAssignment(resourceType!!, assignment!!)

        // In the "preferred" case, we remap an 'unknown' assignment to 'shared'
        val effectiveAssignment: CompartmentAssignment =
            (if (assignment.isUnknown) CompartmentAssignment.Companion.shared() else assignment)
        return directories(resourceType, effectiveAssignment).stream().findFirst().orElseThrow()
    }

    fun preferredPath(
        resourceType: Class<out IBaseResource?>?,
        idPart: String?,
        assignment: CompartmentAssignment?,
    ): Path {
        Objects.requireNonNull(resourceType, "resourceType cannot be null")
        Objects.requireNonNull<String?>(idPart, "idPart cannot be null")
        Objects.requireNonNull<CompartmentAssignment?>(assignment, "assignment cannot be null")
        validateAssignment(resourceType!!, assignment!!)

        return preferredDirectory(resourceType, assignment)
            .resolve(preferredFilename(resourceType, idPart))
    }

    private fun preferredFilename(
        resourceType: Class<out IBaseResource?>,
        idPart: String?,
    ): String {
        return buildFilename(
            resourceType.simpleName,
            idPart,
            conventions.encodingBehavior!!.preferredEncoding,
        )
    }

    /**
     * Get the list of directories to search for resources of the given type and compartment
     * assignment.
     *
     * The order of the returned list reflects the search priority order, with first being highest
     * priority. (i.e. the preferred directory is the first element of the list).
     *
     * @param resourceType the FHIR resource type class
     * @param assignment the compartment assignment
     * @return a list of directories to search
     */
    fun directories(
        resourceType: Class<out IBaseResource>,
        assignment: CompartmentAssignment,
    ): MutableList<Path> {
        validateAssignment(resourceType, assignment)

        val category = ResourceCategory.Companion.forType(resourceType.simpleName)
        val bases = categoryDirectories(category)

        val typeSegment =
            if (conventions.typeLayout == FhirTypeLayout.DIRECTORY_PER_TYPE)
                resourceType.simpleName.lowercase()
            else null

        // LinkedHashSet to preserve order while ensuring uniqueness
        val paths = LinkedHashSet<Path?>()
        for (base in bases!!) {
            val basePath = root.resolve(base)
            val expanded = applyCompartmentAssignment(basePath, assignment)
            for (path in expanded) {
                val typedPath = if (typeSegment != null) path.resolve(typeSegment) else path
                paths.add(typedPath)
                if (category == ResourceCategory.TERMINOLOGY) {
                    if (
                        conventions.categoryLayout == CategoryLayout.DEFINITIONAL_AND_DATA &&
                            base == bases!!.get(0)
                    ) {
                        paths.add(basePath.resolve(EXTERNAL_DIRECTORY))
                    } else if (conventions.categoryLayout != CategoryLayout.DEFINITIONAL_AND_DATA) {
                        paths.add(typedPath.resolve(EXTERNAL_DIRECTORY))
                    }
                }
            }
        }

        return List.copyOf<Path?>(paths)
    }

    private fun validateAssignment(
        resourceType: Class<out IBaseResource>,
        assignment: CompartmentAssignment,
    ) {
        require(!(assignment.isNone && conventions.compartmentMode != CompartmentMode.NONE)) {
            "CompartmentAssignment cannot be 'none' when conventions specify compartments"
        }
        require(!(conventions.compartmentMode == CompartmentMode.NONE && !assignment.isNone)) {
            "CompartmentAssignment must be 'none' when conventions specify no compartments"
        }

        val category = ResourceCategory.Companion.forType(resourceType.getSimpleName())
        require(
            !(category == ResourceCategory.DATA &&
                (assignment.isShared) &&
                conventions.compartmentIsolation == CompartmentIsolation.FULL)
        ) {
            "Data resources cannot be assigned to a shared compartment when compartment isolation is FULL."
        }
    }

    fun candidates(
        resourceType: Class<out IBaseResource>?,
        idPart: String?,
        assignment: CompartmentAssignment?,
    ): MutableList<Path?> {
        Objects.requireNonNull(resourceType, "resourceType cannot be null")
        Objects.requireNonNull<String?>(idPart, "idPart cannot be null")
        Objects.requireNonNull<CompartmentAssignment?>(assignment, "assignment cannot be null")
        validateAssignment(resourceType!!, assignment!!)

        val directories = directories(resourceType, assignment)
        return combine(directories, candidateFilenames(resourceType, idPart))
    }

    private fun candidateFilenames(
        resourceType: Class<out IBaseResource?>,
        idPart: String?,
    ): MutableList<String> {
        return conventions.encodingBehavior!!
            .enabledEncodings!!
            .map { encoding -> buildFilename(resourceType.simpleName, idPart, encoding) }
            .toMutableList()
    }

    fun filenameForEncoding(
        resourceType: Class<out IBaseResource?>,
        idPart: String?,
        encoding: EncodingEnum?,
    ): String {
        return buildFilename(resourceType.simpleName, idPart, encoding)
    }

    fun encodingForPath(path: Path): EncodingEnum? {
        return FILE_EXTENSIONS.inverse()[fileExtension(path)]
    }

    fun fileMatcher(resourceType: Class<out IBaseResource?>): Predicate<Path?> {
        val typeName = resourceType.simpleName.lowercase()
        return when (conventions.filenameMode!!) {
            FilenameMode.ID_ONLY -> Predicate { path -> hasKnownExtension(path!!) }
            FilenameMode.TYPE_AND_ID ->
                Predicate { path ->
                    val name = path!!.fileName.toString().lowercase()
                    name.startsWith("$typeName-") && hasKnownExtension(path)
                }
        }
    }

    fun isExternalPath(path: Path): Boolean {
        return isExternalDirectory(path) ||
            (path.parent != null && isExternalDirectory(path.getParent()))
    }

    private fun isExternalDirectory(path: Path): Boolean {
        return path.fileName != null &&
            path.fileName.toString().equals(EXTERNAL_DIRECTORY, ignoreCase = true)
    }

    // Helper to get the base directories for a given category and the current layout conventions.
    // relative to the root.
    private fun categoryDirectories(category: ResourceCategory?): MutableList<String?>? {
        val map: MutableMap<ResourceCategory?, MutableList<String?>?> =
            BASE_DIRECTORIES.rowMap()[conventions.categoryLayout]!!
        check(!(map == null || !map.containsKey(category))) {
            ("No base directories configured for category " +
                category +
                " with layout " +
                conventions.categoryLayout)
        }
        return map.get(category)
    }

    private fun combine(
        directories: MutableList<Path>,
        filenames: MutableList<String>,
    ): MutableList<Path?> {
        val paths = ArrayList<Path?>()
        for (directory in directories) {
            for (name in filenames) {
                paths.add(directory.resolve(name))
            }
        }
        return paths.stream().distinct().toList()
    }

    private fun hasKnownExtension(path: Path): Boolean {
        val extension = fileExtension(path)
        return FILE_EXTENSIONS.inverse().containsKey(extension)
    }

    private fun fileExtension(path: Path): String {
        val name = path.getFileName().toString()
        val dot = name.lastIndexOf('.')
        return if (dot >= 0) name.substring(dot + 1).lowercase(Locale.getDefault()) else ""
    }

    private fun buildFilename(
        resourceTypeName: String?,
        resourceId: String?,
        encoding: EncodingEnum?,
    ): String {
        val extension: String? = FILE_EXTENSIONS.get(encoding)
        val idComponent = resourceId + "." + extension
        return when (conventions.filenameMode!!) {
            FilenameMode.ID_ONLY -> idComponent
            FilenameMode.TYPE_AND_ID -> resourceTypeName + "-" + idComponent
        }
    }

    private fun applyCompartmentAssignment(
        base: Path?,
        assignment: CompartmentAssignment?,
    ): MutableList<Path> {
        Objects.requireNonNull<Path?>(base, "base cannot be null")
        Objects.requireNonNull<CompartmentAssignment?>(assignment, "assignment cannot be null")

        if (!supportsCompartments(base!!) || assignment!!.isNone) {
            return mutableListOf(base)
        }

        if (assignment.isShared) {
            return mutableListOf(base.resolve(CompartmentAssignment.Companion.SHARED_COMPARTMENT))
        }

        if (assignment.hasCompartmentId()) {
            return mutableListOf(
                base.resolve(assignment.compartmentType).resolve(assignment.compartmentId)
            )
        }

        if (assignment.isUnknown) {
            return enumerateCompartments(base, conventions.compartmentMode!!.type().lowercase())
        }

        throw IllegalStateException("Unhandled compartment assignment: $assignment")
    }

    private fun supportsCompartments(base: Path): Boolean {
        // Swapping backslashes to handle Windows paths
        val normalized = base.toString().lowercase(Locale.getDefault()).replace('\\', '/')
        return normalized.contains("tests/data/fhir") || normalized.contains("input/tests")
    }

    private fun enumerateCompartments(base: Path, compartmentType: String): MutableList<Path> {
        val paths = ArrayList<Path>()
        val compartmentRoot = base.resolve(compartmentType)
        if (Files.isDirectory(compartmentRoot)) {
            try {
                Files.list(compartmentRoot).use { stream ->
                    stream
                        .filter { path: Path? -> Files.isDirectory(path) }
                        .forEach { e: Path? -> paths.add(e!!) }
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to enumerate compartments", e)
            }
        }

        paths.add(base.resolve(CompartmentAssignment.Companion.SHARED_COMPARTMENT))

        return paths
    }

    companion object {
        const val EXTERNAL_DIRECTORY: String = "external"

        private val FILE_EXTENSIONS: BiMap<EncodingEnum?, String?> =
            ImmutableBiMap.Builder<EncodingEnum?, String?>()
                .put(EncodingEnum.JSON, "json")
                .put(EncodingEnum.XML, "xml")
                .put(EncodingEnum.RDF, "rdf")
                .put(EncodingEnum.NDJSON, "ndjson")
                .build()

        // This table defines the base directories for each combination of category layout and
        // resource category.
        // The paths are relative to the IG root, and will be further modified based on compartment
        // assignment and type
        // layout.
        // The order of the lists defines the search/read/write priority (index 0 is the preferred
        // directory).
        private val BASE_DIRECTORIES:
            Table<CategoryLayout?, ResourceCategory?, MutableList<String?>?> =
            ImmutableTable.Builder<CategoryLayout?, ResourceCategory?, MutableList<String?>?>()
                .put(CategoryLayout.FLAT, ResourceCategory.CONTENT, mutableListOf<String?>("input"))
                .put(
                    CategoryLayout.FLAT,
                    ResourceCategory.TERMINOLOGY,
                    mutableListOf<String?>("input"),
                )
                .put(CategoryLayout.FLAT, ResourceCategory.DATA, mutableListOf<String?>("input"))
                .put(
                    CategoryLayout.DIRECTORY_PER_CATEGORY,
                    ResourceCategory.CONTENT,
                    mutableListOf<String?>("input/resources", "input/tests"),
                )
                .put(
                    CategoryLayout.DIRECTORY_PER_CATEGORY,
                    ResourceCategory.TERMINOLOGY,
                    mutableListOf<String?>("input/vocabulary", "input/tests/vocabulary"),
                )
                .put(
                    CategoryLayout.DIRECTORY_PER_CATEGORY,
                    ResourceCategory.DATA,
                    mutableListOf<String?>("input/tests", "input/resources"),
                )
                .put(
                    CategoryLayout.DEFINITIONAL_AND_DATA,
                    ResourceCategory.CONTENT,
                    mutableListOf<String?>("src/fhir", "tests/data/fhir"),
                )
                .put(
                    CategoryLayout.DEFINITIONAL_AND_DATA,
                    ResourceCategory.TERMINOLOGY,
                    mutableListOf<String?>("src/fhir", "tests/data/fhir"),
                )
                .put(
                    CategoryLayout.DEFINITIONAL_AND_DATA,
                    ResourceCategory.DATA,
                    mutableListOf<String?>("tests/data/fhir", "src/fhir"),
                )
                .build()
    }
}
