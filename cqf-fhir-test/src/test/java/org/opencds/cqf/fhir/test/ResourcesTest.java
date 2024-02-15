package org.opencds.cqf.fhir.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.nested.Dummy;

class ResourcesTest {

    @TempDir
    static Path tempDir;

    @Test
    void copyFromCurrentClass() throws URISyntaxException, IOException {
        Resources.copyFromJar("", tempDir);
        assertTrue(tempDir.resolve("test-copy.txt").toFile().exists());

        // Ensure recursive copy works
        assertTrue(tempDir.resolve("child/test-child.txt").toFile().exists());
    }

    @Test
    void copyFromCurrentClassRoot() throws URISyntaxException, IOException {
        Resources.copyFromJar("/", tempDir);
        assertTrue(tempDir.resolve("test-root.txt").toFile().exists());
    }

    @Test
    void copyFromCurrentClassChildPackage() throws URISyntaxException, IOException {
        Resources.copyFromJar("child", tempDir);
        assertTrue(tempDir.resolve("test-child.txt").toFile().exists());
    }

    @Test
    void copyFromSpecifiedClass() throws URISyntaxException, IOException {
        Resources.copyFromJar(Dummy.class, "", tempDir);
        assertTrue(tempDir.resolve("test-nested.txt").toFile().exists());
    }

    @Test
    void copyFromSpecifiedClassChildPackage() throws URISyntaxException, IOException {
        Resources.copyFromJar(Dummy.class, "child", tempDir);
        assertTrue(tempDir.resolve("test-nested-child.txt").toFile().exists());
    }
}
