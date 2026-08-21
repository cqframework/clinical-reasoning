package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.math.BigDecimal;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.r4.model.CanonicalType;
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
        assertInstanceOf(UriType.class, VersionUtilities.canonicalTypeForVersion(FhirVersionEnum.DSTU3));
        assertInstanceOf(BooleanType.class, VersionUtilities.booleanTypeForVersion(FhirVersionEnum.DSTU3, true));
        assertInstanceOf(Reference.class, VersionUtilities.referenceTypeForVersion(FhirVersionEnum.DSTU3, "ref"));
        assertInstanceOf(
                Reference.class, VersionUtilities.referenceTypeForVersion(FhirVersionEnum.DSTU3, new Patient()));
        assertInstanceOf(CodeType.class, VersionUtilities.codeTypeForVersion(FhirVersionEnum.DSTU3, "code"));
        assertInstanceOf(IntegerType.class, VersionUtilities.integerTypeForVersion(FhirVersionEnum.DSTU3, 1));
        assertInstanceOf(
                DecimalType.class, VersionUtilities.decimalTypeForVersion(FhirVersionEnum.DSTU3, new BigDecimal(1)));
        assertInstanceOf(
                DateTimeType.class,
                VersionUtilities.dateTimeTypeForVersion(FhirVersionEnum.DSTU3, "2026-01-01T12:12:12"));
        assertInstanceOf(
                InstantType.class,
                VersionUtilities.instantTypeForVersion(FhirVersionEnum.DSTU3, "2026-01-01T12:12:12"));
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
        assertInstanceOf(CanonicalType.class, VersionUtilities.canonicalTypeForVersion(FhirVersionEnum.R4));
        assertInstanceOf(
                org.hl7.fhir.r4.model.BooleanType.class,
                VersionUtilities.booleanTypeForVersion(FhirVersionEnum.R4, true));
        assertInstanceOf(
                org.hl7.fhir.r4.model.Reference.class,
                VersionUtilities.referenceTypeForVersion(FhirVersionEnum.R4, "ref"));
        assertInstanceOf(
                org.hl7.fhir.r4.model.Reference.class,
                VersionUtilities.referenceTypeForVersion(FhirVersionEnum.R4, new Patient()));
        assertInstanceOf(
                org.hl7.fhir.r4.model.CodeType.class, VersionUtilities.codeTypeForVersion(FhirVersionEnum.R4, "code"));
        assertInstanceOf(
                org.hl7.fhir.r4.model.IntegerType.class, VersionUtilities.integerTypeForVersion(FhirVersionEnum.R4, 1));
        assertInstanceOf(
                org.hl7.fhir.r4.model.DecimalType.class,
                VersionUtilities.decimalTypeForVersion(FhirVersionEnum.R4, new BigDecimal(1)));
        assertInstanceOf(
                org.hl7.fhir.r4.model.DateTimeType.class,
                VersionUtilities.dateTimeTypeForVersion(FhirVersionEnum.R4, "2026-01-01T12:12:12"));
        assertInstanceOf(
                org.hl7.fhir.r4.model.InstantType.class,
                VersionUtilities.instantTypeForVersion(FhirVersionEnum.R4, "2026-01-01T12:12:12"));
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
                org.hl7.fhir.r5.model.CanonicalType.class,
                VersionUtilities.canonicalTypeForVersion(FhirVersionEnum.R5));
        assertInstanceOf(
                org.hl7.fhir.r5.model.BooleanType.class,
                VersionUtilities.booleanTypeForVersion(FhirVersionEnum.R5, true));
        assertInstanceOf(
                org.hl7.fhir.r5.model.Reference.class,
                VersionUtilities.referenceTypeForVersion(FhirVersionEnum.R5, "ref"));
        assertInstanceOf(
                org.hl7.fhir.r5.model.Reference.class,
                VersionUtilities.referenceTypeForVersion(FhirVersionEnum.R5, new Patient()));
        assertInstanceOf(
                org.hl7.fhir.r5.model.CodeType.class, VersionUtilities.codeTypeForVersion(FhirVersionEnum.R5, "code"));
        assertInstanceOf(
                org.hl7.fhir.r5.model.IntegerType.class, VersionUtilities.integerTypeForVersion(FhirVersionEnum.R5, 1));
        assertInstanceOf(
                org.hl7.fhir.r5.model.DecimalType.class,
                VersionUtilities.decimalTypeForVersion(FhirVersionEnum.R5, new BigDecimal(1)));
        assertInstanceOf(
                org.hl7.fhir.r5.model.DateTimeType.class,
                VersionUtilities.dateTimeTypeForVersion(FhirVersionEnum.R5, "2026-01-01T12:12:12"));
        assertInstanceOf(
                org.hl7.fhir.r5.model.InstantType.class,
                VersionUtilities.instantTypeForVersion(FhirVersionEnum.R5, "2026-01-01T12:12:12"));
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
            VersionUtilities.canonicalTypeForVersion(FhirVersionEnum.R4B);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.booleanTypeForVersion(FhirVersionEnum.R4B, true);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.referenceTypeForVersion(FhirVersionEnum.R4B, "test");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.referenceTypeForVersion(FhirVersionEnum.R4B, new org.hl7.fhir.r4b.model.Patient());
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.codeTypeForVersion(FhirVersionEnum.R4B, "test");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.integerTypeForVersion(FhirVersionEnum.R4B, 1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.decimalTypeForVersion(FhirVersionEnum.R4B, new BigDecimal(1));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.dateTimeTypeForVersion(FhirVersionEnum.R4B, "test");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            VersionUtilities.instantTypeForVersion(FhirVersionEnum.R4B, "test");
        });
    }
}
