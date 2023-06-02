package org.opencds.cqf.cql.evaluator.fhir.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.testng.annotations.Test;

public class LibrariesTest {

  private final byte[] testData = "test-data".getBytes();

  @Test
  public void libraryNoContentReturnsNull() {
    Library library = new Library();

    var content = Libraries.getContent(library, "text/cql");

    assertTrue(content.isEmpty());
  }

  @Test
  public void libraryWithContentReturnsContent() {
    Library library = new Library();
    library.addContent().setContentType("text/cql").setData(testData);

    var content = Libraries.getContent(library, "text/cql");

    assertEquals(content.get(), testData);
  }

  @Test
  public void libraryMismatchedContentReturnsNull() {
    Library library = new Library();
    library.addContent().setContentType("text/cql").setData("test-data".getBytes());

    var content = Libraries.getContent(library, "text/elm");

    assertTrue(content.isEmpty());
  }

  @Test
  public void libraryDstu3WithContentReturnsContent() {
    org.hl7.fhir.dstu3.model.Library library = new org.hl7.fhir.dstu3.model.Library();
    library.addContent().setContentType("text/cql").setData(testData);

    var content = Libraries.getContent(library, "text/cql");

    assertEquals(content.get(), testData);
  }

  @Test
  public void notALibraryThrowsException() {
    Measure m = new Measure();
    assertThrows(IllegalArgumentException.class, () -> {
      Libraries.getContent(m, "text/cql");
    });
  }

  @Test
  public void libraryWithVersionReturnsVersion() {
    Library library = new Library().setVersion("1.0.0");

    String version = Libraries.getVersion(library);

    assertEquals("1.0.0", version);
  }

  @Test
  public void libraryNoVersionReturnsNull() {
    Library library = new Library();

    String version = Libraries.getVersion(library);

    assertNull(version);
  }
}
