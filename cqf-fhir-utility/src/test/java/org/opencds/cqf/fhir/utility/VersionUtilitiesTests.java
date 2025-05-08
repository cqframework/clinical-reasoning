package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.junit.jupiter.api.Test;

class VersionUtilitiesTests {

    @Test
    void TestDstu3Versions() {
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("dstu3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("DSTU3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3.0"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3.0.1"));
        assertInstanceOf(StringType.class, VersionUtilities.stringTypeForVersion(FhirVersionEnum.DSTU3));
        assertInstanceOf(UriType.class, VersionUtilities.uriTypeForVersion(FhirVersionEnum.DSTU3));
        assertInstanceOf(BooleanType.class, VersionUtilities.booleanTypeForVersion(FhirVersionEnum.DSTU3, true));
    }

    @Test
    void TestR4Versions() {
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("r4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("R4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4.0"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4.0.1"));
        assertInstanceOf(
                org.hl7.fhir.r4.model.StringType.class, VersionUtilities.stringTypeForVersion(FhirVersionEnum.R4));
        assertInstanceOf(org.hl7.fhir.r4.model.UriType.class, VersionUtilities.uriTypeForVersion(FhirVersionEnum.R4));
        assertInstanceOf(
                org.hl7.fhir.r4.model.BooleanType.class,
                VersionUtilities.booleanTypeForVersion(FhirVersionEnum.R4, true));
    }

    @Test
    void TestR5Versions() {
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("r5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("R5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5.0"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5.0.1"));
        assertInstanceOf(
                org.hl7.fhir.r5.model.StringType.class, VersionUtilities.stringTypeForVersion(FhirVersionEnum.R5));
        assertInstanceOf(org.hl7.fhir.r5.model.UriType.class, VersionUtilities.uriTypeForVersion(FhirVersionEnum.R5));
        assertInstanceOf(
                org.hl7.fhir.r5.model.BooleanType.class,
                VersionUtilities.booleanTypeForVersion(FhirVersionEnum.R5, true));
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
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.stringTypeForVersion(FhirVersionEnum.R4B);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.uriTypeForVersion(FhirVersionEnum.R4B);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.booleanTypeForVersion(FhirVersionEnum.R4B, true);
        });
    }
}
