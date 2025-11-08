package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the different file structures for an IG repository. The main differences
 * between the various configurations are whether or not the files are organized by resource type
 * and/or category, and whether or not the files are prefixed with the resource type.
 */
public record IgConventions(
        org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FhirTypeLayout typeLayout,
        org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout categoryLayout,
        org.opencds.cqf.fhir.utility.repository.ig.CompartmentMode compartmentMode,
        org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentIsolation compartmentIsolation,
        org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FilenameMode filenameMode,
        org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior encodingBehavior) {

    private static final Logger logger = LoggerFactory.getLogger(IgConventions.class);

    /**
     * Whether or not the files are organized by resource type.
     */
    public enum FhirTypeLayout {
        DIRECTORY_PER_TYPE,
        FLAT
    }

    /**
     * Whether or not the resources are organized by category (tests, resources, vocabulary)
     */
    public enum CategoryLayout {
        DIRECTORY_PER_CATEGORY,
        DEFINITIONAL_AND_DATA,
        FLAT
    }

    /**
     * Whether or not the files are prefixed with the resource type.
     */
    public enum FilenameMode {
        TYPE_AND_ID,
        ID_ONLY
    }

    /**
     * Whether or not the data resources are fully isolated by compartment.
     */
    public enum CompartmentIsolation {
        FHIR, // Isolate data resources by FHIR compartment definitions
        FULL // Isolate all data resources
    }

    public static final IgConventions FLAT = new IgConventions(
            FhirTypeLayout.FLAT,
            CategoryLayout.FLAT,
            CompartmentMode.NONE,
            CompartmentIsolation.FULL,
            FilenameMode.TYPE_AND_ID,
            EncodingBehavior.DEFAULT);
    public static final IgConventions STANDARD = new IgConventions(
            FhirTypeLayout.DIRECTORY_PER_TYPE,
            CategoryLayout.DIRECTORY_PER_CATEGORY,
            CompartmentMode.NONE,
            CompartmentIsolation.FULL,
            FilenameMode.ID_ONLY,
            EncodingBehavior.DEFAULT);

    public static final IgConventions KALM = new IgConventions(
            FhirTypeLayout.DIRECTORY_PER_TYPE,
            CategoryLayout.DEFINITIONAL_AND_DATA,
            CompartmentMode.PATIENT,
            CompartmentIsolation.FHIR,
            FilenameMode.ID_ONLY,
            EncodingBehavior.KALM);

    private static final List<String> FHIR_TYPE_NAMES = Stream.of(FHIRAllTypes.values())
            .map(FHIRAllTypes::name)
            .map(String::toLowerCase)
            .distinct()
            .toList();

    /**
     * Auto-detect the IG conventions based on the structure of the IG. If the path is null or the
     * convention can not be reliably detected, the default configuration is returned.
     *
     * @param path The path to the IG.
     * @return The IG conventions.
     * @throws IllegalArgumentException if the path is invalid or does not conform to IG conventions.
     */
    public static IgConventions autoDetect(Path path) {
        if (path == null || !Files.exists(path)) {
            return KALM;
        }

        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                if (!stream.findFirst().isPresent()) {
                    return KALM; // Empty directory, return default
                }
            } catch (IOException e) {
                logger.warn("Error listing files in path: {}", path, e);
            }
        }

        // Check for a `src` directory, which is used for KALM projects.
        var srcPath = path.resolve("src");
        if (Files.exists(srcPath)) {
            return KALM;
        }

        // Check for an `input` directory, which is used for standard IGs.
        // If it exists, we will use that as the base path for further checks.
        path = path.resolve("input");
        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    "The provided path does not contain an 'input' or 'src' directory: " + path);
        }

        // A "category" hierarchy may exist in the ig file structure,
        // where resource categories ("data", "terminology", "content") are organized into
        // subdirectories ("tests", "vocabulary", "resources").
        //
        // e.g. "input/tests", "input/vocabulary".
        //
        // Check all possible category paths and grab the first that exists,
        // or use the IG path if none exist.
        var categoryPath = Stream.of("tests", "vocabulary", "resources")
                .map(path::resolve)
                .filter(x -> x.toFile().exists())
                .findFirst()
                .orElse(path);

        var hasCategoryDirectory = !path.equals(categoryPath);

        var compartmentMode = CompartmentMode.NONE;

        // Compartments can only exist for test data
        if (hasCategoryDirectory) {
            var tests = path.resolve("tests");
            // A compartment under the tests looks like a set of subdirectories
            // e.g. "input/tests/patient", "input/tests/practitioner"
            // that themselves contain subdirectories for each test case.
            // e.g. "input/tests/patient/test1", "input/tests/patient/test2"
            // Then within those, the structure may be flat (e.g. "input/tests/patient/test1/123.json")
            // or grouped by type (e.g. "input/tests/patient/test1/patient/123.json").
            //
            // The trick is that the in the case that the test cases are
            // grouped by type, the compartment directory will be the same as the type directory.
            // so we need to look at the resource type directory and check if the contents are files
            // or more directories. If more directories exist, and the directory name is not a
            // FHIR type, then we have a compartment directory.
            if (tests.toFile().exists()) {
                var potentialCompartments =
                        FHIR_TYPE_NAMES.stream().map(tests::resolve).filter(Files::exists);

                // Check if any of the potential compartment directories
                // have subdirectories that are not FHIR types (e.g. "input/tests/patient/test1).
                var compartment = potentialCompartments
                        .flatMap(IgConventions::listFiles)
                        .filter(Files::isDirectory)
                        .filter(f -> !matchesAnyResourceType(f))
                        .findFirst()
                        .orElse(categoryPath);

                var hasCompartmentDirectory = !categoryPath.equals(compartment);
                if (hasCompartmentDirectory) {
                    categoryPath = compartment;

                    // Getting the parent here, because our "compartment" directory is a specific one,
                    // e.g. tests/patient/123 (not just test/patient).
                    compartmentMode = CompartmentMode.fromType(
                            compartment.getParent().getFileName().toString());

                    if (compartmentMode == CompartmentMode.NONE) {
                        throw new IllegalArgumentException(
                                "The compartment directory does not match any known compartment type: " + compartment);
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
        var typePath = FHIR_TYPE_NAMES.stream()
                .map(categoryPath::resolve)
                .filter(Files::exists)
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(categoryPath);

        var hasTypeDirectory = !categoryPath.equals(typePath);

        // A file "claims" to be a FHIR resource type if its filename starts with a valid FHIR type name.
        // For files that "claim" to be a FHIR resource type, we check to see if the contents of the file
        // have a resource that matches the claimed type.
        var hasTypeFilename = hasTypeFilename(typePath);

        // Should also check for all the file extension that are used in the IG
        // e.g. .json, .xml, and add them to the enabled encodings.

        var config = new IgConventions(
                hasTypeDirectory ? FhirTypeLayout.DIRECTORY_PER_TYPE : FhirTypeLayout.FLAT,
                hasCategoryDirectory ? CategoryLayout.DIRECTORY_PER_CATEGORY : CategoryLayout.FLAT,
                compartmentMode,
                // TODO: Cannot auto-detect this yet, default to FULL
                // We can check for non-compartment resources in compartment directories to detect FHIR vs FULL
                // For example, if we find a Medication resource in a Patient compartment directory,
                // we know it is FULL isolation.
                CompartmentIsolation.FULL,
                hasTypeFilename ? FilenameMode.TYPE_AND_ID : FilenameMode.ID_ONLY,
                EncodingBehavior.DEFAULT);

        logger.info("Auto-detected repository configuration: {}", config);

        return config;
    }

    private static boolean hasTypeFilename(Path typePath) {
        try (var fileStream = Files.list(typePath)) {
            return fileStream
                    .filter(Files::isRegularFile)
                    .filter(IgConventions::fileNameMatchesType)
                    .filter(filePath -> claimedFhirType(filePath) != FHIRAllTypes.NULL)
                    .anyMatch(filePath -> contentsMatchClaimedType(filePath, claimedFhirType(filePath)));
        } catch (IOException exception) {
            logger.error("Error listing files in path: {}", typePath, exception);
            return false;
        }
    }

    private static boolean fileNameMatchesType(Path innerFile) {
        requireNonNull(innerFile);
        var fileName = innerFile.getFileName().toString();
        return FHIR_TYPE_NAMES.stream().anyMatch(type -> fileName.toLowerCase().startsWith(type));
    }

    private static boolean matchesAnyResourceType(Path innerFile) {
        requireNonNull(innerFile);
        return FHIR_TYPE_NAMES.contains(innerFile.getFileName().toString().toLowerCase());
    }

    private static Stream<Path> listFiles(Path innerPath) {
        try {
            return Files.list(innerPath);
        } catch (IOException e) {
            logger.error("Error listing files in path: {}", innerPath, e);
            return Stream.empty();
        }
    }

    // This method checks to see if the contents of a file match the type claimed by the filename
    private static boolean contentsMatchClaimedType(Path filePath, FHIRAllTypes claimedFhirType) {
        Objects.requireNonNull(filePath);
        Objects.requireNonNull(claimedFhirType);

        try (var linesStream = Files.lines(filePath, StandardCharsets.UTF_8)) {
            var contents = linesStream.collect(Collectors.joining());
            if (contents.isEmpty()) {
                return false;
            }

            var filename = filePath.getFileName().toString();
            var fileNameWithoutExtension = filename.substring(0, filename.lastIndexOf("."));
            // Check that the contents contain the claimed type, and that the id is not the same as the filename
            // NOTE: This does not work for XML files.
            return contents.toUpperCase().contains("\"RESOURCETYPE\": \"%s\"".formatted(claimedFhirType.name()))
                    && !contents.toUpperCase()
                            .contains("\"ID\": \"%s\"".formatted(fileNameWithoutExtension.toUpperCase()));

        } catch (IOException e) {
            return false;
        }
    }

    // Detects the FHIR type claimed by the filename
    private static FHIRAllTypes claimedFhirType(Path filePath) {
        var filename = filePath.getFileName().toString();
        if (!filename.contains("-")) {
            return FHIRAllTypes.NULL;
        }

        var codeName = filename.substring(0, filename.indexOf("-")).toUpperCase();
        try {
            return FHIRAllTypes.valueOf(codeName);
        } catch (Exception e) {
            return FHIRAllTypes.NULL;
        }
    }

    @Override
    public String toString() {
        return "IGConventions [typeLayout=%s, categoryLayout=%s compartmentMode=%s, compartmentIsolation=%s, filenameMode=%s]"
                .formatted(typeLayout, categoryLayout, compartmentMode, compartmentIsolation, filenameMode);
    }
}
