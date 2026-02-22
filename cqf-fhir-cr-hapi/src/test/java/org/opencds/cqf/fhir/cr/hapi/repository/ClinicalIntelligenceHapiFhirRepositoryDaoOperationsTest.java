package org.opencds.cqf.fhir.cr.hapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Map;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ClinicalIntelligenceHapiFhirRepositoryDaoOperationsTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();
    private static final String TENANT_ID = "test-tenant";
    private static final RequestPartitionId PARTITION_ID = RequestPartitionId.fromPartitionId(123);

    @Mock
    private DaoRegistry daoRegistry;

    @Mock
    private IFhirResourceDao<Patient> patientDao;

    @Mock
    private IFhirResourceDao<Library> libraryDao;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        lenient().when(daoRegistry.getFhirContext()).thenReturn(FHIR_CONTEXT);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ===== READ =====

    @Test
    void read_PartitionedResource_PreservesPartitionAndTenant() {
        when(daoRegistry.getResourceDao(Patient.class)).thenReturn(patientDao);
        when(patientDao.read(any(), any(RequestDetails.class))).thenReturn(new Patient());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.read(Patient.class, new IdType("Patient/123"), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(patientDao).read(any(), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.READ);
    }

    @Test
    void read_NonPartitionedResource_ClearsPartitionId() {
        when(daoRegistry.getResourceDao(Library.class)).thenReturn(libraryDao);
        when(libraryDao.read(any(), any(RequestDetails.class))).thenReturn(new Library());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.read(Library.class, new IdType("Library/456"), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(libraryDao).read(any(), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.READ);
    }

    @Test
    void read_PassesHeadersToClonedRequestDetails() {
        when(daoRegistry.getResourceDao(Patient.class)).thenReturn(patientDao);
        when(patientDao.read(any(), any(RequestDetails.class))).thenReturn(new Patient());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.read(Patient.class, new IdType("Patient/123"), Map.of("X-Custom", "header-value"));

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(patientDao).read(any(), captor.capture());
        assertEquals("header-value", captor.getValue().getHeader("X-Custom"));
    }

    @Test
    void read_WithoutPartitionInOriginalRequestDetails_DoesNotSetPartition() {
        when(daoRegistry.getResourceDao(Patient.class)).thenReturn(patientDao);
        when(patientDao.read(any(), any(RequestDetails.class))).thenReturn(new Patient());

        var repo = createRepository(new SystemRequestDetails());
        repo.read(Patient.class, new IdType("Patient/123"), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(patientDao).read(any(), captor.capture());
        assertInstanceOf(SystemRequestDetails.class, captor.getValue());
        assertNull(((SystemRequestDetails) captor.getValue()).getRequestPartitionId());
    }

    // ===== CREATE =====

    @Test
    void create_PartitionedResource_PreservesPartitionAndTenant() {
        doReturn(patientDao).when(daoRegistry).getResourceDao(any(Patient.class));
        when(patientDao.create(any(Patient.class), any(RequestDetails.class))).thenReturn(new DaoMethodOutcome());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.create(new Patient(), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(patientDao).create(any(Patient.class), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.CREATE);
    }

    @Test
    void create_NonPartitionedResource_PreservesPartitionId() {
        doReturn(libraryDao).when(daoRegistry).getResourceDao(any(Library.class));
        when(libraryDao.create(any(Library.class), any(RequestDetails.class))).thenReturn(new DaoMethodOutcome());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.create(new Library(), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(libraryDao).create(any(Library.class), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.CREATE);
    }

    // ===== UPDATE =====

    @Test
    void update_PartitionedResource_PreservesPartitionAndTenant_WhenCreated() {
        doReturn(patientDao).when(daoRegistry).getResourceDao(any(Patient.class));
        var outcome = new DaoMethodOutcome();
        outcome.setCreated(true);
        when(patientDao.update(any(Patient.class), any(RequestDetails.class))).thenReturn(outcome);

        var repo = createRepository(createPartitionedRequestDetails());
        var result = repo.update(new Patient(), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(patientDao).update(any(Patient.class), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.UPDATE);
        assertEquals(201, result.getResponseStatusCode());
    }

    @Test
    void update_SetsStatus200_WhenNotCreated() {
        doReturn(patientDao).when(daoRegistry).getResourceDao(any(Patient.class));
        var outcome = new DaoMethodOutcome();
        outcome.setCreated(false);
        when(patientDao.update(any(Patient.class), any(RequestDetails.class))).thenReturn(outcome);

        var repo = createRepository(createPartitionedRequestDetails());
        var result = repo.update(new Patient(), Map.of());

        assertEquals(200, result.getResponseStatusCode());
    }

    @Test
    void update_SetsStatus200_WhenCreatedIsNull() {
        doReturn(patientDao).when(daoRegistry).getResourceDao(any(Patient.class));
        when(patientDao.update(any(Patient.class), any(RequestDetails.class))).thenReturn(new DaoMethodOutcome());

        var repo = createRepository(createPartitionedRequestDetails());
        var result = repo.update(new Patient(), Map.of());

        assertEquals(200, result.getResponseStatusCode());
    }

    @Test
    void update_NonPartitionedResource_ClearsPartitionId() {
        doReturn(libraryDao).when(daoRegistry).getResourceDao(any(Library.class));
        var outcome = new DaoMethodOutcome();
        outcome.setCreated(false);
        when(libraryDao.update(any(Library.class), any(RequestDetails.class))).thenReturn(outcome);

        var repo = createRepository(createPartitionedRequestDetails());
        repo.update(new Library(), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(libraryDao).update(any(Library.class), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.UPDATE);
    }

    // ===== DELETE =====

    @Test
    void delete_PartitionedResource_PreservesPartitionAndTenant() {
        when(daoRegistry.getResourceDao(Patient.class)).thenReturn(patientDao);
        when(patientDao.delete(any(), any(RequestDetails.class))).thenReturn(new DaoMethodOutcome());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.delete(Patient.class, new IdType("Patient/123"), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(patientDao).delete(any(), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.DELETE);
    }

    @Test
    void delete_NonPartitionedResource_ClearsPartitionId() {
        when(daoRegistry.getResourceDao(Library.class)).thenReturn(libraryDao);
        when(libraryDao.delete(any(), any(RequestDetails.class))).thenReturn(new DaoMethodOutcome());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.delete(Library.class, new IdType("Library/456"), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(libraryDao).delete(any(), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.DELETE);
    }

    // ===== PATCH =====

    @Test
    void patch_PreservesPartition_BecauseParametersIsPartitionable() {
        // patch uses getResourceDao(String) for DAO lookup but cloneWithAction uses
        // patchParameters.getClass() (Parameters) for the partition check.
        // Parameters is not in NON_PARTITIONABLE_RESOURCE_TYPES, so partition is preserved.
        doReturn(patientDao).when(daoRegistry).getResourceDao("Patient");
        when(patientDao.patch(any(), any(), any(), any(), any(), any(RequestDetails.class)))
                .thenReturn(new DaoMethodOutcome());

        var repo = createRepository(createPartitionedRequestDetails());
        repo.patch(new IdType("Patient/123"), new Parameters(), Map.of());

        var captor = ArgumentCaptor.forClass(RequestDetails.class);
        verify(patientDao).patch(any(), isNull(), eq(PatchTypeEnum.FHIR_PATCH_JSON), isNull(), any(), captor.capture());
        assertPartitionPreserved(captor.getValue(), RestOperationTypeEnum.PATCH);
    }

    // ===== Helpers =====

    private SystemRequestDetails createPartitionedRequestDetails() {
        var details = new SystemRequestDetails();
        details.setRequestPartitionId(PARTITION_ID);
        details.setTenantId(TENANT_ID);
        return details;
    }

    private ClinicalIntelligenceHapiFhirRepository createRepository(SystemRequestDetails requestDetails) {
        return new ClinicalIntelligenceHapiFhirRepository(daoRegistry, requestDetails, new RestfulServer(FHIR_CONTEXT));
    }

    private void assertPartitionPreserved(RequestDetails clonedDetails, RestOperationTypeEnum expectedOp) {
        assertInstanceOf(SystemRequestDetails.class, clonedDetails);
        var systemDetails = (SystemRequestDetails) clonedDetails;
        assertEquals(expectedOp, clonedDetails.getRestOperationType());
        assertEquals(TENANT_ID, clonedDetails.getTenantId());
        assertEquals(PARTITION_ID, systemDetails.getRequestPartitionId());
    }

    private void assertPartitionCleared(RequestDetails clonedDetails, RestOperationTypeEnum expectedOp) {
        assertInstanceOf(SystemRequestDetails.class, clonedDetails);
        var systemDetails = (SystemRequestDetails) clonedDetails;
        assertEquals(expectedOp, clonedDetails.getRestOperationType());
        // Tenant ID is preserved because the SystemRequestDetails constructor copies it
        assertEquals(TENANT_ID, clonedDetails.getTenantId());
        // Partition ID is cleared for non-partitionable resources
        assertNull(systemDetails.getRequestPartitionId());
    }
}
