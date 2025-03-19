package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CdsCrDiscoveryServiceRegistryTest {

    CdsCrDiscoveryServiceRegistry fixture = new CdsCrDiscoveryServiceRegistry();

    @Test
    void testRegisterAndUnregister() {
        fixture.unregister(FhirVersionEnum.R4);
        assertEquals(Optional.empty(), fixture.find(FhirVersionEnum.R4));
        fixture.register(FhirVersionEnum.R4, CrDiscoveryService.class);
        assertEquals(Optional.of(CrDiscoveryService.class), fixture.find(FhirVersionEnum.R4));
    }
}
