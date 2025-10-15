package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;

class CdsResponseEncoderServiceTest {

    @Test
    void methodEncodeResponse_shouldThrowExceptionWhenResponseIsNotIBaseResource() {
        // given
        Object invalidResponse = new Object();
        IAdapterFactory mockAdapterFactory = mock(IAdapterFactory.class);
        IRepository mockRepository = mock(IRepository.class);

        CdsResponseEncoderService encoder = new CdsResponseEncoderService(mockRepository, mockAdapterFactory);

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> encoder.encodeResponse(invalidResponse));

        // then
        assertEquals("response is not an instance of a Resource", exception.getMessage());
    }

    @Test
    void methodEncodeResponse_shouldThrowExceptionWhenResponseIsParametersWithoutBundle() {
        // given
        IBaseParameters mockParameters = mock(IBaseParameters.class);
        IAdapterFactory mockAdapterFactory = mock(IAdapterFactory.class);
        IRepository mockRepository = mock(IRepository.class);

        IParametersAdapter mockParametersAdapter = mock(IParametersAdapter.class);
        when(mockAdapterFactory.createParameters(mockParameters)).thenReturn(mockParametersAdapter);
        when(mockParametersAdapter.getParameter()).thenReturn(List.of());

        CdsResponseEncoderService encoder = new CdsResponseEncoderService(mockRepository, mockAdapterFactory);

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> encoder.encodeResponse(mockParameters));

        // then
        assertEquals("response does not contain a Bundle", exception.getMessage());
    }

    @Test
    void methodEncodeResponse_shouldThrowExceptionWhenUnableToResolveResponse() {
        // given
        IRepository mockRepository = mock(IRepository.class);
        var adapterFactory = new AdapterFactory();
        var emptyBundle = new Bundle();

        final Parameters parameters = new Parameters()
                .addParameter(
                        new ParametersParameterComponent().setName("return").setResource(emptyBundle));

        CdsResponseEncoderService encoder = new CdsResponseEncoderService(mockRepository, adapterFactory);

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> encoder.encodeResponse(parameters));

        // then
        assertEquals("unable to resolve response", exception.getMessage());
    }
}
