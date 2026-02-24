package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.collect.Multimap;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
class SearchHelperTest {
    private IRepository r4mockRepository;
    private IRepository r5mockRepository;
    private IRepository dstu3mockRepository;

    @BeforeEach
    void resetRepository() {
        r4mockRepository = mockRepositoryWithValueSetR4(new org.hl7.fhir.r4.model.ValueSet());
        r5mockRepository = mockRepositoryWithValueSetR5(new org.hl7.fhir.r5.model.ValueSet());
        dstu3mockRepository = mockRepositoryWithValueSetDstu3(new org.hl7.fhir.dstu3.model.ValueSet());
    }

    @Test
    void getResourceTypeTest() {
        var validResourceTypeR4Canonical = new org.hl7.fhir.r4.model.CanonicalType("www.test.com/fhir/ValueSet/123");
        assertEquals(ValueSet.class, SearchHelper.getResourceType(r4mockRepository, validResourceTypeR4Canonical));
        var invalidResourceTypeR4Canonical =
                new org.hl7.fhir.r4.model.CanonicalType("www.test.com/fhir/invalid-resource-type/123");
        assertEquals(
                org.hl7.fhir.r4.model.CodeSystem.class,
                SearchHelper.getResourceType(r4mockRepository, invalidResourceTypeR4Canonical));
        var cqfResourceType = "Condition";
        var invalidResourceTypeR4CanonicalWithCqfResourceTypeExtension =
                new org.hl7.fhir.r4.model.CanonicalType("www.test.com/fhir/invalid-resource-type/123");
        invalidResourceTypeR4CanonicalWithCqfResourceTypeExtension
                .addExtension()
                .setUrl(Constants.CQF_RESOURCETYPE)
                .setValue(new org.hl7.fhir.r4.model.CodeType(cqfResourceType));
        assertEquals(
                org.hl7.fhir.r4.model.Condition.class,
                SearchHelper.getResourceType(
                        r4mockRepository, invalidResourceTypeR4CanonicalWithCqfResourceTypeExtension));

        var validResourceTypeR5Canonical = new org.hl7.fhir.r5.model.CanonicalType("www.test.com/fhir/ValueSet/123");
        assertEquals(
                org.hl7.fhir.r5.model.ValueSet.class,
                SearchHelper.getResourceType(r5mockRepository, validResourceTypeR5Canonical));
        var invalidResourceTypeR5Canonical =
                new org.hl7.fhir.r5.model.CanonicalType("www.test.com/fhir/invalid-resource-type/123");
        assertEquals(
                org.hl7.fhir.r5.model.CodeSystem.class,
                SearchHelper.getResourceType(r5mockRepository, invalidResourceTypeR5Canonical));
        var invalidResourceTypeR5CanonicalWithCqfResourceTypeExtension =
                new org.hl7.fhir.r5.model.CanonicalType("www.test.com/fhir/invalid-resource-type/123");
        invalidResourceTypeR5CanonicalWithCqfResourceTypeExtension
                .addExtension()
                .setUrl(Constants.CQF_RESOURCETYPE)
                .setValue(new org.hl7.fhir.r5.model.CodeType(cqfResourceType));
        assertEquals(
                org.hl7.fhir.r5.model.Condition.class,
                SearchHelper.getResourceType(
                        r5mockRepository, invalidResourceTypeR5CanonicalWithCqfResourceTypeExtension));

        var validResourceTypeDstu3Canonical = new org.hl7.fhir.dstu3.model.UriType("www.test.com/fhir/ValueSet/123");
        assertEquals(
                org.hl7.fhir.dstu3.model.ValueSet.class,
                SearchHelper.getResourceType(dstu3mockRepository, validResourceTypeDstu3Canonical));
        var invalidResourceTypeDstu3Canonical =
                new org.hl7.fhir.dstu3.model.UriType("www.test.com/fhir/invalid-resource-type/123");
        assertEquals(
                org.hl7.fhir.dstu3.model.CodeSystem.class,
                SearchHelper.getResourceType(dstu3mockRepository, invalidResourceTypeDstu3Canonical));
        var invalidResourceTypeDstu3CanonicalWithCqfResourceTypeExtension =
                new org.hl7.fhir.dstu3.model.UriType("www.test.com/fhir/invalid-resource-type/123");
        invalidResourceTypeDstu3CanonicalWithCqfResourceTypeExtension
                .addExtension()
                .setUrl(Constants.CQF_RESOURCETYPE)
                .setValue(new org.hl7.fhir.dstu3.model.CodeType(cqfResourceType));
        assertEquals(
                org.hl7.fhir.dstu3.model.Condition.class,
                SearchHelper.getResourceType(
                        dstu3mockRepository, invalidResourceTypeDstu3CanonicalWithCqfResourceTypeExtension));
    }

