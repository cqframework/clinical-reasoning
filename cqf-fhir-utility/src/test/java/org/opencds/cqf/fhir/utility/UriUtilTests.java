package org.opencds.cqf.fhir.utility;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
