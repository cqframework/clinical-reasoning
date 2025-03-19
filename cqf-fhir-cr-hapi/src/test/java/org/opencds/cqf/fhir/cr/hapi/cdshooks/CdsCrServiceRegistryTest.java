package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CdsCrServiceRegistryTest {

    CdsCrServiceRegistry fixture = new CdsCrServiceRegistry();

    @Test
    void testRegisterAndUnregister() {
        fixture.unregister(FhirVersionEnum.R4);
        assertEquals(Optional.empty(), fixture.find(FhirVersionEnum.R4));
        fixture.register(FhirVersionEnum.R4, CdsCrService.class);
        assertEquals(Optional.of(CdsCrService.class), fixture.find(FhirVersionEnum.R4));
    }
}
