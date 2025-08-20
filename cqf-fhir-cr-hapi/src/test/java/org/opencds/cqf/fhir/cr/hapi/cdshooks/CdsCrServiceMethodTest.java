package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CdsCrServiceMethodTest {

    private final CdsServiceJson cdsServiceJson = new CdsServiceJson();
    private final String serviceId = "serviceId";

    @Mock
    private ICdsCrServiceFactory cdsCrServiceFactory;

    @Mock
    private ICdsCrService cdsCrService;

    private CdsCrServiceMethod testSubject;

    @BeforeEach
    void beforeEach() {
        testSubject = new CdsCrServiceMethod(cdsServiceJson, cdsCrServiceFactory);
    }

    @Test
    void testServiceMethodInvoke_callFactoryGeneratedCdsService() {
        when(cdsCrServiceFactory.create(serviceId)).thenReturn(cdsCrService);
        IModelJson cdsServiceRequestJson = new CdsServiceRequestJson();

        testSubject.invoke(cdsServiceRequestJson, serviceId);

        verify(cdsCrService, times(1)).invoke(cdsServiceRequestJson);
    }

    @Test
    void testCreateCdsCrServiceWithServiceFactory() {
        testSubject.createCdsCrService(serviceId);
        verify(cdsCrServiceFactory, times(1)).create(serviceId);
    }

    @Test
    void testGetCdsServiceJson() {
        assertThat(testSubject.getCdsServiceJson()).isEqualTo(cdsServiceJson);
    }

    @Test
    void testisAllowAutoFhirClientPrefetch() {
        assertThat(testSubject.isAllowAutoFhirClientPrefetch()).isTrue();
    }

    @Test
    void testExceptionCauseIsThrown_whenExceptionCause_isInstanceOfBaseServerResponseException() {
        when(cdsCrServiceFactory.create(serviceId)).thenReturn(cdsCrService);
        RuntimeException exception = Mockito.mock(RuntimeException.class);
        when(exception.getCause()).thenReturn(Mockito.mock(BaseServerResponseException.class));
        when(cdsCrService.invoke(new CdsServiceRequestJson())).thenThrow(exception);

        assertThatThrownBy(() -> testSubject.invoke(new ObjectMapper(), new CdsServiceRequestJson(), serviceId))
                .isInstanceOf(BaseServerResponseException.class);
    }

    @Test
    void testInternalErrorExceptionWrappingOriginalException_isThrown_whenInvokeThrowsException() {
        when(cdsCrServiceFactory.create(serviceId)).thenReturn(cdsCrService);
        RuntimeException exception = Mockito.mock(RuntimeException.class);
        when(exception.getCause()).thenReturn(Mockito.mock(Exception.class));
        when(cdsCrService.invoke(new CdsServiceRequestJson())).thenThrow(exception);

        assertThatThrownBy(() -> testSubject.invoke(new ObjectMapper(), new CdsServiceRequestJson(), serviceId))
                .isInstanceOf(InternalErrorException.class);
    }
}
