package org.opencds.cqf.cql.evaluator.fhir.util;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirVersionEnum;

public class VersionUtilitiesTests {

    @Test
    public void TestDstu3Versions() {
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("dstu3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("DSTU3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3.0"));
        assertEquals(FhirVersionEnum.DSTU3, VersionUtilities.enumForVersion("3.0.1"));
    }

    @Test
    public void TestR4Versions() {
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("r4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("R4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4.0"));
        assertEquals(FhirVersionEnum.R4, VersionUtilities.enumForVersion("4.0.1"));
    }

    @Test
    public void TestR5Versions() {
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("r5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("R5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5.0"));
        assertEquals(FhirVersionEnum.R5, VersionUtilities.enumForVersion("5.0.1"));
    }

    @Test(expectedExceptions  = IllegalArgumentException.class)
    public void TestNull() {
        VersionUtilities.enumForVersion(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void TestMalformed() {
        VersionUtilities.enumForVersion("bubba");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void TestUnsupported() {
        VersionUtilities.enumForVersion("R6");
    }
    
}