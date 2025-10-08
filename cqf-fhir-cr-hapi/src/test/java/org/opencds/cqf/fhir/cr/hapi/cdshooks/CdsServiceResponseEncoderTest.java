package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;

class CdsServiceResponseEncoderTest {

    /**
     * Tests the {@code encodeResponse} method when the response is not an instance
     * of {@link IBaseResource}.
     */
    @Test
    void encodeResponseShouldThrowExceptionWhenResponseIsNotIBaseResource() {
        // setup
        Object invalidResponse = new Object();
        IAdapterFactory mockAdapterFactory = mock(IAdapterFactory.class);
        IRepository mockRepository = mock(IRepository.class);

        CdsServiceResponseEncoder encoder =
                new CdsServiceResponseEncoder(invalidResponse, mockAdapterFactory, mockRepository);

        // execute & assert
        Exception exception = assertThrows(RuntimeException.class, encoder::encodeResponse);
        assertEquals("response is not an instance of a Resource", exception.getMessage());
    }

    /**
     * Tests the {@code encodeResponse} method when the response is an instance
     * of {@link IBaseParameters}, but does not contain a Bundle.
     */
    @Test
    void encodeResponseShouldThrowExceptionWhenResponseIsParametersWithoutBundle() {
        // setup
        IBaseParameters mockParameters = mock(IBaseParameters.class);
        IAdapterFactory mockAdapterFactory = mock(IAdapterFactory.class);
        IRepository mockRepository = mock(IRepository.class);

        IParametersAdapter mockParametersAdapter = mock(IParametersAdapter.class);
        when(mockAdapterFactory.createParameters(mockParameters)).thenReturn(mockParametersAdapter);
        when(mockParametersAdapter.getParameter()).thenReturn(List.of());

        CdsServiceResponseEncoder encoder =
                new CdsServiceResponseEncoder(mockParameters, mockAdapterFactory, mockRepository);

        // execute & assert
        Exception exception = assertThrows(RuntimeException.class, encoder::encodeResponse);
        assertEquals("response does not contain a Bundle", exception.getMessage());
    }

    /**
     * Tests the {@code encodeResponse} method when the response cannot be resolved.
     */
    @Test
    void encodeResponseShouldThrowExceptionWhenUnableToResolveResponse() {
        // setup
        IRepository mockRepository = mock(IRepository.class);
        var adapterFactory = new AdapterFactory();
        var emptyBundle = new Bundle();

        final Parameters parameters = new Parameters()
            .addParameter(
                new ParametersParameterComponent().setName("return").setResource(emptyBundle));


        CdsServiceResponseEncoder encoder =
                new CdsServiceResponseEncoder(parameters, adapterFactory, mockRepository);

        // execute & assert
        Exception exception = assertThrows(RuntimeException.class, encoder::encodeResponse);
        assertEquals("unable to resolve response", exception.getMessage());
    }

}