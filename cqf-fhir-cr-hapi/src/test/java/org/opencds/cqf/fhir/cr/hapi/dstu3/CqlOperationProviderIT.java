package org.opencds.cqf.fhir.cr.hapi.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.junit.jupiter.api.Test;

class CqlOperationProviderIT extends BaseCrDstu3TestServer {
    @Test
    void testEvaluateCqlWithPOST() {
        var parameters = newParameters(getFhirContext(), newStringPart(getFhirContext(), "expression", "5*5"));
        var result = ourClient
                .operation()
                .onServer()
                .named(ProviderConstants.CR_OPERATION_CQL)
                .withParameters(parameters)
                .returnResourceType(Parameters.class)
                .execute();

        assertNotNull(result);
        assertEquals(1, result.getParameter().size());
        assertEquals("25", ((IPrimitiveType<?>) result.getParameter().get(0).getValue()).getValueAsString());
    }

    @Test
    void testEvaluateCqlWithGET() {
        var parameters = newParameters(getFhirContext(), newStringPart(getFhirContext(), "expression", "5*5"));
        var result = ourClient
                .operation()
                .onServer()
                .named(ProviderConstants.CR_OPERATION_CQL)
                .withParameters(parameters)
                .returnResourceType(Parameters.class)
                .useHttpGet()
                .execute();

        assertNotNull(result);
        assertEquals(1, result.getParameter().size());
        assertEquals("25", ((IPrimitiveType<?>) result.getParameter().get(0).getValue()).getValueAsString());
    }
}
