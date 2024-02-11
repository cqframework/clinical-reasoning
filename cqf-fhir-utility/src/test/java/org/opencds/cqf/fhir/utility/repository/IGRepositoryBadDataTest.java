package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;

public class IGRepositoryBadDataTest {

    private static Repository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    public static void setup() throws URISyntaxException, IOException, InterruptedException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        ResourceDirectoryCopier.copyFromJar(IGRepositoryBadDataTest.class, "/sampleIgs/badData", tempDir);
        repository = new IGFileStructureRepository(FhirContext.forR4Cached(), tempDir.toString());
    }

    @Test
    void readInvalidContentThrowsException() {
        var id = Ids.newId(Patient.class, "InvalidContent");
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
        assertTrue(e.getMessage().contains("Found empty or invalid content"));
    }

    @Test
    void readMissingIdThrowsException() {
        var id = Ids.newId(Patient.class, "MissingId");
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
        assertTrue(e.getMessage().contains("Found resource without an id"));
    }

    @Test
    void readNoContentThrowsException() {
        var id = Ids.newId(Patient.class, "NoContent");
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
        assertTrue(e.getMessage().contains("Found empty or invalid content"));
    }

    @Test
    void readWrongIdThrowsException() {
        var id = Ids.newId(Patient.class, "WrongId");
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
        assertTrue(e.getMessage().contains("Found resource with an id DoesntMatchFilename"));
    }

    @Test
    void readWrongResourceTypeThrowsException() {
        var id = Ids.newId(Patient.class, "WrongResourceType");
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
        assertTrue(e.getMessage().contains("Found resource with type Encounter"));
    }

    @Test
    void readWrongVersionThrowsException() {
        var id = Ids.newId(Patient.class, "WrongVersion").withVersion("1");
        var e = assertThrows(ResourceNotFoundException.class, () -> repository.read(Patient.class, id));
        assertTrue(e.getMessage().contains("Found resource with version 2"));
    }
}
