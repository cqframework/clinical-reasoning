package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

class NpmRepositoryTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    @Test
    void readFromCacheReturnsResourceWithoutLoadingPackages() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        // Manually add a resource via update
        var patient = new Patient();
        patient.setId("Patient/test-1");
        repo.update(patient);

        // Should return from cache without trying to load packages
        var result = repo.read(Patient.class, new IdType("Patient/test-1"));
        assertNotNull(result);
    }

    @Test
    void readThrowsWhenPackagesEmptyAndResourceNotCached() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());
        var id = new IdType("Patient/nonexistent");

        assertThrows(ResourceNotFoundException.class, () -> repo.read(Resource.class, id));
    }

    @Test
    void readThrowsWhenPackagesNullAndResourceNotCached() {
        var repo = new NpmRepository(fhirContext, null);
        var id = new IdType("Patient/nonexistent");

        assertThrows(ResourceNotFoundException.class, () -> repo.read(Resource.class, id));
    }

    @Test
    void getLoadedPackagesReturnsEmptyListWhenNoPackages() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        var packages = repo.getLoadedPackages();
        assertNotNull(packages);
        assertTrue(packages.isEmpty());
    }

    @Test
    void constructorHandlesNullPackageList() {
        var repo = new NpmRepository(fhirContext, null);

        // Should not throw
        var packages = repo.getLoadedPackages();
        assertNotNull(packages);
        assertTrue(packages.isEmpty());
    }

    @Test
    void getResourceTypeReturnsNullWhenNoPackages() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        assertNull(repo.getResourceType("http://example.org/ValueSet/test"));
    }

    @Test
    void getResourceTypeReturnsNullForNullUrl() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        assertNull(repo.getResourceType(null));
    }

    @Test
    void searchReturnsResourceAfterReadPopulatesCache() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        // Manually cache a patient
        var patient = new Patient();
        patient.setId("Patient/test-search");
        patient.addName().setFamily("TestFamily");
        repo.update(patient);

        // Search should find it
        Multimap<String, List<IQueryParameterType>> searchParams = null;
        var bundle =
                repo.search(org.hl7.fhir.r4.model.Bundle.class, Patient.class, searchParams, Collections.emptyMap());
        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
    }

    @Test
    void getVersionReturnsNullForUnknownUrl() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        assertNull(repo.getVersion("http://example.org/ValueSet/unknown"));
    }

    @Test
    void getVersionReturnsNullForNull() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        assertNull(repo.getVersion(null));
    }

    @Test
    void getPackageInfoReturnsNullForUnknownUrl() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        assertNull(repo.getPackageInfo("http://example.org/ValueSet/unknown"));
    }

    @Test
    void getPackageInfoReturnsNullForNull() {
        var repo = new NpmRepository(fhirContext, Collections.emptyList());

        assertNull(repo.getPackageInfo(null));
    }

    @Test
    void isKnownCodeSystemReturnsTrueForWellKnown() {
        assertTrue(
                org.opencds.cqf.fhir.utility.terminology.CodeSystems.isKnownCodeSystem("http://loinc.org"),
                "LOINC should be a known CodeSystem");
    }

    @Test
    void isKnownCodeSystemReturnsFalseForUnknown() {
        assertFalse(
                org.opencds.cqf.fhir.utility.terminology.CodeSystems.isKnownCodeSystem(
                        "http://example.org/CodeSystem/unknown"),
                "Unknown URL should not be a known CodeSystem");
    }
}
