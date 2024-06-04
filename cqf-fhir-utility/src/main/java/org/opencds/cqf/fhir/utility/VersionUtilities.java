package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public class VersionUtilities {

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
        switch (fhirVersion.substring(0, 1)) {
            case "2":
                return FhirVersionEnum.DSTU2;
            case "3":
                return FhirVersionEnum.DSTU3;
            case "4":
                return FhirVersionEnum.R4;
            case "5":
                return FhirVersionEnum.R5;
            default:
                throw new IllegalArgumentException("unknown or unsupported FHIR version");
        }
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
        switch (fhirVersion) {
            case DSTU2:
                return new org.hl7.fhir.dstu2.model.StringType(string);
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.StringType(string);
            case R4:
                return new org.hl7.fhir.r4.model.StringType(string);
            case R5:
                return new org.hl7.fhir.r5.model.StringType(string);
            default:
                throw new IllegalArgumentException("unknown or unsupported FHIR version");
        }
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
        switch (fhirVersion) {
            case DSTU2:
                return new org.hl7.fhir.dstu2.model.UriType(uri);
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.UriType(uri);
            case R4:
                return new org.hl7.fhir.r4.model.UriType(uri);
            case R5:
                return new org.hl7.fhir.r5.model.UriType(uri);
            default:
                throw new IllegalArgumentException("unknown or unsupported FHIR version");
        }
    }
}
