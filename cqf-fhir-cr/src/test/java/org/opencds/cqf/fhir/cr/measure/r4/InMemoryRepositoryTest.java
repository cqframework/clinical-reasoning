package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

public class InMemoryRepositoryTest {

    Repository repository;

    public InMemoryRepositoryTest() {
        repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
        Library library1 = new Library();
        library1.setId(new IdType("Library", "example1"));
        repository.update(library1);
        Library library2 = new Library();
        library2.setId(new IdType("Library/example2"));
        repository.update(library2);
        Encounter encounter1 = new Encounter();
        encounter1.setId(new IdType("Encounter", "example1"));
        repository.update(encounter1);
    }

    @Test
    public void testRead() {
        IBaseResource res = repository.read(Library.class, new IdType("Library/example1"), null);
        assertEquals(res.getIdElement().getIdPart(), "example1");
    }

    @Test
    public void testSearchWithId() {
        Map<String, List<IQueryParameterType>> mapLibrary = new HashMap<>();
        mapLibrary.put("_id", Collections.singletonList(new ReferenceParam("Library/example1")));
        IBaseBundle bundle = repository.search(IBaseBundle.class, Library.class, mapLibrary, null);
        assertEquals(((Bundle) bundle).getEntry().size(), 1);

        Map<String, List<IQueryParameterType>> mapEncounter = new HashMap<>();
        mapEncounter.put("_id", Collections.singletonList(new ReferenceParam("Encounter/example1")));
        IBaseBundle bundleEncounter = repository.search(IBaseBundle.class, Encounter.class, mapEncounter, null);
        assertEquals(((Bundle) bundleEncounter).getEntry().size(), 1);
    }
}
