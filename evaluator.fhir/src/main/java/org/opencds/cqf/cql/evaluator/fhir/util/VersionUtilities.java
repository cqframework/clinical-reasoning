package org.opencds.cqf.cql.evaluator.fhir.util;

import ca.uhn.fhir.context.FhirVersionEnum;

public class VersionUtilities {

    /**
     * Returns a FhirVersionEnum for the supplied version string.
     * Supports partial versions (e.g. "3.0") and named versions (e.g. "R4")
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
        }
        catch (Exception e) {}

        // This matches specific FHIR versions that match the structure versions
        // e.g. 4.0.1, 3.0.2, etc, including partials.
        FhirVersionEnum version = FhirVersionEnum.forVersionString(fhirVersion);
        if (version != null) {
            return version;
        }

        // This returns the closest matching major version
        switch(fhirVersion.substring(0, 1)) {
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
}