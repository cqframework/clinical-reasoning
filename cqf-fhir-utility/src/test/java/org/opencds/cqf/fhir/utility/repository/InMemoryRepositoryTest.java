package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;

public class InMemoryRepositoryTest {

    Repository repository;

    public InMemoryRepositoryTest() {
        repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repository.update(new Library().setId("Library/example1"));
        repository.update(new Library().setId("Library/example2"));
        repository.update(new Encounter().setId("Encounter/example1"));
    }

    @Test
    public void testRead() {
        IBaseResource res = repository.read(Library.class, new IdType("Library/example1"), null);
        assertEquals("example1", res.getIdElement().getIdPart());
    }

    @Test
    public void testSearchWithId() {
        Map<String, List<IQueryParameterType>> search = new HashMap<>();
        search.put("_id", Collections.singletonList(new TokenParam("example1")));
        var resources = repository.search(Bundle.class, Library.class, search);
        // The _id parameter will be consumed if the index is being used.
        assertTrue(search.isEmpty());
        assertEquals(1, resources.getEntry().size());

        search.put("_id", Collections.singletonList(new TokenParam("example1")));
        resources = repository.search(Bundle.class, Encounter.class, search);
        assertTrue(search.isEmpty());
        assertEquals(1, resources.getEntry().size());

        search.put("_id", Collections.singletonList(new TokenParam("example2345")));
        resources = repository.search(Bundle.class, Encounter.class, search);
        assertTrue(search.isEmpty());
        assertEquals(0, resources.getEntry().size());
    }
}
