package org.opencds.cqf.fhir.utility.repository.ig;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This enum represents the different file structures for an IG repository. The main differences between the
 * various configurations are whether or not the files are organized by resource type and/or category, and whether
 * or not the files are prefixed with the resource type.
 */
public enum IGRepositoryConfig {
    FLAT(FhirTypeLayout.FLAT, CategoryLayout.FLAT, FilenameMode.ID_ONLY),
    FLAT_WITH_TYPE_NAMES(FhirTypeLayout.FLAT, CategoryLayout.FLAT, FilenameMode.TYPE_AND_ID),
    WITH_CATEGORY_DIRECTORY_AND_TYPE_NAMES(
            FhirTypeLayout.FLAT, CategoryLayout.DIRECTORY_PER_CATEGORY, FilenameMode.TYPE_AND_ID),
    WITH_CATEGORY_AND_TYPE_DIRECTORIES(
            FhirTypeLayout.DIRECTORY_PER_TYPE, CategoryLayout.DIRECTORY_PER_CATEGORY, FilenameMode.ID_ONLY),
    WITH_CATEGORY_AND_TYPE_DIRECTORIES_AND_TYPE_NAMES(
            FhirTypeLayout.DIRECTORY_PER_TYPE, CategoryLayout.DIRECTORY_PER_CATEGORY, FilenameMode.TYPE_AND_ID);

    private static final Logger LOG = LoggerFactory.getLogger(IGRepositoryConfig.class);

    private IGRepositoryConfig(FhirTypeLayout typeLayout, CategoryLayout categoryLayout, FilenameMode filenameMode) {
        this.typeLayout = typeLayout;
        this.categoryLayout = categoryLayout;
        this.filenameMode = filenameMode;
    }

    private final FhirTypeLayout typeLayout;
    private final CategoryLayout categoryLayout;
    private final FilenameMode filenameMode;

    FhirTypeLayout typeLayout() {
        return typeLayout;
    }

    CategoryLayout categoryLayout() {
        return categoryLayout;
    }

    FilenameMode filenameMode() {
        return filenameMode;
    }

    /**
     * Auto-detect the IG repository configuration based on the structure of the IG. If the path is null or the configuration can not be reliably detected,
     * the default configuration is returned.
     *
     * @param path The path to the IG.
     *
     * @return The IG repository configuration.
     */
    public static IGRepositoryConfig autoDetect(Path path) {
        if (path == null || !path.toFile().exists()) {
            return WITH_CATEGORY_AND_TYPE_DIRECTORIES;
        }

        // Check all possible category paths and grab the first that exists,
        // or use the IG path if none exist. The "resources" directory is a special case
        // since flat igs also contain that one.
        var categoryDirectory = Stream.of("tests", "vocabulary")
                .map(path::resolve)
                .map(Path::toFile)
                .filter(File::exists)
                .map(File::toPath)
                .findFirst()
                .orElse(path);

        var hasCategoryDirectory = !path.equals(categoryDirectory);

        // If the category directory is the same as the IG path
        // then we need to check for the "resources" directory
        if (!hasCategoryDirectory) {
            var resourcesDirectory = path.resolve("resources");
            if (resourcesDirectory.toFile().exists()) {
                categoryDirectory = resourcesDirectory;
            }
        }

        // Check all possible type paths and grab the first that exists,
        // or use the category directory if none exist
        var typeDirectory = Stream.of(FHIRAllTypes.values())
                .map(FHIRAllTypes::name)
                .map(String::toLowerCase)
                .map(categoryDirectory::resolve)
                .map(Path::toFile)
                .filter(File::exists)
                .map(File::toPath)
                .findFirst()
                .orElse(categoryDirectory);

        var hasTypeDirectory = !categoryDirectory.equals(typeDirectory);

        FilenameFilter resourceFileFilter = (dir, name) -> name.contains(".")
                && IGRepository.FILE_EXTENSIONS.containsValue(name.toLowerCase().substring(name.lastIndexOf('.')));

        var files = typeDirectory.toFile().listFiles(resourceFileFilter);

        var hasTypeFilename = Stream.of(files)
                .filter(x -> x.getName().contains("-"))
                .map(x -> x.getName().substring(0, x.getName().indexOf("-")))
                // This handles the case that the type in the filename is lowercase
                .map(x -> x.substring(0, 1).toUpperCase() + x.substring(1))
                .distinct()
                .anyMatch(x -> FHIRAllTypes.fromCode(x) != null);

        if (hasCategoryDirectory && hasTypeDirectory && hasTypeFilename) {
            LOG.info("Auto-detected IG repository configuration: WITH_CATEGORY_AND_TYPE_DIRECTORIES_AND_TYPE_NAMES");
            return WITH_CATEGORY_AND_TYPE_DIRECTORIES_AND_TYPE_NAMES;
        } else if (hasCategoryDirectory && hasTypeDirectory) {
            LOG.info("Auto-detected IG repository configuration: WITH_CATEGORY_AND_TYPE_DIRECTORIES");
            return WITH_CATEGORY_AND_TYPE_DIRECTORIES;
        } else if (hasCategoryDirectory && hasTypeFilename) {
            LOG.info("Auto-detected IG repository configuration: WITH_CATEGORY_DIRECTORY");
            return WITH_CATEGORY_DIRECTORY_AND_TYPE_NAMES;
        } else if (hasTypeFilename) {
            LOG.info("Auto-detected IG repository configuration: FLAT_WITH_TYPE_NAMES");
            return FLAT_WITH_TYPE_NAMES;
        } else if (!hasCategoryDirectory && !hasTypeDirectory) {
            LOG.info("Auto-detected IG repository configuration: FLAT");
            return FLAT;
        }

        LOG.warn(
                "Detected an unsupported IG repository configuration. Defaulting to WITH_CATEGORY_AND_TYPE_DIRECTORIES");
        return WITH_CATEGORY_AND_TYPE_DIRECTORIES;
    }
}
