package org.opencds.cqf.cql.evaluator.builder.util;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class UriUtilTests {
    @Test
    public void TestFileUris() {
        assertTrue(UriUtil.isFileUri("file://path/to/resource"));
        assertTrue(UriUtil.isFileUri("path/to/resource"));
        assertTrue(UriUtil.isFileUri("/path/to/resource"));
    }

    @Test
    public void TestNull() {
        assertFalse(UriUtil.isFileUri(null));
    }

    @Test
    public void TestNetworkUris() {
        assertFalse(UriUtil.isFileUri("http://www.test.com"));
        assertFalse(UriUtil.isFileUri("https://www.test.com"));
        assertFalse(UriUtil.isFileUri("http://localhost"));
    }
}