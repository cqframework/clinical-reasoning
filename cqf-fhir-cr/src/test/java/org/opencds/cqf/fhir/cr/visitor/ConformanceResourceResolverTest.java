package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;

class ConformanceResourceResolverTest {

    @Test
    void testResolveCoreFhirType() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);
        var result = resolver.resolveStructureDefinition("http://hl7.org/fhir/StructureDefinition/Patient");

        assertNotNull(result, "Should resolve core FHIR Patient type via DefaultProfileValidationSupport");
    }

    @Test
    void testResolveCoreFhirObservation() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);
        var result = resolver.resolveStructureDefinition("http://hl7.org/fhir/StructureDefinition/Observation");

        assertNotNull(result, "Should resolve core FHIR Observation type");
    }

    static Stream<String> unresolvedUrls() {
        return Stream.of(null, "", "http://example.org/StructureDefinition/NonExistent");
    }

    @ParameterizedTest
    @MethodSource("unresolvedUrls")
    void testResolveReturnsNullForUnresolvableUrl(String url) {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);
        assertNull(resolver.resolveStructureDefinition(url));
    }

    @Test
    void testGetFhirVersion() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);

        assertEquals(FhirVersionEnum.R4, resolver.getFhirVersion());
    }

    @Test
    void testConstructorWithEmptyDependsOn() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository, Collections.emptyList(), Collections.emptyList());

        // Should still resolve core types
        var result = resolver.resolveStructureDefinition("http://hl7.org/fhir/StructureDefinition/Patient");
        assertNotNull(result, "Should resolve core types even with empty dependsOn");
    }

    @Test
    void testCreateAdapter() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);
        var resource = resolver.resolveStructureDefinition("http://hl7.org/fhir/StructureDefinition/Patient");

        assertNotNull(resource);
        var adapter = resolver.createAdapter(resource);
        assertNotNull(adapter);
        assertEquals("Patient", adapter.getType());
    }

    @Test
    void testGetRepositoryReturnsOriginalWhenNoPackages() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);

        assertSame(repository, resolver.getFederatedRepository(), "Should return original repository when no packages");
    }

    @Test
    void testGetRepositoryReturnsFederatedWhenPackagesProvided() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var packages = new java.util.ArrayList<String[]>();
        packages.add(new String[] {"hl7.fhir.us.core", "6.1.0"});
        var resolver = new ConformanceResourceResolver(repository, packages, Collections.emptyList());

        var result = resolver.getFederatedRepository();
        assertNotNull(result);
        assertEquals(
                FederatedRepository.class,
                result.getClass(),
                "Should return FederatedRepository when packages are provided");
    }

    @Test
    void testGetResourceTypeReturnsNullWhenNoPackages() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);

        assertNull(resolver.getResourceType("http://example.org/ValueSet/test"));
    }

    @Test
    void testGetRepositoryReturnsOriginalWhenNullPackages() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository, null, Collections.emptyList());

        assertSame(
                repository,
                resolver.getFederatedRepository(),
                "Should return original repository when packages is null");
    }

    @Test
    void testGetResourceTypeReturnsCodeSystemForKnownUrl() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);

        assertEquals("CodeSystem", resolver.getResourceType("http://loinc.org"), "LOINC should be a known CodeSystem");
    }

    @Test
    void testGetVersionReturnsNullWhenNoPackages() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);

        assertNull(resolver.getVersion("http://example.org/ValueSet/unknown"));
    }

    @Test
    void testGetPackageInfoReturnsNullWhenNoPackages() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);

        assertNull(resolver.getPackageInfo("http://example.org/ValueSet/unknown"));
    }

    @Test
    void testResolveStructureDefinitionFromRepository() {
        var repository = new org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository(FhirContext.forR4Cached());

        // Add an SD to the repository
        var sd = new org.hl7.fhir.r4.model.StructureDefinition();
        sd.setId("StructureDefinition/test-sd");
        sd.setUrl("http://example.org/StructureDefinition/TestSD");
        sd.setVersion("1.0.0");
        sd.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
        sd.setName("TestSD");
        sd.setType("Patient");
        sd.setKind(org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind.RESOURCE);
        sd.setAbstract(false);
        repository.update(sd);

        var resolver = new ConformanceResourceResolver(repository);

        var result = resolver.resolveStructureDefinition("http://example.org/StructureDefinition/TestSD");
        assertNotNull(result, "Should resolve SD from repository (Tier 1)");
    }
}
