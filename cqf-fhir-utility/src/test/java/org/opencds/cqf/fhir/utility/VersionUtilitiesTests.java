package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;

class VersionUtilitiesTests {

    @Test
    void TestDstu3Versions() {
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("dstu3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("DSTU3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3.0"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3.0.1"));
    }

    @Test
    void TestR4Versions() {
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("r4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("R4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4.0"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4.0.1"));
    }

    @Test
    void TestR5Versions() {
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("r5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("R5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5.0"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5.0.1"));
    }

    @Test
    void TestNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.enumForVersion(null);
        });
    }

    @Test
    void TestMalformed() {
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.enumForVersion("bubba");
        });
    }

    @Test
    void TestUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.enumForVersion("R6");
        });
    }
}
