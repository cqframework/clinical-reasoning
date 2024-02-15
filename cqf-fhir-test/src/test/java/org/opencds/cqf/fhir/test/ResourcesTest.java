package org.opencds.cqf.fhir.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void getResourcePath() {
        // The behavior of this function intentionally changes based
        // on whether it is called from from a jar or a unit test.
        // This is so that the unit tests can be run from the IDE
        // and on the CI server without modification. This makes it
        // difficult to test the behavior of the function in isolation.
        assertNotNull(Resources.getResourcePath(getClass()));
        assertNotNull(Resources.getResourcePath(Dummy.class));
    }

    @Test
    void copyFromCurrentClass() throws URISyntaxException, IOException, ClassNotFoundException {
        Resources.copyFromJar("", tempDir);
        assertTrue(tempDir.resolve("test-copy.txt").toFile().exists());

        // Ensure recursive copy works
        assertTrue(tempDir.resolve("child/test-child.txt").toFile().exists());
    }

    @Test
    void copyFromCurrentClassRoot() throws URISyntaxException, IOException, ClassNotFoundException {
        Resources.copyFromJar("/", tempDir);
        assertTrue(tempDir.resolve("test-root.txt").toFile().exists());
    }

    @Test
    void copyFromCurrentClassChildPackage() throws URISyntaxException, IOException, ClassNotFoundException {
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
