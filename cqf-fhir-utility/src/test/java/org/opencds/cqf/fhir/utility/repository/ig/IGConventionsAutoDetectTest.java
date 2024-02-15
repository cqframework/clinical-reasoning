package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.utility.repository.ResourceDirectoryCopier;

class IGConventionsAutoDetectTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws URISyntaxException, IOException, InterruptedException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        ResourceDirectoryCopier.copyFromJar(IGRepositoryBadDataTest.class, "/sampleIgs", tempDir);
    }

    @Test
    void autoDetectDefault() {
        assertEquals(IGConventions.STANDARD, IGConventions.autoDetect(null));
        assertEquals(IGConventions.STANDARD, IGConventions.autoDetect(tempDir.resolve("does_not_exist")));
    }

    @Test
    void autoDetectStandard() {
        assertEquals(IGConventions.STANDARD, IGConventions.autoDetect(tempDir.resolve("directoryPerType/standard")));
    }

    @Test
    void autoDetectPrefix() {
        var config = IGConventions.autoDetect(tempDir.resolve("directoryPerType/prefixed"));
        assertEquals(FilenameMode.TYPE_AND_ID, config.filenameMode());
        assertEquals(CategoryLayout.DIRECTORY_PER_CATEGORY, config.categoryLayout());
        assertEquals(FhirTypeLayout.DIRECTORY_PER_TYPE, config.typeLayout());
    }

    @Test
    void autoDetectFlat() {
        assertEquals(IGConventions.FLAT, IGConventions.autoDetect(tempDir.resolve("flat")));
    }

    @Test
    void autoDetectFlatNoTypeNames() {
        var config = IGConventions.autoDetect(tempDir.resolve("flatNoTypeNames"));
        assertEquals(FilenameMode.ID_ONLY, config.filenameMode());
        assertEquals(CategoryLayout.FLAT, config.categoryLayout());
        assertEquals(FhirTypeLayout.FLAT, config.typeLayout());
    }

    @Test
    void autoDetectWithMisleadingFileName() {
        assertEquals(IGConventions.STANDARD, IGConventions.autoDetect(tempDir.resolve("misleadingFileName")));
    }
}
