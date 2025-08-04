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
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior.PreserveEncoding;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FhirTypeLayout;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.FilenameMode;

class IgRepositoryOverwriteEncodingTest {

    private static IRepository repository;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        Resources.copyFromJar("/sampleIgs/mixedEncoding", tempDir);
        var conventions = new IgConventions(
                FhirTypeLayout.DIRECTORY_PER_TYPE,
                CategoryLayout.DIRECTORY_PER_CATEGORY,
                CompartmentLayout.FLAT,
                FilenameMode.ID_ONLY,
                new EncodingBehavior(EncodingEnum.XML, PreserveEncoding.OVERWRITE_WITH_PREFERRED_ENCODING));
        repository = new IgRepository(FhirContext.forR4Cached(), tempDir, conventions, null);
    }

    @Test
    void readLibrary() {
        var id = Ids.newId(Library.class, "123");
        var lib = repository.read(Library.class, id);
        assertNotNull(lib);
        assertEquals(id.getIdPart(), lib.getIdElement().getIdPart());
    }

    @Test
    void updateLibrary() {
        var id = Ids.newId(Library.class, "123");
        var lib = repository.read(Library.class, id);
        assertNotNull(lib);
        assertEquals(id.getIdPart(), lib.getIdElement().getIdPart());

        lib.addAuthor().setName("Test Author");

        repository.update(lib);
        assertFalse(tempDir.resolve("input/resources/library/123.json").toFile().exists());
        assertTrue(tempDir.resolve("input/resources/library/123.xml").toFile().exists());
    }
}
