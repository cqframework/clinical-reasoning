package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.math.BigDecimal;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public class VersionUtilities {
    private static final String UNSUPPORTED = "unknown or unsupported FHIR version";

    private VersionUtilities() {
        // intentionally empty
    }

    /**
     * Returns a FhirVersionEnum for the supplied version string. Supports partial versions (e.g.
     * "3.0") and named versions (e.g. "R4")
     *
     * @param fhirVersion the FHIR version to get an enum for.
     * @return the FhirVersionEnum
     */
    public static FhirVersionEnum enumForVersion(String fhirVersion) {
        if (fhirVersion == null || fhirVersion.isEmpty()) {
            throw new IllegalArgumentException("fhirVersion can not be null or empty");
        }

        // This matches "R4", "dstu3", etc.
        try {
            return FhirVersionEnum.valueOf(fhirVersion.toUpperCase());
        } catch (Exception e) {
            // intentionally empty, we want to fall through
        }

        // This matches specific FHIR versions that match the structure versions
        // e.g. 4.0.1, 3.0.2, etc, including partials.
        FhirVersionEnum version = FhirVersionEnum.forVersionString(fhirVersion);
        if (version != null) {
            return version;
        }

        // This returns the closest matching major version
        return switch (fhirVersion.substring(0, 1)) {
            case "2" -> FhirVersionEnum.DSTU2;
            case "3" -> FhirVersionEnum.DSTU3;
            case "4" -> FhirVersionEnum.R4;
            case "5" -> FhirVersionEnum.R5;
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    /**
     * Returns a StringType for the supplied FHIR version.
     *
     * @param fhirVersion the FHIR version to create a StringType for
     * @return new StringType
     */
    public static IPrimitiveType<String> stringTypeForVersion(FhirVersionEnum fhirVersion) {
        return stringTypeForVersion(fhirVersion, null);
    }

    /**
     * Returns a StringType for the supplied version with a value of the supplied string.
     *
     * @param fhirVersion the FHIR version to create a StringType for
     * @param string the string value of the StringType
     * @return the new StringType
     */
    public static IPrimitiveType<String> stringTypeForVersion(FhirVersionEnum fhirVersion, String string) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.StringType(string);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.StringType(string);
            case R4 -> new org.hl7.fhir.r4.model.StringType(string);
            case R5 -> new org.hl7.fhir.r5.model.StringType(string);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    /**
     * Returns a UriType for the supplied FHIR version.
     *
     * @param fhirVersion the FHIR version to create a UriType for
     * @return new UriType
     */
    public static IPrimitiveType<String> uriTypeForVersion(FhirVersionEnum fhirVersion) {
        return uriTypeForVersion(fhirVersion, null);
    }

    /**
     * Returns a UriType for the supplied version with a value of the supplied uri.
     *
     * @param fhirVersion the FHIR version to create a UriType for
     * @param uri the string value of the UriType
     * @return the new UriType
     */
    public static IPrimitiveType<String> uriTypeForVersion(FhirVersionEnum fhirVersion, String uri) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.UriType(uri);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.UriType(uri);
            case R4 -> new org.hl7.fhir.r4.model.UriType(uri);
            case R5 -> new org.hl7.fhir.r5.model.UriType(uri);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    /**
     * Returns a CanonicalType for the supplied FHIR version.
     *
     * @param fhirVersion the FHIR version to create a CanonicalType for
     * @return new CanonicalType
     */
    public static IPrimitiveType<String> canonicalTypeForVersion(FhirVersionEnum fhirVersion) {
        return canonicalTypeForVersion(fhirVersion, null);
    }

    /**
     * Returns a CanonicalType for the supplied version with a value of the supplied value.
     * A UriType will be returned for FHIR versions before R4.
     *
     * @param fhirVersion the FHIR version to create a CanonicalType for
     * @param value the string value of the CanonicalType
     * @return the new CanonicalType
     */
    public static IPrimitiveType<String> canonicalTypeForVersion(FhirVersionEnum fhirVersion, String value) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.UriType(value);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.UriType(value);
            case R4 -> new org.hl7.fhir.r4.model.CanonicalType(value);
            case R5 -> new org.hl7.fhir.r5.model.CanonicalType(value);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    /**
     * Returns a BooleanType for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a BooleanType for
     * @param value the value of the BooleanType
     * @return the new BooleanType
     */
    public static IPrimitiveType<Boolean> booleanTypeForVersion(FhirVersionEnum fhirVersion, boolean value) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.BooleanType(value);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.BooleanType(value);
            case R4 -> new org.hl7.fhir.r4.model.BooleanType(value);
            case R5 -> new org.hl7.fhir.r5.model.BooleanType(value);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    /**
     * Returns a CodeType for the supplied version with a value of the supplied code.
     *
     * @param fhirVersion the FHIR version to create a CodeType for
     * @param code the string value of the CodeType
     * @return the new CodeType
     */
    public static IPrimitiveType<String> codeTypeForVersion(FhirVersionEnum fhirVersion, String code) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.CodeType(code);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.CodeType(code);
            case R4 -> new org.hl7.fhir.r4.model.CodeType(code);
            case R5 -> new org.hl7.fhir.r5.model.CodeType(code);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    /**
     * Returns a Reference for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a Reference for
     * @param value the string value of the Reference
     * @return the new Reference
     */
    public static IBaseReference referenceTypeForVersion(FhirVersionEnum fhirVersion, String value) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.Reference().setReference(value);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Reference().setReference(value);
            case R4 -> new org.hl7.fhir.r4.model.Reference().setReference(value);
            case R5 -> new org.hl7.fhir.r5.model.Reference().setReference(value);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    /**
     * Returns a Reference for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a Reference for
     * @param value the string value of the Reference
     * @return the new Reference
     */
    public static IBaseReference referenceTypeForVersion(FhirVersionEnum fhirVersion, IAnyResource value) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.Reference(value);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Reference(value);
            case R4 -> new org.hl7.fhir.r4.model.Reference(value);
            case R5 -> new org.hl7.fhir.r5.model.Reference(value);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    public static IPrimitiveType<Integer> integerTypeForVersion(FhirVersionEnum fhirVersion, Integer value) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.IntegerType(value);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.IntegerType(value);
            case R4 -> new org.hl7.fhir.r4.model.IntegerType(value);
            case R5 -> new org.hl7.fhir.r5.model.IntegerType(value);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }

    public static IPrimitiveType<BigDecimal> decimalTypeForVersion(FhirVersionEnum fhirVersion, BigDecimal value) {
        return switch (fhirVersion) {
            case DSTU2 -> new org.hl7.fhir.dstu2.model.DecimalType(value);
            case DSTU3 -> new org.hl7.fhir.dstu3.model.DecimalType(value);
            case R4 -> new org.hl7.fhir.r4.model.DecimalType(value);
            case R5 -> new org.hl7.fhir.r5.model.DecimalType(value);
            default -> throw new IllegalArgumentException(UNSUPPORTED);
        };
    }
}
