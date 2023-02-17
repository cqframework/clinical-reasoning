package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import ca.uhn.fhir.rest.api.MethodOutcome;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.testng.annotations.Test;

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
