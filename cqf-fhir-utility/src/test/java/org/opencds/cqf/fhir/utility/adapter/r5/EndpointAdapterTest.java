package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r5.model.Endpoint;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.junit.jupiter.api.Test;

public class EndpointAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.AdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new EndpointAdapter(new PlanDefinition()));
    }

    @Test
    void adapter_get_and_set_address() {
        var endpoint = new Endpoint();
        var address = "123 Test Street";
        endpoint.setAddress(address);
        var adapter = (EndpointAdapter) adapterFactory.createResource(endpoint);
        assertEquals(address, adapter.getAddress());
        var newAddress = "456 Test Street";
        adapter.setAddress(newAddress);
        assertEquals(newAddress, endpoint.getAddress());
    }
}
