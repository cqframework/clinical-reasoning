package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.ig.IGRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IGRepositoryConfig;

class R4RepositoryTest {

    Repository repository;
    Path path = Paths.get(R4RepositoryTest.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath());

    public R4RepositoryTest() {
        repository = new IGRepository(
                FhirContext.forR4Cached(),
                path.resolve("org/opencds/cqf/fhir/cr/measure/r4/res").toString(),
                IGRepositoryConfig.WITH_CATEGORY_DIRECTORY_AND_TYPE_NAMES,
                EncodingEnum.JSON,
                null);
    }

    @Test
    void testRead() {
        IBaseResource res = repository.read(Patient.class, new IdType("Patient/example"), null);
        assertEquals("example", res.getIdElement().getIdPart());
    }

    @Test
    void testReadLibrary() throws IOException {
        Library res = repository.read(Library.class, new IdType("Library/dependency-example"), null);
        assertEquals("dependency-example", res.getIdElement().getIdPart());
        assertTrue(IOUtils.toString(
                        new ByteArrayInputStream(res.getContent().get(0).getData()), StandardCharsets.UTF_8)
                .startsWith("library DependencyExample version '0.1.0'"));
    }

    /*
     * todo :: work on ProxyRepository create()
     *
     * @Test public void testCreate() { Patient john = new Patient(); john.setId(new
     * IdType("Patient",
     * "id-john-doe")); MethodOutcome methodOutcome = repository.create(john, null);
     * assertTrue(methodOutcome.getCreated());
     *
     * repository.create(john, null); }
     */

    @Test
    void testSearch() {
        IBaseBundle bundle = repository.search(IBaseBundle.class, Library.class, null, null);
        assertEquals(6, ((Bundle) bundle).getEntry().size());
    }
}
