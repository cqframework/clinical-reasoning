package org.opencds.cqf.fhir.utility.repository.ig;

import com.google.common.io.Files;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the different file structures for an IG repository. The main differences between the
 * various configurations are whether or not the files are organized by resource type and/or category, and whether
 * or not the files are prefixed with the resource type.
 */
public final class IgConventions {

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
        FLAT
    }

    /**
     * Whether or not the files are prefixed with the resource type.
     */
    public enum FilenameMode {
        TYPE_AND_ID,
        ID_ONLY
    }

    public static final IgConventions FLAT =
            new IgConventions(FhirTypeLayout.FLAT, CategoryLayout.FLAT, FilenameMode.TYPE_AND_ID);
    public static final IgConventions STANDARD = new IgConventions(
            FhirTypeLayout.DIRECTORY_PER_TYPE, CategoryLayout.DIRECTORY_PER_CATEGORY, FilenameMode.ID_ONLY);

    private static final Logger LOG = LoggerFactory.getLogger(IgConventions.class);

    /**
     * Creates new IGConventions with the given typeLayout, categoryLayout, and filenameMode.
     *
     * NOTE: The preferred way to create an IGConventions is to use the autoDetect method or one of the static instances, STANDARD or FLAT. The only cases where this constructor should be used is if the IG repository configuration is known ahead of time and is non-standard.
     *
     * @param typeLayout
     * @param categoryLayout
     * @param filenameMode
     */
    public IgConventions(FhirTypeLayout typeLayout, CategoryLayout categoryLayout, FilenameMode filenameMode) {
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
     * Auto-detect the IG conventions based on the structure of the IG.
     * If the path is null or the convention can not be reliably detected,
     * the default configuration is returned.
     *
     * @param path The path to the IG.
     *
     * @return The IG conventions.
     */
    public static IgConventions autoDetect(Path path) {
        if (path == null || !path.toFile().exists()) {
            return STANDARD;
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

        // A "type" may also exist in the igs file structure, where resources
        // are grouped by type into subdirectories.
        //
        // e.g. "input/vocabulary/valueset", "input/resources/valueset".
        //
        // Check all possible type paths and grab the first that exists,
        // or use the category directory if none exist
        var typePath = Stream.of(FHIRAllTypes.values())
                .map(FHIRAllTypes::name)
                .map(String::toLowerCase)
                .map(categoryPath::resolve)
                .filter(x -> x.toFile().exists())
                .findFirst()
                .orElse(categoryPath);

        var hasTypeDirectory = !categoryPath.equals(typePath);

        // Potential resource files are files that contain a "." and have a valid FHIR file extension.
        FilenameFilter resourceFileFilter = (dir, name) -> name.contains(".")
                && IgRepository.FILE_EXTENSIONS.containsValue(name.toLowerCase().substring(name.lastIndexOf('.') + 1));
        var potentialResourceFiles = typePath.toFile().listFiles(resourceFileFilter);

        // A file "claims" to be a FHIR resource type if its filename starts with a valid FHIR type name.
        // For files that "claim" to be a FHIR resource type, we check to see if the contents of the file
        // have a resource that matches the claimed type.
        var hasTypeFilename = Stream.of(potentialResourceFiles)
                .filter(file -> claimedFhirType(file) != FHIRAllTypes.NULL)
                .anyMatch(file -> contentsMatchClaimedType(file, claimedFhirType(file)));

        var config = new IgConventions(
                hasTypeDirectory ? FhirTypeLayout.DIRECTORY_PER_TYPE : FhirTypeLayout.FLAT,
                hasCategoryDirectory ? CategoryLayout.DIRECTORY_PER_CATEGORY : CategoryLayout.FLAT,
                hasTypeFilename ? FilenameMode.TYPE_AND_ID : FilenameMode.ID_ONLY);

        LOG.info("Auto-detected repository configuration: {}", config);

        return config;
    }

    // This method checks to see if the contents of a file match the type claimed by the filename
    private static boolean contentsMatchClaimedType(File file, FHIRAllTypes claimedFhirType) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(claimedFhirType);

        try {
            @SuppressWarnings("null")
            var contents = Files.asCharSource(file, StandardCharsets.UTF_8).read();
            if (contents == null || contents.isEmpty()) {
                return false;
            }

            return contents.toUpperCase().contains(String.format("\"RESOURCETYPE\": \"%s\"", claimedFhirType.name()));
        } catch (IOException e) {
            return false;
        }
    }

    // Detects the FHIR type claimed by the filename
    private static FHIRAllTypes claimedFhirType(File file) {
        var filename = file.getName();
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((typeLayout == null) ? 0 : typeLayout.hashCode());
        result = prime * result + ((categoryLayout == null) ? 0 : categoryLayout.hashCode());
        result = prime * result + ((filenameMode == null) ? 0 : filenameMode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        IgConventions other = (IgConventions) obj;
        if (typeLayout != other.typeLayout) return false;
        if (categoryLayout != other.categoryLayout) return false;
        return filenameMode == other.filenameMode;
    }

    @Override
    public String toString() {
        return String.format(
                "IGConventions [typeLayout=%s, categoryLayout=%s, filenameMode=%s]",
                typeLayout, categoryLayout, filenameMode);
    }
}
