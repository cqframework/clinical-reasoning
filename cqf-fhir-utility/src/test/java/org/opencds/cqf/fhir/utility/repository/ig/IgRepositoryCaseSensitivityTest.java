package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.test.Resources;
import org.opencds.cqf.fhir.utility.search.Searches;

public class IgRepositoryCaseSensitivityTest {

    private static Repository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    public static void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        Resources.copyFromJar("/sampleIgs/caseSensitivity", tempDir);
        repository = new IgRepository(FhirContext.forR4Cached(), tempDir.toString());
    }

    @Test
    void readPatient() {
        var p = repository.read(Patient.class, new IdType("1"));
        assertNotNull(p);
        assertEquals("1", p.getIdElement().getIdPart());
    }

    @Test
    void searchPatient() {
        var p = repository.search(Bundle.class, Patient.class, Searches.ALL);
        assertNotNull(p);
        assertEquals(1, p.getEntry().size());
    }

    @Test
    void readEncounter() {
        var e = repository.read(Encounter.class, new IdType("1"));
        assertNotNull(e);
        assertEquals("1", e.getIdElement().getIdPart());
    }

    @Test
    void searchEncounter() {
        var e = repository.search(Bundle.class, Encounter.class, Searches.ALL);
        assertNotNull(e);
        assertEquals(1, e.getEntry().size());
    }

    @Test
    void readValueSet() {
        var vs = repository.read(ValueSet.class, new IdType("1"));
        assertNotNull(vs);
        assertEquals("1", vs.getIdElement().getIdPart());
    }

    @Test
    void searchValueSet() {
        var vs = repository.search(Bundle.class, ValueSet.class, Searches.ALL);
        assertNotNull(vs);
        assertEquals(1, vs.getEntry().size());
    }

    @Test
    void readLibrary() {
        var l = repository.read(Library.class, new IdType("1"));
        assertNotNull(l);
        assertEquals("1", l.getIdElement().getIdPart());
    }

    @Test
    void searchLibrary() {
        var l = repository.search(Bundle.class, Library.class, Searches.ALL);
        assertNotNull(l);
        assertEquals(1, l.getEntry().size());
    }
}
