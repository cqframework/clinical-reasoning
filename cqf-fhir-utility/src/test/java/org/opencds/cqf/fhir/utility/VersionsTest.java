package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;

class VersionsTest {

    @Test
    void versionCompare() {
        int result = Versions.compareVersions("1.0.0", "0.9.9");
        assertEquals(1, result);

        result = Versions.compareVersions("0.9.9", "1.0.0");
        assertEquals(-1, result);

        result = Versions.compareVersions("1.0.0", "1.0.0");
        assertEquals(0, result);

        result = Versions.compareVersions(null, null);
        assertEquals(0, result);

        result = Versions.compareVersions(null, "1.0.0");
        assertEquals(1, result);

        result = Versions.compareVersions("1.0.0", null);
        assertEquals(-1, result);

        result = Versions.compareVersions("1.0", "0.0.9");
        assertEquals(1, result);

        result = Versions.compareVersions("0", "1.1.2");
        assertEquals(-1, result);

        result = Versions.compareVersions("1.1.1", "1.1.1-draft");
        assertEquals(-1, result);

        result = Versions.compareVersions("1.1.2", "1.1.1-draft");
        assertEquals(1, result);

        result = Versions.compareVersions("1.1.2invalid", "1.1.1-draft");
        assertEquals(1, result);

        result = Versions.compareVersions("1.1.2invalid", "1.1.1-draft-draft-draft");
        assertEquals(1, result);

        result = Versions.compareVersions("1.1.2-draft", "1.1.1-draft-draft-draft/+*'^%!@#$");
        assertEquals(1, result);

        result = Versions.compareVersions("1111", "2222");
        assertEquals(-1, result);

        result = Versions.compareVersions("abc1", "xyz9");
        assertEquals(-1, result);

        result = Versions.compareVersions("1.1.1-draft", "1.1.1-draft-draft2-draft3/+*'^%!@#$");
        assertEquals(-1, result);

        result = Versions.compareVersions("1.1bleh.1", "1.1.1");
        assertEquals(1, result);
    }

    @Test
    void selectFromList() {
        List<Library> libraries =
                Arrays.asList(new Library().setVersion("1.0.0"), new Library().setVersion("0.0.1"), new Library());

        Function<Library, String> getVersion = l -> l.getVersion();

        // Gets matching version
        Library lib = Versions.selectByVersion(libraries, "1.0.0", getVersion);
        assertEquals("1.0.0", lib.getVersion());

        // Gets max version (null version on library is considered max version)
        lib = Versions.selectByVersion(libraries, "2.0.0", getVersion);
        assertNull(lib.getVersion());

        // Null version max version (null input gets max version)
        lib = Versions.selectByVersion(libraries, null, getVersion);
        assertNull(lib.getVersion());
    }
}
