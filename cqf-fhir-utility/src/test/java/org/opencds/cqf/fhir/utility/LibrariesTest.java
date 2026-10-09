package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;

class LibrariesTest {

    private final byte[] testData = "test-data".getBytes();

    @Test
    void libraryNoContentReturnsNull() {
        Library library = new Library();

        var content = Libraries.getContent(library, "text/cql");

        assertNull(content);
    }

    @Test
    void libraryWithContentReturnsContent() {
        Library library = new Library();
        library.addContent().setContentType("text/cql").setData(testData);

        var content = Libraries.getContent(library, "text/cql");

        assertEquals(testData, content);
    }

    @Test
    void libraryMismatchedContentReturnsNull() {
        Library library = new Library();
        library.addContent().setContentType("text/cql").setData("test-data".getBytes());

        var content = Libraries.getContent(library, "text/elm");

        assertNull(content);
    }

    @Test
    void libraryDstu3WithContentReturnsContent() {
        org.hl7.fhir.dstu3.model.Library library = new org.hl7.fhir.dstu3.model.Library();
        library.addContent().setContentType("text/cql").setData(testData);

        var content = Libraries.getContent(library, "text/cql");

        assertEquals(testData, content);
    }

    @Test
    void notALibraryThrowsException() {
        Measure m = new Measure();
        assertThrows(IllegalArgumentException.class, () -> {
            Libraries.getContent(m, "text/cql");
        });
    }

    @Test
    void libraryWithVersionReturnsVersion() {
        Library library = new Library().setVersion("1.0.0");

        String version = Libraries.getVersion(library);

        assertEquals("1.0.0", version);
    }

    @Test
    void libraryNoVersionReturnsNull() {
        Library library = new Library();

        String version = Libraries.getVersion(library);

        assertNull(version);
    }

    @Test
    void libraryWithNameReturnsName() {
        Library library = new Library().setName("test");

        String name = Libraries.getName(library);

        assertEquals("test", name);
    }

    @Test
    void libraryNoNameReturnsNull() {
        Library library = new Library();

        String name = Libraries.getName(library);

        assertNull(name);
    }
}
