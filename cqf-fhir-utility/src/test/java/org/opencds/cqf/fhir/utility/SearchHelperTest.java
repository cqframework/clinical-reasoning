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

public class SearchHelperTest {
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
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Multimap.class), isNull())).thenReturn(bundle);
        return mockRepository;
    }

    IRepository mockRepositoryWithValueSetR5(org.hl7.fhir.r5.model.ValueSet valueSet) {
        var mockRepository = mock(IRepository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forR5Cached());
        org.hl7.fhir.r5.model.Bundle bundle = new org.hl7.fhir.r5.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Multimap.class), isNull())).thenReturn(bundle);
        return mockRepository;
    }

    IRepository mockRepositoryWithValueSetDstu3(org.hl7.fhir.dstu3.model.ValueSet valueSet) {
        var mockRepository = mock(IRepository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forDstu3Cached());
        org.hl7.fhir.dstu3.model.Bundle bundle = new org.hl7.fhir.dstu3.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Multimap.class), isNull())).thenReturn(bundle);
        return mockRepository;
    }
}
