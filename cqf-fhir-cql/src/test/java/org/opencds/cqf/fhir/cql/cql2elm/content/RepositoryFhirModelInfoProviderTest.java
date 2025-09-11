package org.opencds.cqf.fhir.cql.cql2elm.content;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.cql.model.ModelIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings("UnstableApiUsage")
class RepositoryFhirModelInfoProviderTest {

    private IRepository repository;
    private IAdapterFactory adapterFactory;
    private RepositoryFhirModelInfoProvider fixture;
    private ModelIdentifier usCoreModelId =
            new ModelIdentifier().withId("USCore").withVersion("7.0.0");

    @BeforeEach
    public void beforeEach() {
        var path = Path.of(getResourcePath(RepositoryFhirModelInfoProviderTest.class) + "/org/opencds/cqf/fhir/cql");
        repository = new IgRepository(FhirContext.forR4Cached(), path);
        adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        fixture = new RepositoryFhirModelInfoProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
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
}
