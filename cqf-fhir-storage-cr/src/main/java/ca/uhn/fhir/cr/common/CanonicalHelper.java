package ca.uhn.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public class CanonicalHelper {

    private CanonicalHelper() {}

    public static <C extends IPrimitiveType<String>> C getCanonicalType(
            FhirVersionEnum fhirVersion, String canonical, String url, String version) {
        String urlToUse = version == null ? url : String.format("%s|%s", url, version);
        String canonicalToUse = canonical == null ? urlToUse : canonical;
        return newCanonicalType(fhirVersion, canonicalToUse);
    }

    @SuppressWarnings("unchecked")
    private static <C extends IPrimitiveType<String>> C newCanonicalType(
            FhirVersionEnum fhirVersion, String canonical) {
        if (canonical == null) {
            return null;
        }
        switch (fhirVersion) {
            case DSTU3:
                return (C) new org.hl7.fhir.dstu3.model.StringType(canonical);
            case R4:
                return (C) new org.hl7.fhir.r4.model.CanonicalType(canonical);
            case R5:
                return (C) new org.hl7.fhir.r5.model.CanonicalType(canonical);
            default:
                return null;
        }
    }
}
