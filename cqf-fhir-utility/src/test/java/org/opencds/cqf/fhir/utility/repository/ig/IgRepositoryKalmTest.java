package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.search.Searches;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IgRepositoryKalmTest {

    private static IRepository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        Resources.copyFromJar("/sampleIgs/kalm", tempDir);
        repository = new IgRepository(FhirContext.forR4Cached(), tempDir);
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
    void searchLibraryNotExists() {
        var libs = repository.search(Bundle.class, Library.class, Searches.byUrl("not-exists"));
        assertNotNull(libs);
        assertEquals(0, libs.getEntry().size());
    }

    @Test
    void readPatientNoCompartment() {
        var id = Ids.newId(Patient.class, "123");
        var patient = repository.read(Patient.class, id);

        assertNotNull(patient);
        var sourcePath = (Path) patient.getUserData(IgRepository.SOURCE_PATH_TAG);
        assertNotNull(sourcePath);
        assertTrue(sourcePath.toString().contains(Path.of("patient", "123").toString()));
    }

    @Test
    void readPatient() {
        var id = Ids.newId(Patient.class, "123");
        var p = repository.read(Patient.class, id);

        assertNotNull(p);
        assertEquals(id.getIdPart(), p.getIdElement().getIdPart());
    }

    @Test
    void searchPatientById() {
        var id = Ids.newId(Patient.class, "123");
        var b = repository.search(Bundle.class, Patient.class, Searches.byId("123"));

        assertNotNull(b);
        assertEquals(1, b.getEntry().size());
        var p = (Patient) b.getEntryFirstRep().getResource();
        assertNotNull(p);
        assertEquals(id.getIdPart(), p.getIdElement().getIdPart());
    }

    @Test
    void searchEncounterNoCompartment() {
        var encounters = repository.search(Bundle.class, Encounter.class, Searches.ALL);
        assertNotNull(encounters);
        assertEquals(2, encounters.getEntry().size());
    }

    @Test
    void searchEncounter() {
        var bySubject = Searches.toFlattenedMap(
                Searches.builder().withReferenceParam("subject", "Patient/123").build());
        var encounters = repository.search(Bundle.class, Encounter.class, bySubject);
        assertNotNull(encounters);
        assertEquals(1, encounters.getEntry().size());
    }

    // Terminology resources are not in compartments
    @Test
    void readValueSet() {
        var id = Ids.newId(ValueSet.class, "456");
        var vs = repository.read(ValueSet.class, id);

        assertNotNull(vs);
        assertEquals(vs.getIdPart(), vs.getIdElement().getIdPart());
    }

    @Test
    void searchValueSet() {
        var sets = repository.search(Bundle.class, ValueSet.class, Searches.byUrl("example.com/ValueSet/456"));
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

        var loc = tempDir.resolve("src/fhir/library/new-library.json");
        assertTrue(Files.exists(loc));

        repository.delete(Library.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    // Organization does not exist within a Patient compartment
    // So should be in a shared folder.
    @Test
    void createAndDeleteOrganizationWithCompartment() {
        var org = new Organization();
        org.setId("new-organization");
        var o = repository.create(org);
        var created = repository.read(Organization.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("tests/data/fhir/shared/organization/new-organization.json");
        assertTrue(Files.exists(loc));

        repository.delete(Organization.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void createValueSetPrefersSrcTerminologyDirectory() {
        var valueSet = new ValueSet();
        valueSet.setId("new-terminology");

        var outcome = repository.create(valueSet);
        var created = repository.read(ValueSet.class, outcome.getId());
        assertNotNull(created);

        var srcPath = tempDir.resolve("src/fhir/valueset/new-terminology.json");
        var deprecatedPath = tempDir.resolve("tests/data/fhir/valueset/new-terminology.json");

        assertTrue(Files.exists(srcPath));
        assertFalse(Files.exists(deprecatedPath));

        repository.delete(ValueSet.class, created.getIdElement());
        assertFalse(Files.exists(srcPath));
    }

    @Test
    void createAndDeletePatient() {
        var p = new Patient();
        p.setId("new-patient");
        var o = repository.create(p);
        var created = repository.read(Patient.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("tests/data/fhir/patient/new-patient/patient/new-patient.json");
        assertTrue(Files.exists(loc));

        repository.delete(Patient.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void createAndDeleteValueSet() {
        var v = new ValueSet();
        v.setId("new-valueset");
        var o = repository.create(v);
        var created = repository.read(ValueSet.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("src/fhir/valueset/new-valueset.json");
        assertTrue(Files.exists(loc));

        repository.delete(ValueSet.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void updateMovesDataResourceFromSharedToCompartment() {
        var encounter = new Encounter();
        encounter.setId("enc-shared");
        repository.create(encounter);
        var sharedPath = tempDir.resolve("tests/data/fhir/shared/encounter/enc-shared.json");
        assertTrue(Files.exists(sharedPath));

        var stored = repository.read(Encounter.class, encounter.getIdElement());
        stored.setSubject(new Reference("Patient/123"));

        repository.update(stored);

        var compartmentPath = tempDir.resolve("tests/data/fhir/patient/123/encounter/enc-shared.json");
        assertTrue(Files.exists(compartmentPath));
        assertFalse(Files.exists(sharedPath));

        repository.delete(Encounter.class, stored.getIdElement());
    }

    @Test
    void updatePatient() {
        var id = Ids.newId(Patient.class, "123");
        var p = repository.read(Patient.class, id);
        assertFalse(p.hasActive());

        p.setActive(true);
        repository.update(p);

        var updated = repository.read(Patient.class, id);
        assertTrue(updated.hasActive());
        assertTrue(updated.getActive());
    }

    @Test
    void deleteNonExistentPatient() {
        var id = Ids.newId(Patient.class, "DoesNotExist");
        assertThrows(ResourceNotFoundException.class, () -> repository.delete(Patient.class, id));
    }

    // "Shared" (non-compartment) test data is in a "shared" folder
    @Test
    void readMedicationWithCompartment() {
        var id = Ids.newId(Medication.class, "456");
        var m = repository.read(Medication.class, id);

        assertNotNull(m);
        assertEquals(m.getIdPart(), m.getIdElement().getIdPart());
    }

    @Test
    void readMedicationWithoutCompartment() {
        var id = Ids.newId(Medication.class, "456");
        var m = repository.read(Medication.class, id, Map.of());

        assertNotNull(m);
        assertEquals(m.getIdPart(), m.getIdElement().getIdPart());
    }

    @Test
    void searchMedicationWithCompartment() {
        var sets = repository.search(Bundle.class, Medication.class, Map.of());
        assertNotNull(sets);
        assertEquals(1, sets.getEntry().size());
    }

    @Test
    void searchNonExistentType() {
        var unknownSubject = Searches.toFlattenedMap(Searches.builder()
                .withReferenceParam("subject", "Patient/DoesNotExist")
                .build());
        var results = repository.search(Bundle.class, Encounter.class, unknownSubject);
        assertNotNull(results);
        assertEquals(0, results.getEntry().size());
    }

    @Test
    void searchById() {
        var bundle = repository.search(Bundle.class, Library.class, Searches.byId("123"));
        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
    }

    @Test
    void searchByIdNotFound() {
        var bundle = repository.search(Bundle.class, Library.class, Searches.byId("DoesNotExist"));
        assertNotNull(bundle);
        assertEquals(0, bundle.getEntry().size());
    }

    @Test
    @Order(1) // Do this test first because it puts the filesystem (temporarily) in an invalid state
    void resourceMissingWhenCacheCleared() throws IOException {
        var id = new IdType("Library", "ToDelete");
        var lib = new Library().setIdElement(id);
        var path = tempDir.resolve("src/fhir/library/ToDelete.json");

        repository.create(lib);
        assertTrue(path.toFile().exists());

        // Read back, should exist
        lib = repository.read(Library.class, id);
        assertNotNull(lib);

        // Overwrite the file on disk.
        Files.writeString(path, "");

        // Read from cache, repo doesn't know the content is gone.
        lib = repository.read(Library.class, id);
        assertNotNull(lib);
        assertEquals("ToDelete", lib.getIdElement().getIdPart());

        ((IgRepository) repository).clearCache();

        // Try to read again, should be gone because it's not in the cache and the content is gone.
        assertThrows(ResourceNotFoundException.class, () -> repository.read(Library.class, id));

        // Clean up so that we don't affect other tests
        path.toFile().delete();
    }
}
