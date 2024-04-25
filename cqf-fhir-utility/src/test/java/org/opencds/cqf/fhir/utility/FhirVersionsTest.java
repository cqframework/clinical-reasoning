package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Library;
import org.junit.jupiter.api.Test;

class FhirVersionsTest {

    @Test
    void getFhirVersion() {
        FhirVersionEnum fhirVersion = FhirVersions.forClass(Library.class);

        assertEquals(FhirVersionEnum.DSTU3, fhirVersion);
    }

    @Test
    void getFhirVersionUnknownClass() {
        Library library = new Library();
        FhirVersionEnum fhirVersion = FhirVersions.forClass(library.getClass());

        assertEquals(FhirVersionEnum.DSTU3, fhirVersion);
    }
}
