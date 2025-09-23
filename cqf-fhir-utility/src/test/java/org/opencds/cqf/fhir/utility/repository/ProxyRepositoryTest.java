package org.opencds.cqf.fhir.utility.repository;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class ProxyRepositoryTest {
    private static IRepository repository;
    private static IRepository localRepository;
    private static IRepository mockInMemoryFhirRepository;
    private static IRepository mockRestRepository;

    @BeforeAll
    @SuppressWarnings("unused")
    static void setup() {
        localRepository = mock(IgRepository.class);
        mockInMemoryFhirRepository = mock(InMemoryFhirRepository.class);
        mockRestRepository = mock(RestRepository.class);
        repository = new ProxyRepository(localRepository, mockInMemoryFhirRepository, mockRestRepository);
    }

    @Test
    void testLink() {
        Bundle emptyBundle = new Bundle();
        emptyBundle.setType(BundleType.COLLECTION);
        emptyBundle.setId("empty-bundle");
        doReturn(emptyBundle).when(localRepository).link(any(), any(), any());
        var error = new NotImplementedOperationException("Paging is not currently supported");
        doThrow(error).when(mockInMemoryFhirRepository).link(any(), any(), any());
        Bundle bundle = new Bundle();
        bundle.setType(BundleType.COLLECTION);
        bundle.setId("simple-bundle");
        Bundle.BundleEntryComponent bec = new Bundle.BundleEntryComponent();
        bec.setResource(new Patient().setId("simple-patient"));
        bundle.addEntry(bec);
        doReturn(bundle).when(mockRestRepository).link(any(), any(), any());

        Bundle result = repository.link(Bundle.class, "url");
        assertNotNull(result);
        assertEquals("simple-bundle", result.getIdElement().getIdPart());
        verify(localRepository, times(1)).link(any(), any(), any());
        verify(mockInMemoryFhirRepository, times(1)).link(any(), any(), any());
        verify(mockRestRepository, times(1)).link(any(), any(), any());
        assertSame(bundle, result);
    }
}
