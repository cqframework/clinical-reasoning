package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class AdapterFactoryTest {
    @Test
    void testUnsupportedVersion() {
        assertThrows(IllegalArgumentException.class, () -> AdapterFactory.forFhirVersion(FhirVersionEnum.DSTU2));
    }

    @Test
    void testFhirContext() {
        var context = FhirContext.forR4Cached();
        var adapterFactory = AdapterFactory.forFhirContext(context);
        assertNotNull(adapterFactory);
        var adapter = adapterFactory.createResource(new Patient());
        assertNotNull(adapter);
    }
}
