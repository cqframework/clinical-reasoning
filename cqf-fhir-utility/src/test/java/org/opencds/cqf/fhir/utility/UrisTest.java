package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
