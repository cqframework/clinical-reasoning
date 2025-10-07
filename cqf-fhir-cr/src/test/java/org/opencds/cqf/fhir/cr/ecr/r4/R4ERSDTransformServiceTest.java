package org.opencds.cqf.fhir.cr.ecr.r4;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

/**
 * Unit tests for R4ERSDTransformService
 */
public class R4ERSDTransformServiceTest {

    @Mock
    private IRepository repository;

    @InjectMocks
    private R4ERSDTransformService service;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testMissingAppAuthoritativeUrl_ThrowsException() {
        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class, () -> service.eRSDV2ImportOperation(new Bundle(), null));
        assertTrue(ex.getMessage().contains("appAuthoritativeUrl"));
    }

    @Test
    void testMissingResource_ThrowsException() {
        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class,
                () -> service.eRSDV2ImportOperation(null, "http://example.com/fhir"));
        assertTrue(ex.getMessage().contains("Resource is missing"));
    }

    @Test
    void testNonBundleResource_ThrowsException() {
        var fakeResource = new Patient();
        UnprocessableEntityException ex = assertThrows(
                UnprocessableEntityException.class,
                () -> service.eRSDV2ImportOperation(fakeResource, "http://example.com/fhir"));
        assertTrue(ex.getMessage().contains("Resource is not a bundle"));
    }

    @Test
    void testSuccessfulImportOperation() throws Exception {
        Bundle mockBundle = new Bundle();
        List<Bundle.BundleEntryComponent> transformedEntries = new ArrayList<>();
        transformedEntries.add(new Bundle.BundleEntryComponent());
        transformedEntries.add(new Bundle.BundleEntryComponent());

        try (MockedStatic<R4ImportBundleProducer> mockStatic = mockStatic(R4ImportBundleProducer.class)) {
            mockStatic
                    .when(() -> R4ImportBundleProducer.transformImportBundle(
                            any(Bundle.class), any(IRepository.class), anyString()))
                    .thenReturn(transformedEntries);

            when(repository.transaction(any(Bundle.class))).thenReturn(new Bundle());

            OperationOutcome outcome = service.eRSDV2ImportOperation(mockBundle, "http://example.com/fhir");

            assertNotNull(outcome);
            assertEquals(1, outcome.getIssue().size());
            String diag = outcome.getIssueFirstRep().getDiagnostics();
            assertTrue(diag.contains("Import completed in"));

            verify(repository, atLeastOnce()).transaction(any(Bundle.class));

            mockStatic.verify(() -> R4ImportBundleProducer.transformImportBundle(any(), any(), anyString()), times(1));
        }
    }

    @Test
    void testSplitListDividesCorrectly() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> parts = R4ERSDTransformService.splitList(list, 2);

        assertEquals(3, parts.size());
        assertEquals(List.of(1, 2), parts.get(0));
        assertEquals(List.of(3, 4), parts.get(1));
        assertEquals(List.of(5), parts.get(2));
    }

    @Test
    void testSplitListEmpty() {
        List<Integer> list = new ArrayList<>();
        List<List<Integer>> parts = R4ERSDTransformService.splitList(list, 3);
        assertTrue(parts.isEmpty());
    }

    @Test
    void testTransactionFailureDoesNotThrow() throws Exception {
        Bundle mockBundle = new Bundle();
        List<Bundle.BundleEntryComponent> transformedEntries = List.of(new Bundle.BundleEntryComponent());

        try (MockedStatic<R4ImportBundleProducer> mockStatic = mockStatic(R4ImportBundleProducer.class)) {
            mockStatic
                    .when(() -> R4ImportBundleProducer.transformImportBundle(any(), any(), anyString()))
                    .thenReturn(transformedEntries);

            doThrow(new RuntimeException("Simulated failure")).when(repository).transaction(any(Bundle.class));

            assertDoesNotThrow(() -> service.eRSDV2ImportOperation(mockBundle, "http://example.com/fhir"));
        }
    }
}
