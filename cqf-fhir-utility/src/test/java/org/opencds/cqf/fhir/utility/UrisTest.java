package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UrisTest {
    @Test
    void TestFileUris() {
        assertTrue(Uris.isFileUri("file://path/to/resource"));
        assertTrue(Uris.isFileUri("path/to/resource"));
        assertTrue(Uris.isFileUri("/path/to/resource"));
    }

    @Test
    void TestNull() {
        assertFalse(Uris.isFileUri(null));
    }

    @Test
    void TestNetworkUris() {
        assertFalse(Uris.isFileUri("http://www.test.com"));
        assertFalse(Uris.isFileUri("https://www.test.com"));
        assertFalse(Uris.isFileUri("http://localhost"));
    }

    @Test
    void testEnsureHttpsConvertsHttpToHttps() throws Exception {
        String input = "http://example.com/path";
        String output = Uris.ensureHttps(input);
        assertTrue(output.startsWith("https://"));
    }

    @Test
    void testEnsureHttpsPreservesHttps() throws Exception {
        String input = "https://secure.com";
        String output = Uris.ensureHttps(input);
        assertEquals(input, output);
    }

    @Test
    void testEnsureHttpsMalformedUrl() {
        assertThrows(IllegalArgumentException.class, () -> Uris.ensureHttps("://bad-url"));
    }
}
