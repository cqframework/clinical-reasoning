package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.search.Searches;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

public class IGFileStructureRepositoryDirectoryTest {

  private static Repository repository;

  @TempDir
  static Path tempDir;

  @BeforeAll
  public static void setup() throws URISyntaxException, IOException {
    // This copies the sample IG to a temporary directory so that
    // we can test against an actual filesystem
    ResourceDirectoryCopier.copyFromJar(IGFileStructureRepositoryDirectoryTest.class, "CMS111",
        tempDir);

    repository = new IGFileStructureRepository(FhirContext.forR4Cached(),
        tempDir.toString());
  }

  @Test
  public void readLibrary() {
    var lib = repository.read(Library.class, Ids.newId(Library.class, "CMS111"));
    assertNotNull(lib);
    assertEquals("CMS111", lib.getIdElement().getIdPart());
  }

  @Test
  public void readLibraryNotExists() {
    assertThrows(ResourceNotFoundException.class,
        () -> {
          repository.read(Library.class, Ids.newId(Library.class, "DoesNotExist"));
        });
  }


  @Test
  public void searchLibrary() {
    var libs = repository.search(Bundle.class, Library.class, Searches.ALL);

    assertNotNull(libs);
    assertEquals(4, libs.getEntry().size());

  }

  @Test
  public void searchLibraryWithFilter() {
    var libs = repository.search(Bundle.class, Library.class, Searches.byUrl("test.example"));

    assertNotNull(libs);
    assertEquals(1, libs.getEntry().size());

  }

  @Test
  public void searchLibraryNotExists() {
    var libs = repository.search(Bundle.class, Library.class, Searches.byUrl("not-exists"));
    assertNotNull(libs);
    assertEquals(0, libs.getEntry().size());
  }

  @Test
  public void readCondition() {

  }


  @Test
  public void searchCondition() {

  }

  @Test
  public void readValueSet() {

  }

  @Test
  public void searchValueSet() {

  }

}
