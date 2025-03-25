package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;

class ExpandRunnerTest {

    @Test
    void testTimeout() {
        var url = "test";
        var params = new Parameters();
        var settings = new TerminologyServerClientSettings().setTimeoutSeconds(2);
        var fhirClient = mock(IGenericClient.class, new ReturnsDeepStubs());
        var fixture = new ExpandRunner(fhirClient, settings, url, params);
        assertThrows(
                UnprocessableEntityException.class,
                fixture::expandValueSet,
                "Terminology Server expansion took longer than the allotted timeout: 2");
    }
}
