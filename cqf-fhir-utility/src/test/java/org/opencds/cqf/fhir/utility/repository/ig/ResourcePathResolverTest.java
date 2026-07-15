package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePathResolverTest {

    @Test
    void preferredDirectoryUsesCompartmentAssignmentForDataResources(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM);
        var assignment = CompartmentAssignment.of("patient", "123");

        var path = resolver.preferredDirectory(Observation.class, assignment);

        assertEquals(tempDir.resolve("tests/data/fhir/patient/123/observation"), path);
    }

    @Test
    void preferredDirectoryFallsBackToSharedWhenSpecified(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM);
        var assignment = CompartmentAssignment.shared();

        var path = resolver.preferredDirectory(Observation.class, assignment);

        assertEquals(tempDir.resolve("tests/data/fhir/shared/observation"), path);
    }

    @Test
    void preferredDirectoryForContentResourceInStandardLayout(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.STANDARD);

        var path = resolver.preferredDirectory(Library.class, CompartmentAssignment.none());

        assertEquals(tempDir.resolve("input/resources/library"), path);
    }

    @Test
    void candidateDirectoriesIncludeCompartmentSharedAndSrcPaths(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("tests/data/fhir/patient/123/observation"));
        Files.createDirectories(tempDir.resolve("tests/data/fhir/shared/observation"));
        Files.createDirectories(tempDir.resolve("src/fhir/observation"));

        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM);

        var paths = resolver.directories(Observation.class, CompartmentAssignment.unknown("Patient"));

        assertTrue(paths.contains(tempDir.resolve("tests/data/fhir/patient/123/observation")));
        assertTrue(paths.contains(tempDir.resolve("tests/data/fhir/shared/observation")));
        assertTrue(paths.contains(tempDir.resolve("src/fhir/observation")));
        assertFalse(paths.contains(tempDir.resolve("tests/data/fhir/observation")));
    }

    @Test
    void searchDirectoriesForTerminologyInKalmUseSharedDirectories(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM);

        var directories = resolver.directories(ValueSet.class, CompartmentAssignment.shared());

        assertTrue(directories.contains(tempDir.resolve("src/fhir/valueset")));
        assertTrue(directories.contains(tempDir.resolve("tests/data/fhir/shared/valueset")));
        assertFalse(directories.contains(tempDir.resolve("tests/data/fhir/valueset")));
    }

    @Test
    void directoriesForTerminologyInKalmPreferSrcThenShared(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM);

        var directories = resolver.directories(ValueSet.class, CompartmentAssignment.shared());

        assertEquals(tempDir.resolve("src/fhir/valueset"), directories.get(0));
        assertTrue(directories.contains(tempDir.resolve("tests/data/fhir/shared/valueset")));
        assertFalse(directories.contains(tempDir.resolve("tests/data/fhir/valueset")));
    }

    @Test
    void directoriesForTerminologyInKalmIncludeExternalSource(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM);
        var external = tempDir.resolve("src/fhir/external");

        assertEquals(
                List.of(
                        tempDir.resolve("src/fhir/valueset"),
                        external,
                        tempDir.resolve("tests/data/fhir/shared/valueset")),
                resolver.directories(ValueSet.class, CompartmentAssignment.shared()));
        assertEquals(
                List.of(
                        tempDir.resolve("src/fhir/codesystem"),
                        tempDir.resolve("src/fhir/external"),
                        tempDir.resolve("tests/data/fhir/shared/codesystem")),
                resolver.directories(CodeSystem.class, CompartmentAssignment.shared()));
        assertTrue(resolver.isExternalPath(external));
        assertTrue(resolver.isExternalPath(external.resolve("example.json")));
    }

    @Test
    void terminologyDirectoriesIncludeExternalFallbacks(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("input/vocabulary/valueset"));
        Files.createDirectories(tempDir.resolve("input/vocabulary/valueset/external"));

        var resolver = new ResourcePathResolver(tempDir, IgConventions.STANDARD);

        var directories = resolver.directories(ValueSet.class, CompartmentAssignment.none());

        assertEquals(tempDir.resolve("input/vocabulary/valueset"), directories.get(0));
        assertTrue(directories.contains(tempDir.resolve("input/vocabulary/valueset/external")));
    }
}
