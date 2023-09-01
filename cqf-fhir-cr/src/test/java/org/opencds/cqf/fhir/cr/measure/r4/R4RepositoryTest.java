package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

// import ca.uhn.fhir.rest.api.MethodOutcome;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

import ca.uhn.fhir.context.FhirContext;


public class R4RepositoryTest {

  Repository repository;

  public R4RepositoryTest() {
    repository = TestRepositoryFactory.createRepository(FhirContext.forR4Cached(),
        this.getClass(), Measure.CLASS_PATH + "/res");
  }

  @Test
  public void testRead() {
    IBaseResource res = repository.read(Patient.class, new IdType("Patient/example"), null);
    assertEquals(res.getIdElement().getIdPart(), "example");
  }

  @Test
  public void testReadLibrary() throws IOException {
    Library res = repository.read(Library.class, new IdType("Library/dependency-example"), null);
    assertEquals(res.getIdElement().getIdPart(), "dependency-example");
    assertTrue(IOUtils.toString(new ByteArrayInputStream(res.getContent().get(0).getData()),
        StandardCharsets.UTF_8).startsWith("library DependencyExample version '0.1.0'"));
  }

  /*
   * todo :: work on ProxyRepository create()
   *
   * @Test public void testCreate() { Patient john = new Patient(); john.setId(new IdType("Patient",
   * "id-john-doe")); MethodOutcome methodOutcome = repository.create(john, null);
   * assertTrue(methodOutcome.getCreated());
   *
   * repository.create(john, null); }
   */

  @Test
  public void testSearch() {
    IBaseBundle bundle = repository.search(IBaseBundle.class, Library.class, null, null);
    assertEquals(((Bundle) bundle).getEntry().size(), 6);
  }
}
