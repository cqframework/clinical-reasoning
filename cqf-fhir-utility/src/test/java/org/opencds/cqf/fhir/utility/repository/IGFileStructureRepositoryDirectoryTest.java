package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.search.Searches;

public class IGFileStructureRepositoryDirectoryTest {

    private static Repository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    public static void setup() throws URISyntaxException, IOException, InterruptedException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        ResourceDirectoryCopier.copyFromJar(IGFileStructureRepositoryDirectoryTest.class, "/sampleIgs/directoryPerType/standard", tempDir);
        repository = new IGFileStructureRepository(FhirContext.forR4Cached(), tempDir.toString());
    }

    @Test
    void readLibrary() {
        var id = Ids.newId(Library.class, "123");
        var lib = repository.read(Library.class, id);
        assertNotNull(lib);
        assertEquals(id.getIdPart(), lib.getIdElement().getIdPart());
    }

    @Test
    void readLibraryNotExists() {
        var id = Ids.newId(Library.class, "DoesNotExist");
        assertThrows(ResourceNotFoundException.class, () -> repository.read(Library.class, id));
    }

    @Test
    void searchLibrary() {
        var libs = repository.search(Bundle.class, Library.class, Searches.ALL);

        assertNotNull(libs);
        assertEquals(2, libs.getEntry().size());
    }

    @Test
    void searchLibraryWithFilter() {
        var libs = repository.search(
                Bundle.class, Library.class, Searches.byUrl("http://example.com/Library/Test"));

        assertNotNull(libs);
        assertEquals(1, libs.getEntry().size());
    }

    @Test
    void searchLibraryNotExists() {
        var libs = repository.search(Bundle.class, Library.class, Searches.byUrl("not-exists"));
        assertNotNull(libs);
        assertEquals(0, libs.getEntry().size());
    }

    @Test
    void readPatient() {
        var id = Ids.newId(Patient.class, "ABC");
        var cond = repository.read(Patient.class, id);

        assertNotNull(cond);
        assertEquals(id.getIdPart(), cond.getIdElement().getIdPart());
    }

    @Test
    void searchCondition() {
        var cons = repository.search(
                Bundle.class, Condition.class, Searches.byCodeAndSystem("12345", "example.com/codesystem"));
        assertNotNull(cons);
        assertEquals(2, cons.getEntry().size());
    }

    @Test
    void readValueSet() {
        var id = Ids.newId(ValueSet.class, "456");
        var vs = repository.read(ValueSet.class, id);

        assertNotNull(vs);
        assertEquals(vs.getIdPart(), vs.getIdElement().getIdPart());
    }

    @Test
    void searchValueSet() {
        var sets = repository.search(
                Bundle.class,
                ValueSet.class,
                Searches.byUrl("example.com/ValueSet/456"));
        assertNotNull(sets);
        assertEquals(1, sets.getEntry().size());
    }

    @Test
    void createAndDeleteLibrary() {
        var lib = new Library();
        lib.setId("new-library");
        var o = repository.create(lib);
        var created = repository.read(Library.class, o.getId());
        assertNotNull(created);
        assertTrue(Files.exists(tempDir.resolve("resources/library/new-library.json")));

        repository.delete(Library.class, created.getIdElement());
        assertFalse(Files.exists(tempDir.resolve("resources/library/new-library.json")));
    }

    @Test
    void createAndDeletePatient() {
        var p = new Patient();
        p.setId("new-patient");
        var o = repository.create(p);
        var created = repository.read(Patient.class, o.getId());
        assertNotNull(created);
        assertTrue(Files.exists(tempDir.resolve("tests/patient/new-patient.json")));

        repository.delete(Patient.class, created.getIdElement());
        assertFalse(Files.exists(tempDir.resolve("resources/library/new-patient.json")));
    }

    @Test
    void createAndDeleteValueSet() {
        var v = new ValueSet();
        v.setId("new-valueset");
        var o = repository.create(v);
        var created = repository.read(ValueSet.class, o.getId());
        assertNotNull(created);
        assertTrue(Files.exists(tempDir.resolve("vocabulary/valueset/new-valueset.json")));

        repository.delete(ValueSet.class, created.getIdElement());
        assertFalse(Files.exists(tempDir.resolve("vocabulary/valueset/new-valueset.json")));
    }

    @Test
    void updatePatient() {
        var id = Ids.newId(Patient.class, "ABC");
        var p = repository.read(Patient.class, id);
        assertFalse(p.hasActive());

        p.setActive(true);
        repository.update(p);

        var updated = repository.read(Patient.class, id);
        assertTrue(updated.hasActive());
        assertTrue(updated.getActive());
    }
}
