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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

public class InMemoryRepositoryTest {

    Repository repository;

    public InMemoryRepositoryTest() {
        IBaseBundle bundle = (IBaseBundle) FhirContext.forR4Cached()
                .newJsonParser()
                .parseResource(InMemoryRepositoryTest.class.getResourceAsStream("bundle/test-bundle.json"));
        repository = new InMemoryFhirRepository(FhirContext.forR4Cached(), bundle);
    }

    @Test
    public void testRead() {
        IBaseResource res = repository.read(Library.class, new IdType("Library/LibraryEvaluationTest"), null);
        assertEquals(res.getIdElement().getIdPart(), "LibraryEvaluationTest");
    }

    @Test
    public void testSearchWithId() {
        Map<String, List<IQueryParameterType>> map = new HashMap<>();
        map.put("id", Collections.singletonList(new ReferenceParam("LibraryEvaluationTest")));
        IBaseBundle bundle = repository.search(IBaseBundle.class, Library.class, map, null);
        assertEquals(((Bundle) bundle).getEntry().size(), 1);
    }
}
