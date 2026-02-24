package org.opencds.cqf.fhir.utility.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import java.util.Map;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;

class VersionSpecificHelperTest {

    private IRepository mockR4Repo() {
        var repo = mock(IRepository.class);
        when(repo.fhirContext()).thenReturn(FhirContext.forR4Cached());
        return repo;
    }

    // -- R4 SearchHelper --

    @Test
    void searchRepositoryByCanonicalR4() {
        var repo = mockR4Repo();
        var vs = new ValueSet().setId("test-vs");
        var bundle = new Bundle();
        bundle.addEntry().setResource(vs);
        when(repo.search(any(), any(), any(Map.class))).thenReturn(bundle);
        var result = SearchHelper.searchRepositoryByCanonical(
                repo, new org.hl7.fhir.r4.model.CanonicalType("http://example.org/ValueSet/test"));
        assertEquals("test-vs", result.getIdElement().getIdPart());
    }

    @Test
    void searchRepositoryByCanonicalWithVersionR4() {
        var repo = mockR4Repo();
        var vs = new ValueSet().setId("test-vs");
        var bundle = new Bundle();
        bundle.addEntry().setResource(vs);
        when(repo.search(any(), any(), any(Map.class))).thenReturn(bundle);
        var result = SearchHelper.searchRepositoryByCanonical(
                repo, new org.hl7.fhir.r4.model.CanonicalType("http://example.org/ValueSet/test|1.0"));
        assertNotNull(result);
    }

    @Test
    void searchRepositoryByCanonicalNotFoundThrows() {
        var repo = mockR4Repo();
        when(repo.search(any(), any(), any(Map.class))).thenReturn(new Bundle());
        assertThrows(
                FHIRException.class,
                () -> SearchHelper.searchRepositoryByCanonical(
                        repo, new org.hl7.fhir.r4.model.CanonicalType("http://example.org/ValueSet/missing")));
    }

    @Test
    void searchRepositoryWithPagingR4() {
        var repo = mockR4Repo();
        var bundle = new Bundle();
        bundle.addEntry().setResource(new ValueSet().setId("vs1"));
        when(repo.search(any(), any(), any(Map.class), any())).thenReturn(bundle);
        var result = SearchHelper.searchRepositoryWithPaging(
                repo, ValueSet.class, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(1, result.getEntry().size());
    }

    @Test
    void searchRepositoryWithPagingAndNextLink() {
        var repo = mockR4Repo();
        var bundle1 = new Bundle();
        bundle1.addEntry().setResource(new ValueSet().setId("vs1"));
        bundle1.addLink().setRelation("next").setUrl("http://example.com/next");
        when(repo.search(any(), any(), any(Map.class), any())).thenReturn(bundle1);

        var bundle2 = new Bundle();
        bundle2.addEntry().setResource(new ValueSet().setId("vs2"));
        when(repo.link(any(), any(String.class))).thenReturn(bundle2);

        var result = SearchHelper.searchRepositoryWithPaging(
                repo, ValueSet.class, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(2, result.getEntry().size());
    }

    // -- R4 MetadataResourceHelper --

    @Test
    void forEachMetadataResourceR4() {
        var repo = mockR4Repo();
        var library = new org.hl7.fhir.r4.model.Library();
        library.setId("lib1");
        when(repo.read(any(), any())).thenReturn(library);

        var entry = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
        entry.getResponse().setLocation("Library/lib1");

        var results = new java.util.ArrayList<>();
        MetadataResourceHelper.forEachMetadataResource(java.util.List.of(entry), results::add, repo);
        assertEquals(1, results.size());
    }

    @Test
    void forEachMetadataResourceMultipleTypes() {
        var repo = mockR4Repo();
        when(repo.read(any(), any())).thenReturn(new org.hl7.fhir.r4.model.ValueSet());

        var entries = java.util.List.of(
                entryWithLocation("ValueSet/vs1"),
                entryWithLocation("Measure/m1"),
                entryWithLocation("PlanDefinition/pd1"),
                entryWithLocation("ActivityDefinition/ad1"),
                entryWithLocation("UnknownType/x1"));

        var results = new java.util.ArrayList<>();
        MetadataResourceHelper.forEachMetadataResource(entries, results::add, repo);
        assertEquals(5, results.size());
    }

    private org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entryWithLocation(String location) {
        var entry = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
        entry.getResponse().setLocation(location);
        return entry;
    }
}
