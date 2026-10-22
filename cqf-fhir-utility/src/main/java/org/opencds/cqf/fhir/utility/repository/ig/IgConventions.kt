package org.opencds.cqf.fhir.utility.repository.ig

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream
import org.hl7.fhir.r4.model.Enumerations
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class represents the different file structures for an IG repository. The main differences
 * between the various configurations are whether or not the files are organized by resource type
 * and/or category, and whether or not the files are prefixed with the resource type.
 */
@JvmRecord
data class IgConventions(
    val typeLayout: FhirTypeLayout?,
    val categoryLayout: CategoryLayout?,
    val compartmentMode: CompartmentMode?,
    val compartmentIsolation: CompartmentIsolation?,
    val filenameMode: FilenameMode?,
    val encodingBehavior: EncodingBehavior?,
) {
    /** Whether or not the files are organized by resource type. */
    enum class FhirTypeLayout {
        DIRECTORY_PER_TYPE,
        FLAT,
    }

    /** Whether or not the resources are organized by category (tests, resources, vocabulary) */
    enum class CategoryLayout {
        DIRECTORY_PER_CATEGORY,
        DEFINITIONAL_AND_DATA,
        FLAT,
    }

    /** Whether or not the files are prefixed with the resource type. */
    enum class FilenameMode {
        TYPE_AND_ID,
        ID_ONLY,
    }

    /** Whether or not the data resources are fully isolated by compartment. */
    enum class CompartmentIsolation {
        FHIR, // Isolate data resources by FHIR compartment definitions
        FULL, // Isolate all data resources
    }

    override fun toString(): String {
        return "IGConventions [typeLayout=$typeLayout, categoryLayout=$categoryLayout compartmentMode=$compartmentMode, compartmentIsolation=$compartmentIsolation, filenameMode=$filenameMode]"
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IgConventions::class.java)

        @JvmField
        val FLAT: IgConventions =
            IgConventions(
                FhirTypeLayout.FLAT,
                CategoryLayout.FLAT,
                CompartmentMode.NONE,
                CompartmentIsolation.FULL,
                FilenameMode.TYPE_AND_ID,
                EncodingBehavior.Companion.DEFAULT,
            )
        @JvmField
        val STANDARD: IgConventions =
            IgConventions(
                FhirTypeLayout.DIRECTORY_PER_TYPE,
                CategoryLayout.DIRECTORY_PER_CATEGORY,
                CompartmentMode.NONE,
                CompartmentIsolation.FULL,
                FilenameMode.ID_ONLY,
                EncodingBehavior.Companion.DEFAULT,
            )

        @JvmField
        val KALM: IgConventions =
            IgConventions(
                FhirTypeLayout.DIRECTORY_PER_TYPE,
                CategoryLayout.DEFINITIONAL_AND_DATA,
                CompartmentMode.PATIENT,
                CompartmentIsolation.FHIR,
                FilenameMode.ID_ONLY,
                EncodingBehavior.Companion.KALM,
            )

        private val FHIR_TYPE_NAMES: MutableList<String?> =
            Enumerations.FHIRAllTypes.entries
                .map { obj -> obj.name }
                .map { obj -> obj.lowercase() }
                .distinct()
                .toMutableList()

        /**
         * Auto-detect the IG conventions based on the structure of the IG. If the path is null or
         * the convention can not be reliably detected, the default configuration is returned.
         *
         * @param path The path to the IG.
         * @return The IG conventions.
         * @throws IllegalArgumentException if the path is invalid or does not conform to IG
         *   conventions.
         */
        @JvmStatic
        fun autoDetect(path: Path?): IgConventions {
            var path = path
            if (path == null || !Files.exists(path)) {
                return KALM
            }

            if (Files.isDirectory(path)) {
                try {
                    Files.list(path).use { stream ->
                        if (!stream.findFirst().isPresent()) {
                            return KALM // Empty directory, return default
                        }
                    }
                } catch (e: IOException) {
                    logger.warn("Error listing files in path: {}", path, e)
                }
            }

            // Check for a `src` directory, which is used for KALM projects.
            val srcPath = path.resolve("src")
            if (Files.exists(srcPath)) {
                return KALM
            }

            // Check for an `input` directory, which is used for standard IGs.
            // If it exists, we will use that as the base path for further checks.
            path = path.resolve("input")
            require(Files.exists(path)) {
                "The provided path does not contain an 'input' or 'src' directory: " + path
            }

            // A "category" hierarchy may exist in the ig file structure,
            // where resource categories ("data", "terminology", "content") are organized into
            // subdirectories ("tests", "vocabulary", "resources").
            //
            // e.g. "input/tests", "input/vocabulary".
            //
            // Check all possible category paths and grab the first that exists,
            // or use the IG path if none exist.
            var categoryPath =
                Stream.of<String?>("tests", "vocabulary", "resources")
                    .map<Path> { other: String? -> path.resolve(other) }
                    .filter { x: Path? -> x!!.toFile().exists() }
                    .findFirst()
                    .orElse(path)

            val hasCategoryDirectory = path != categoryPath

            var compartmentMode: CompartmentMode? = CompartmentMode.NONE

            // Compartments can only exist for test data
            if (hasCategoryDirectory) {
                val tests = path.resolve("tests")
                // A compartment under the tests looks like a set of subdirectories
                // e.g. "input/tests/patient", "input/tests/practitioner"
                // that themselves contain subdirectories for each test case.
                // e.g. "input/tests/patient/test1", "input/tests/patient/test2"
                // Then within those, the structure may be flat (e.g.
                // "input/tests/patient/test1/123.json")
                // or grouped by type (e.g. "input/tests/patient/test1/patient/123.json").
                //
                // The trick is that the in the case that the test cases are
                // grouped by type, the compartment directory will be the same as the type
                // directory.
                // so we need to look at the resource type directory and check if the contents are
                // files
                // or more directories. If more directories exist, and the directory name is not a
                // FHIR type, then we have a compartment directory.
                if (tests.toFile().exists()) {
                    val potentialCompartments =
                        FHIR_TYPE_NAMES.map { other -> tests.resolve(other) }
                            .filter { path -> Files.exists(path) }

                    // Check if any of the potential compartment directories
                    // have subdirectories that are not FHIR types (e.g.
                    // "input/tests/patient/test1).
                    val compartment =
                        potentialCompartments
                            .flatMap { innerPath -> listFiles(innerPath) }
                            .filter { path -> Files.isDirectory(path) }
                            .firstOrNull { f -> !matchesAnyResourceType(f!!) } ?: categoryPath

                    val hasCompartmentDirectory = categoryPath != compartment
                    if (hasCompartmentDirectory) {
                        categoryPath = compartment

                        // Getting the parent here, because our "compartment" directory is a
                        // specific one,
                        // e.g. tests/patient/123 (not just test/patient).
                        compartmentMode =
                            CompartmentMode.Companion.fromType(
                                compartment.parent.fileName.toString()
                            )

                        require(compartmentMode != CompartmentMode.NONE) {
                            "The compartment directory does not match any known compartment type: $compartment"
                        }
                    }
                }
            }

            // A "type" may also exist in the igs file structure, where resources
            // are grouped by type into subdirectories.
            //
            // e.g. "input/vocabulary/valueset", "input/resources/valueset".
            //
            // Check all possible type paths and grab the first that exists,
            // or use the category directory if none exist
            val typePath: Path =
                FHIR_TYPE_NAMES.map { other -> categoryPath.resolve(other) }
                    .filter { path -> Files.exists(path) }
                    .firstOrNull { path -> Files.isDirectory(path) } ?: categoryPath

            val hasTypeDirectory = categoryPath != typePath

            // A file "claims" to be a FHIR resource type if its filename starts with a valid FHIR
            // type name.
            // For files that "claim" to be a FHIR resource type, we check to see if the contents of
            // the file
            // have a resource that matches the claimed type.
            val hasTypeFilename: Boolean = hasTypeFilename(typePath)

            // Should also check for all the file extension that are used in the IG
            // e.g. .json, .xml, and add them to the enabled encodings.
            val config =
                IgConventions(
                    if (hasTypeDirectory) FhirTypeLayout.DIRECTORY_PER_TYPE
                    else FhirTypeLayout.FLAT,
                    if (hasCategoryDirectory) CategoryLayout.DIRECTORY_PER_CATEGORY
                    else CategoryLayout.FLAT,
                    compartmentMode, // TODO: Cannot auto-detect this yet, default to FULL
                    // We can check for non-compartment resources in compartment directories to
                    // detect FHIR vs FULL
                    // For example, if we find a Medication resource in a Patient compartment
                    // directory,
                    // we know it is FULL isolation.
                    CompartmentIsolation.FULL,
                    if (hasTypeFilename) FilenameMode.TYPE_AND_ID else FilenameMode.ID_ONLY,
                    EncodingBehavior.Companion.DEFAULT,
                )

            logger.info("Auto-detected repository configuration: {}", config)

            return config
        }

        private fun hasTypeFilename(typePath: Path): Boolean {
            try {
                Files.list(typePath).use { fileStream ->
                    return fileStream
                        .filter { path -> Files.isRegularFile(path) }
                        .filter { innerFile -> fileNameMatchesType(innerFile) }
                        .filter { filePath ->
                            Companion.claimedFhirType(filePath!!) != Enumerations.FHIRAllTypes.NULL
                        }
                        .anyMatch { filePath ->
                            contentsMatchClaimedType(
                                filePath,
                                Companion.claimedFhirType(filePath!!),
                            )
                        }
                }
            } catch (exception: IOException) {
                logger.error("Error listing files in path: {}", typePath, exception)
                return false
            }
        }

        private fun fileNameMatchesType(innerFile: Path): Boolean {
            val fileName = innerFile.fileName.toString()
            return FHIR_TYPE_NAMES.any { type -> fileName.lowercase().startsWith(type!!) }
        }

        private fun matchesAnyResourceType(innerFile: Path): Boolean {
            return FHIR_TYPE_NAMES.contains(innerFile.fileName.toString().lowercase())
        }

        private fun listFiles(innerPath: Path): List<Path?> {
            try {
                return Files.list(innerPath).toList()
            } catch (e: IOException) {
                logger.error("Error listing files in path: {}", innerPath, e)
                return mutableListOf()
            }
        }

        // This method checks to see if the contents of a file match the type claimed by the
        // filename
        private fun contentsMatchClaimedType(
            filePath: Path,
            claimedFhirType: Enumerations.FHIRAllTypes,
        ): Boolean {

            try {
                Files.lines(filePath, StandardCharsets.UTF_8).use { linesStream ->
                    val contents = linesStream.collect(Collectors.joining())
                    if (contents.isEmpty()) {
                        return false
                    }

                    val filename = filePath.fileName.toString()
                    val fileNameWithoutExtension = filename.substring(0, filename.lastIndexOf("."))
                    // Check that the contents contain the claimed type, and that the id is not the
                    // same as the filename
                    // NOTE: This does not work for XML files.
                    return contents
                        .uppercase()
                        .contains("\"RESOURCETYPE\": \"${claimedFhirType.name}\"") &&
                        !contents
                            .uppercase()
                            .contains("\"ID\": \"${fileNameWithoutExtension.uppercase( )}\"")
                }
            } catch (e: IOException) {
                return false
            }
        }

        // Detects the FHIR type claimed by the filename
        private fun claimedFhirType(filePath: Path): Enumerations.FHIRAllTypes {
            val filename = filePath.fileName.toString()
            if (!filename.contains("-")) {
                return Enumerations.FHIRAllTypes.NULL
            }

            val codeName = filename.substring(0, filename.indexOf("-")).uppercase()
            try {
                return Enumerations.FHIRAllTypes.valueOf(codeName)
            } catch (e: Exception) {
                return Enumerations.FHIRAllTypes.NULL
            }
        }
    }
}
