package org.opencds.cqf.fhir.cql.cql2elm.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.cql.model.ModelIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepositoryForTests;

@SuppressWarnings("UnstableApiUsage")
class RepositoryFhirModelInfoProviderTest {

    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private IRepository repository;
    private IAdapterFactory adapterFactory;
    private RepositoryFhirModelInfoProvider fixture;
    private ModelIdentifier usCoreModelId =
            new ModelIdentifier().withId("USCore").withVersion("7.0.0");

    @BeforeEach
    void beforeEach() {
        var path = Path.of(getResourcePath(RepositoryFhirModelInfoProviderTest.class) + "/org/opencds/cqf/fhir/cql");
        repository = new IgRepositoryForTests(fhirContextR4, path);
        adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        fixture = new RepositoryFhirModelInfoProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
    }

    @Test
    void testFhirContext() {
        assertEquals(fhirContextR4, fixture.getFhirContext());
    }

    @Test
    void testGetLibrary() {
        var actual = fixture.getLibrary(usCoreModelId);
        assertNotNull(actual);
    }

    @Test
    void testLoadModelInfo() {
        var actual = fixture.load(usCoreModelId);
        assertNotNull(actual);
    }

    @Test
    void testLoadModelInfo_Exception() {
        var actual = fixture.load(new ModelIdentifier().withId("Invalid").withVersion("1.0.0"));
        assertNull(actual);
    }
}