    IRepository mockRepositoryWithValueSetR4(org.hl7.fhir.r4.model.ValueSet valueSet) {
        var mockRepository = mock(IRepository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        var bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Multimap.class), isNull())).thenReturn(bundle);
        return mockRepository;
    }

    IRepository mockRepositoryWithValueSetR5(org.hl7.fhir.r5.model.ValueSet valueSet) {
        var mockRepository = mock(IRepository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forR5Cached());
        var bundle = new org.hl7.fhir.r5.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Multimap.class), isNull())).thenReturn(bundle);
        return mockRepository;
    }

    IRepository mockRepositoryWithValueSetDstu3(org.hl7.fhir.dstu3.model.ValueSet valueSet) {
        var mockRepository = mock(IRepository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forDstu3Cached());
        var bundle = new org.hl7.fhir.dstu3.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Multimap.class), isNull())).thenReturn(bundle);
        return mockRepository;
    }

    @Test
    void readRepository() {
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setId("Patient/123");
        when(r4mockRepository.read(any(), any())).thenReturn(patient);
        var result = SearchHelper.readRepository(r4mockRepository, new org.hl7.fhir.r4.model.IdType("Patient/123"));
        assertEquals("Patient/123", result.getIdElement().getValue());
    }

    @Test
    void getResourceClass() {
        var result = SearchHelper.getResourceClass(r4mockRepository, "Patient");
        assertEquals(org.hl7.fhir.r4.model.Patient.class, result);
    }

    @Test
    void searchRepositoryByCanonicalR4() {
        var vs = new org.hl7.fhir.r4.model.ValueSet();
        vs.setId("test-vs");
        vs.setUrl("http://example.org/ValueSet/test");
        var bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setResource(vs);
        // searchRepositoryByCanonical calls the 3-arg search (no headers)
        when(r4mockRepository.search(any(), any(), any(java.util.Map.class))).thenReturn(bundle);
        var canonical = new org.hl7.fhir.r4.model.CanonicalType("http://example.org/ValueSet/test");
        var result = SearchHelper.searchRepositoryByCanonical(r4mockRepository, canonical);
        assertEquals("test-vs", result.getIdElement().getIdPart());
    }

    private IRepository fullMockR4() {
        var repo = mock(IRepository.class);
        when(repo.fhirContext()).thenReturn(FhirContext.forR4Cached());
        var bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setResource(new org.hl7.fhir.r4.model.ValueSet().setId("vs1"));
        when(repo.search(any(), any(), any(Multimap.class), any())).thenReturn(bundle);
        when(repo.search(any(), any(), any(java.util.Map.class), any())).thenReturn(bundle);
        when(repo.search(any(), any(), any(java.util.Map.class))).thenReturn(bundle);
        return repo;
    }

    private IRepository fullMockDstu3() {
        var repo = mock(IRepository.class);
        when(repo.fhirContext()).thenReturn(FhirContext.forDstu3Cached());
        var bundle = new org.hl7.fhir.dstu3.model.Bundle();
        bundle.addEntry().setResource(new org.hl7.fhir.dstu3.model.ValueSet().setId("vs1"));
        when(repo.search(any(), any(), any(Multimap.class), any())).thenReturn(bundle);
        return repo;
    }

    private IRepository fullMockR5() {
        var repo = mock(IRepository.class);
        when(repo.fhirContext()).thenReturn(FhirContext.forR5Cached());
        var bundle = new org.hl7.fhir.r5.model.Bundle();
        bundle.addEntry().setResource(new org.hl7.fhir.r5.model.ValueSet().setId("vs1"));
        when(repo.search(any(), any(), any(Multimap.class), any())).thenReturn(bundle);
        return repo;
    }

    @Test
    void searchRepositoryByCanonicalWithPagingR4() {
        var repo = fullMockR4();
        var canonical = new org.hl7.fhir.r4.model.CanonicalType("http://example.org/ValueSet/test|1.0");
        var result = SearchHelper.searchRepositoryByCanonicalWithPaging(repo, canonical);
        assertEquals(1, BundleHelper.getEntryResources(result).size());
    }

    @Test
    void searchRepositoryByCanonicalWithPagingString() {
        var repo = fullMockR4();
        var result = SearchHelper.searchRepositoryByCanonicalWithPaging(repo, "http://example.org/ValueSet/test");
        assertEquals(1, BundleHelper.getEntryResources(result).size());
    }

    @Test
    void searchRepositoryByCanonicalWithPagingWithParams() {
        var repo = fullMockR4();
        var result = SearchHelper.searchRepositoryByCanonicalWithPagingWithParams(
                repo, "http://example.org/ValueSet/test|1.0", java.util.Collections.emptyMap());
        assertEquals(1, BundleHelper.getEntryResources(result).size());
    }

    @Test
    void searchRepositoryWithPagingR4() {
        var repo = fullMockR4();
        var result = SearchHelper.searchRepositoryWithPaging(
                repo,
                org.hl7.fhir.r4.model.ValueSet.class,
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap());
        assertEquals(1, BundleHelper.getEntryResources(result).size());
    }

    @Test
    void searchRepositoryWithPagingDstu3() {
        var repo = fullMockDstu3();
        var result = SearchHelper.searchRepositoryWithPaging(
                repo,
                org.hl7.fhir.dstu3.model.ValueSet.class,
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap());
        assertEquals(1, BundleHelper.getEntryResources(result).size());
    }

    @Test
    void searchRepositoryWithPagingR5() {
        var repo = fullMockR5();
        var result = SearchHelper.searchRepositoryWithPaging(
                repo,
                org.hl7.fhir.r5.model.ValueSet.class,
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap());
        assertEquals(1, BundleHelper.getEntryResources(result).size());
    }
}
