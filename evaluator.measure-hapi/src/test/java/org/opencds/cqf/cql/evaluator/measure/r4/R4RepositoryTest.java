package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import ca.uhn.fhir.rest.api.MethodOutcome;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class R4RepositoryTest {

    FhirRepository repository;

    public R4RepositoryTest() {
        repository = new FhirRepository(this.getClass(),
                List.of("res/tests", "res/vocabulary/CodeSystem/", "res/vocabulary/ValueSet/", "res/content/"), false);
    }

    @Test
    public void testRead() {
        IBaseResource res = repository.read(Patient.class, new IdType("example"), null);
        assertEquals(res.getIdElement().getIdPart(), "example");
    }

  @Test
  public void testReadLibrary() throws IOException {
    Library res = repository.read(Library.class, new IdType("dependency-example"), null);
    assertEquals(res.getIdElement().getIdPart(), "dependency-example");
    assertTrue(IOUtils.toString(new ByteArrayInputStream(res.getContent().get(0).getData()), StandardCharsets.UTF_8)
        .startsWith("library DependencyExample version '0.1.0'"));
  }


    @Test
    public void testCreate() {
        Patient john = new Patient();
        john.setId(new IdType("Patient", "id-john-doe"));
        MethodOutcome methodOutcome = repository.create(john, null);
        assertTrue(methodOutcome.getCreated());

        repository.create(john, null);
    }

    @Test
    public void testSearch() {
        IBaseBundle bundle = repository.search(IBaseBundle.class, Patient.class, null, null);
        assertEquals(((Bundle) bundle).getEntry().size(), 3);
    }
}
