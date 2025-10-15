package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("UnstableApiUsage")
class CdsResponseEncoderServiceTest {

    private IRepository repository;

    @BeforeEach
    void beforeEach() {
         repository = new InMemoryFhirRepository(FhirContext.forR4());
    }

    @Test
    void methodEncodeResponse_shouldThrowExceptionWhenResponseIsNotIBaseResource() {
        // given
        Object invalidResponse = new Object();
        CdsResponseEncoderService encoder = new CdsResponseEncoderService(repository);

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> encoder.encodeResponse(invalidResponse));

        // then
        assertEquals("response is not an instance of a Resource", exception.getMessage());
    }

    @Test
    void methodEncodeResponse_shouldThrowExceptionWhenResponseIsParametersWithoutBundle() {
        // given
        Parameters parameters = new Parameters();

        CdsResponseEncoderService encoder = new CdsResponseEncoderService(repository);

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> encoder.encodeResponse(parameters));

        // then
        assertEquals("response does not contain a Bundle", exception.getMessage());
    }

    @Test
    void methodEncodeResponse_shouldThrowExceptionWhenUnableToResolveResponse() {
        // given
        var emptyBundle = new Bundle();

        final Parameters parameters = new Parameters()
                .addParameter(
                        new ParametersParameterComponent().setName("return").setResource(emptyBundle));

        CdsResponseEncoderService encoder = new CdsResponseEncoderService(repository);

        // when
        Exception exception = assertThrows(RuntimeException.class, () -> encoder.encodeResponse(parameters));

        // then
        assertEquals("unable to resolve response", exception.getMessage());
    }
}
