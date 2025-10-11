package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
        var assignment = Optional.of(CompartmentAssignment.of("patient", "123"));

        var path = resolver.preferredDirectory(Observation.class, assignment);

        assertEquals(tempDir.resolve("tests/data/fhir/patient/123/observation"), path);
    }

    @Test
    void preferredDirectoryFallsBackToSharedWhenSpecified(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);
        var assignment = Optional.of(CompartmentAssignment.shared());

        var path = resolver.preferredDirectory(Observation.class, assignment);

        assertEquals(tempDir.resolve("tests/data/fhir/shared/observation"), path);
    }

    @Test
    void preferredDirectoryForContentResourceInStandardLayout(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.STANDARD, FHIR_CONTEXT);

        var path = resolver.preferredDirectory(Library.class, Optional.empty());

        assertEquals(tempDir.resolve("input/resources/library"), path);
    }

    @Test
    void candidateDirectoriesIncludeCompartmentSharedAndBasePaths(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);

        List<Path> paths = resolver.candidateDirectoriesForId(Observation.class, "123");

        assertTrue(paths.contains(tempDir.resolve("tests/data/fhir/patient/123/observation")));
        assertTrue(paths.contains(tempDir.resolve("tests/data/fhir/shared/observation")));
        assertTrue(paths.contains(tempDir.resolve("tests/data/fhir/observation")));
    }

    @Test
    void searchDirectoriesExpandTypeDirectories(@TempDir Path tempDir) {
        var resolver = new ResourcePathResolver(tempDir, IgConventions.KALM, FHIR_CONTEXT);

        List<Path> directories = resolver.searchDirectories(ValueSet.class);
;
        assertTrue(directories.contains(tempDir.resolve("src/fhir/valueset")));
        assertTrue(directories.contains(tempDir.resolve("tests/data/fhir/valueset")));
    }
}
