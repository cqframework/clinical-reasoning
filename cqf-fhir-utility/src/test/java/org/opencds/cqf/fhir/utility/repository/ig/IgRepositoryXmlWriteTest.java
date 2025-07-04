package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.EncodingEnum;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior.PreserveEncoding;
import org.opencds.cqf.fhir.utility.search.Searches;

class IgRepositoryXmlWriteTest {

    private static IRepository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        Resources.copyFromJar("/sampleIgs/mixedEncoding", tempDir);
        var conventions = IgConventions.autoDetect(tempDir);
        repository = new IgRepository(
                FhirContext.forR4Cached(),
                tempDir,
                conventions,
                new EncodingBehavior(EncodingEnum.XML, PreserveEncoding.PRESERVE_ORIGINAL_ENCODING),
                null);
    }

    @Test
    void createAndDeleteLibrary() {
        var lib = new Library();
        lib.setId("new-library");
        var o = repository.create(lib);
        var created = repository.read(Library.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("resources/library/new-library.xml");
        assertTrue(Files.exists(loc));

        repository.delete(Library.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void createAndDeletePatient() {
        var p = new Patient();
        p.setId("new-patient");
        var o = repository.create(p);
        var created = repository.read(Patient.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("tests/patient/new-patient.xml");
        assertTrue(Files.exists(loc));

        repository.delete(Patient.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void readExternalValueSet() {
        var id = Ids.newId(ValueSet.class, "789");
        var vs = repository.read(ValueSet.class, id);
        assertNotNull(vs);
        assertEquals(vs.getIdPart(), vs.getIdElement().getIdPart());

        // Should be tagged with its source path
        var path = (Path) vs.getUserData(IgRepository.SOURCE_PATH_TAG);
        assertNotNull(path);
        assertTrue(path.toFile().exists());
        assertTrue(path.toString().contains("external"));
    }

    @Test
    void searchExternalValueSet() {
        var sets = repository.search(Bundle.class, ValueSet.class, Searches.byUrl("example.com/ValueSet/789"));
        assertNotNull(sets);
        assertEquals(1, sets.getEntry().size());
    }
}
