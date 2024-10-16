package org.opencds.cqf.fhir.cql;

import java.io.File;
import java.nio.file.Path;

// This class is used to ensure unit tests work in both IDEs and Maven.
// Maven copies resources to the target directory, so this class will return
// the target directory if the resource is not found on the classpath.
class ResourceDirectoryResolver {

    public static Path getResourceDirectory() {
        // Check for the Maven `target/test-classes` directory first
        File mavenResourceDir = new File("target/test-classes");
        if (mavenResourceDir.exists()) {
            return mavenResourceDir.toPath();
        }

        // Fallback to IDE directory, assuming the resources are in `src/test/resources`
        File ideResourceDir = new File("src/test/resources");
        if (ideResourceDir.exists()) {
            return ideResourceDir.toPath();
        }

        throw new IllegalStateException("Resource directory not found.");
    }
}
