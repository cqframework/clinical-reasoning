package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;

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

    @Test
    void testResolveNullUrl() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);
        var result = resolver.resolveStructureDefinition(null);

        assertNull(result, "Should return null for null URL");
    }

    @Test
    void testResolveEmptyUrl() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);
        var result = resolver.resolveStructureDefinition("");

        assertNull(result, "Should return null for empty URL");
    }

    @Test
    void testResolveUnknownUrl() {
        var repository = mock(IRepository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var resolver = new ConformanceResourceResolver(repository);
        var result = resolver.resolveStructureDefinition("http://example.org/StructureDefinition/NonExistent");

        assertNull(result, "Should return null for unknown URL");
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
}
