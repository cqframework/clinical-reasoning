package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.test.Resources;
import org.opencds.cqf.fhir.utility.search.Searches;

class IgRepositoryBadDataTest {

    private record InvalidContentTestDataParams(IIdType id, String errorMessage) {}

    private static IRepository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        Resources.copyFromJar("/sampleIgs/badData", tempDir);
        repository = new IgRepository(FhirContext.forR4Cached(), tempDir);
    }

    @ParameterizedTest
    @MethodSource("invalidContentTestData")
    void readInvalidContentThrowsException(InvalidContentTestDataParams params) {
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, params.id()));
        assertTrue(e.getMessage().contains(params.errorMessage()));
    }

    @Test
    void nonFhirFilesAreIgnored() {
        var id = new IdType("Patient/NotAFhirFile");
        assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
    }

    @Test
    void searchThrowsBecauseOfInvalidContent() {
        // If there's any invalid content in the directory, the search will fail
        assertThrows(
                ResourceNotFoundException.class, () -> repository.search(Bundle.class, Patient.class, Searches.ALL));
    }

    private static Stream<InvalidContentTestDataParams> invalidContentTestData() {
        return Stream.of(
                new InvalidContentTestDataParams(
                        new IdType("Patient/InvalidContent"), "Found empty or invalid content"),
                new InvalidContentTestDataParams(new IdType("Patient/MissingId"), "Found resource without an id"),
                new InvalidContentTestDataParams(new IdType("Patient/NoContent"), "Found empty or invalid content"),
                new InvalidContentTestDataParams(
                        new IdType("Patient/WrongId"), "Found resource with an id DoesntMatchFilename"),
                new InvalidContentTestDataParams(
                        new IdType("Patient/WrongResourceType"), "Found resource with type Encounter"),
                new InvalidContentTestDataParams(
                        new IdType("Patient/WrongVersion").withVersion("1"), "Found resource with version 2"));
    }
}
