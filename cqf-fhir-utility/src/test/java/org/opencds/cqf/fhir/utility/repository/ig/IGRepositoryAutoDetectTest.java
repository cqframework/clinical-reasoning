package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.utility.repository.ResourceDirectoryCopier;

class IGRepositoryAutoDetectTest {

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
        assertEquals(IGRepositoryConfig.WITH_CATEGORY_AND_TYPE_DIRECTORIES, IGRepositoryConfig.autoDetect(null));
        assertEquals(IGRepositoryConfig.WITH_CATEGORY_AND_TYPE_DIRECTORIES, IGRepositoryConfig.autoDetect(tempDir.resolve("does_not_exist")));
    }

    @Test
    void autoDetectStandard() {
        assertEquals(IGRepositoryConfig.WITH_CATEGORY_AND_TYPE_DIRECTORIES, IGRepositoryConfig.autoDetect(tempDir.resolve("directoryPerType/standard")));
    }

    @Test
    void autoDetectPrefix() {
        assertEquals(IGRepositoryConfig.WITH_CATEGORY_AND_TYPE_DIRECTORIES_AND_TYPE_NAMES, IGRepositoryConfig.autoDetect(tempDir.resolve("directoryPerType/prefixed")));
    }

    @Test
    void autoDetectFlat() {
        assertEquals(IGRepositoryConfig.FLAT, IGRepositoryConfig.autoDetect(tempDir.resolve("flat")));
    }

    @Test
    void autoDetectFlatWithTypeNames() {
        assertEquals(IGRepositoryConfig.FLAT_WITH_TYPE_NAMES, IGRepositoryConfig.autoDetect(tempDir.resolve("flatWithTypeNames")));
    }
}
