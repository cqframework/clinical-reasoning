package org.opencds.cqf.fhir.cql.cql2elm.content;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.cql.model.ModelIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

@SuppressWarnings("UnstableApiUsage")
class RepositoryFhirModelInfoProviderTest {

    private IRepository repository;

    @BeforeEach
    public void beforeEach() {
        var path = Path.of(getResourcePath(RepositoryFhirModelInfoProviderTest.class) + "/org/opencds/cqf/fhir/cql");
        repository = new IgRepository(FhirContext.forR4Cached(), path);
    }

    @Test
    void testGetLibrary() {
        var adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        var fixture = new RepositoryFhirModelInfoProvider(repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
        var actual = fixture.getLibrary(new ModelIdentifier().withId("USCore").withVersion("7.0.0"));
        assertNotNull(actual);
    }

    @Test
    void testLoadModelInfo() {
        var adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        var fixture = new RepositoryFhirModelInfoProvider(repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
        var actual = fixture.load(new ModelIdentifier().withId("USCore").withVersion("7.0.0"));
        assertNotNull(actual);
    }
}
