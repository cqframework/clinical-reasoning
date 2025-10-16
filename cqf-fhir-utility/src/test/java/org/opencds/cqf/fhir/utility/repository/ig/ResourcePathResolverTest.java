package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePathResolverTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();

    @Test
    void preferredDirectoryUsesCompartmentAssignmentForDataResources(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);
        var assignment = CompartmentAssignment.of("patient", "123");

        var path = resolver.preferredDirectory(Observation.class, assignment);

        assertEquals(tempDir.resolve("tests/data/fhir/patient/123/observation"), path);
    }

    @Test
    void preferredDirectoryFallsBackToSharedWhenSpecified(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);
        var assignment = CompartmentAssignment.shared();

        var path = resolver.preferredDirectory(Observation.class, assignment);

        assertEquals(tempDir.resolve("tests/data/fhir/shared/observation"), path);
    }

    @Test
    void preferredDirectoryForContentResourceInStandardLayout(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.STANDARD, FHIR_CONTEXT);

        var path = resolver.preferredDirectory(Library.class, CompartmentAssignment.none());

        assertEquals(tempDir.resolve("input/resources/library"), path);
    }

    @Test
    void candidateDirectoriesIncludeCompartmentSharedAndSrcPaths(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("tests/data/fhir/patient/123/observation"));
        Files.createDirectories(tempDir.resolve("tests/data/fhir/shared/observation"));
        Files.createDirectories(tempDir.resolve("src/fhir/observation"));

        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);

        var paths = resolver.directories(Observation.class, CompartmentAssignment.unknown("Patient"));

        assertTrue(paths.contains(tempDir.resolve("tests/data/fhir/patient/123/observation")));
        assertTrue(paths.contains(tempDir.resolve("tests/data/fhir/shared/observation")));
        assertTrue(paths.contains(tempDir.resolve("src/fhir/observation")));
        assertFalse(paths.contains(tempDir.resolve("tests/data/fhir/observation")));
    }

    @Test
    void searchDirectoriesForTerminologyInKalmUseSharedDirectories(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);

        var directories = resolver.directories(ValueSet.class, CompartmentAssignment.shared());

        assertTrue(directories.contains(tempDir.resolve("src/fhir/valueset")));
        assertTrue(directories.contains(tempDir.resolve("tests/data/fhir/shared/valueset")));
        assertFalse(directories.contains(tempDir.resolve("tests/data/fhir/valueset")));
    }

    @Test
    void directoriesForTerminologyInKalmPreferSrcThenShared(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);

        var directories = resolver.directories(ValueSet.class, CompartmentAssignment.none());

        assertEquals(tempDir.resolve("src/fhir/valueset"), directories.get(0));
        assertTrue(directories.contains(tempDir.resolve("tests/data/fhir/shared/valueset")));
        assertFalse(directories.contains(tempDir.resolve("tests/data/fhir/valueset")));
    }
}
