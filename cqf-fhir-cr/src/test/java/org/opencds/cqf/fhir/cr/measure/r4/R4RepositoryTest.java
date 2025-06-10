package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.google.common.collect.Multimap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class R4RepositoryTest {

    IRepository repository;
    Path path = Path.of(getResourcePath(R4RepositoryTest.class));

    public R4RepositoryTest() {
        repository =
                new IgRepository(FhirContext.forR4Cached(), path.resolve("org/opencds/cqf/fhir/cr/measure/r4/res"));
    }

    @Test
    void read() {
        IBaseResource res = repository.read(Patient.class, new IdType("Patient/example"), null);
        assertEquals("example", res.getIdElement().getIdPart());
    }

    @Test
    void readLibrary() throws IOException {
        Library res = repository.read(Library.class, new IdType("Library/dependency-example"), null);
        assertEquals("dependency-example", res.getIdElement().getIdPart());
        assertTrue(IOUtils.toString(
                        new ByteArrayInputStream(res.getContent().get(0).getData()), StandardCharsets.UTF_8)
                .startsWith("library DependencyExample version '0.1.0'"));
    }

    @Test
    void testCreate() {
        Patient john = new Patient();
        john.setId(new IdType("Patient", "id-john-doe"));
        MethodOutcome methodOutcome = repository.create(john, null);
        assertTrue(methodOutcome.getCreated());
    }

    @Test
    void search() {
        IBaseBundle bundle = repository.search(
                IBaseBundle.class, Library.class, (Multimap<String, List<IQueryParameterType>>) null, null);
        assertEquals(6, ((Bundle) bundle).getEntry().size());
    }
}
