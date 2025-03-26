package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.opencds.cqf.fhir.utility.client.ExpandRunner.TerminologyServerExpansionException;

class ExpandRunnerTest {

    @Test
    void testTimeout() {
        var url = "test";
        var params = new Parameters();
        var settings = new TerminologyServerClientSettings().setTimeoutSeconds(2);
        var fhirClient = mock(IGenericClient.class, new ReturnsDeepStubs());
        var fixture = new ExpandRunner(fhirClient, settings, url, params);
        assertThrows(
                TerminologyServerExpansionException.class,
                fixture::expandValueSet,
                "Terminology Server expansion took longer than the allotted timeout: 2");
    }
}
