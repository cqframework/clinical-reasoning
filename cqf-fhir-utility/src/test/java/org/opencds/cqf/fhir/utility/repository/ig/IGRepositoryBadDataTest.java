package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.ResourceDirectoryCopier;

public class IGRepositoryBadDataTest {

    private static Repository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    public static void setup() throws URISyntaxException, IOException, InterruptedException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        ResourceDirectoryCopier.copyFromJar(IGRepositoryBadDataTest.class, "/sampleIgs/badData", tempDir);
        repository = new IGRepository(FhirContext.forR4Cached(), tempDir.toString());
    }

    @ParameterizedTest
    @MethodSource("invalidContentTestData")
    void readInvalidContentThrowsException(IIdType id, String errorMessage) {
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
        assertTrue(e.getMessage().contains(errorMessage));
    }

    private static Stream<Arguments> invalidContentTestData() {
        return Stream.of(
                Arguments.of(new IdType("Patient/InvalidContent"), "Found empty or invalid content"),
                Arguments.of(new IdType("Patient/MissingId"), "Found resource without an id"),
                Arguments.of(new IdType("Patient/NoContent"), "Found empty or invalid content"),
                Arguments.of(new IdType("Patient/WrongId"), "Found resource with an id DoesntMatchFilename"),
                Arguments.of(new IdType("Patient/WrongResourceType"), "Found resource with type Encounter"),
                Arguments.of(new IdType("Patient/WrongVersion").withVersion("1"), "Found resource with version 2"));
    }
}
