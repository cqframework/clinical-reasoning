package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static ca.uhn.fhir.jpa.cache.ResourceChangeEvent.fromCreatedUpdatedDeletedResourceIds;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_CR_MODULE_ID;

import ca.uhn.fhir.jpa.cache.ResourceChangeEvent;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsServiceRegistryImpl;
import java.util.List;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICrDiscoveryService;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICrDiscoveryServiceFactory;

@ExtendWith(MockitoExtension.class)
class CdsServiceInterceptorTest {

    @Mock
    private CdsServiceRegistryImpl cdsServiceRegistry;

    @Mock
    private ICrDiscoveryServiceFactory discoveryServiceFactory;

    @Mock
    private ICdsCrServiceFactory crServiceFactory;

    @InjectMocks
    private CdsServiceInterceptor fixture;

    private InOrder inOrder;

    private static final String ID = "myId";
    private static final IIdType ID_TYPE = new IdType(ID);
    private static final List<IIdType> ID_TYPE_LIST = List.of(ID_TYPE);

    @BeforeEach
    void beforeEach() {
        inOrder = Mockito.inOrder(cdsServiceRegistry, discoveryServiceFactory);
    }

    @Test
    void testHandleInit_willRegisterServices() {
        initializeCrDiscoveryServiceFactoryMock();

        fixture.handleInit(ID_TYPE_LIST);

        assertInsertInteractions();
    }

    @Test
    void testHandleServiceCreation_willRegisterServices() {
        initializeCrDiscoveryServiceFactoryMock();
        ResourceChangeEvent resourceChangeEvent =
                fromCreatedUpdatedDeletedResourceIds(ID_TYPE_LIST, emptyList(), emptyList());

        fixture.handleChange(resourceChangeEvent);

        assertInsertInteractions();
    }

    @Test
    void testHandleServiceUpdate_willUpdateRegisteredServices() {
        initializeCrDiscoveryServiceFactoryMock();

        ResourceChangeEvent resourceChangeEvent =
                fromCreatedUpdatedDeletedResourceIds(emptyList(), ID_TYPE_LIST, emptyList());

        fixture.handleChange(resourceChangeEvent);

        inOrder.verify(cdsServiceRegistry, times(1)).unregisterService(ID, CDS_CR_MODULE_ID);

        assertInsertInteractions();
    }

    @Test
    void testHandleServiceDelete_willDeleteRegisteredServices() {
        ResourceChangeEvent resourceChangeEvent =
                fromCreatedUpdatedDeletedResourceIds(emptyList(), emptyList(), ID_TYPE_LIST);

        fixture.handleChange(resourceChangeEvent);

        verify(cdsServiceRegistry, times(1)).unregisterService(ID, CDS_CR_MODULE_ID);
        verifyNoInteractions(crServiceFactory, discoveryServiceFactory);
    }

    private void initializeCrDiscoveryServiceFactoryMock() {
        when(discoveryServiceFactory.create(ID)).thenAnswer(theInvocationOnMock -> (ICrDiscoveryService) CdsServiceJson::new);
    }

    private void assertInsertInteractions() {
        inOrder.verify(discoveryServiceFactory, times(1)).create(ID);
        inOrder.verify(cdsServiceRegistry, times(1))
                .registerService(ID, any(), any(), true, CDS_CR_MODULE_ID);
    }
}
