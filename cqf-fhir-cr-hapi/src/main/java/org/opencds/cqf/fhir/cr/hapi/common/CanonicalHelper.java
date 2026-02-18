package org.opencds.cqf.fhir.cr.hapi.common;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public class CanonicalHelper {

    private CanonicalHelper() {}

    public static <C extends IPrimitiveType<String>> C getCanonicalType(
            FhirVersionEnum fhirVersion,
            IBaseBackboneElement canonical,
            IBaseBackboneElement url,
            IBaseDatatype version) {
        return getCanonicalType(
                fhirVersion,
                getStringValue(fhirVersion, canonical),
                getStringValue(fhirVersion, url),
                getStringValue(version));
    }

    public static <C extends IPrimitiveType<String>> C getCanonicalType(
            FhirVersionEnum fhirVersion, String canonical, String url, String version) {
        String urlToUse = version == null ? url : "%s|%s".formatted(url, version);
        String canonicalToUse = canonical == null ? urlToUse : canonical;
        return newCanonicalType(fhirVersion, canonicalToUse);
    }

    @SuppressWarnings("unchecked")
    public static <C extends IPrimitiveType<String>> C newCanonicalType(FhirVersionEnum fhirVersion, String canonical) {
        if (canonical == null) {
            return null;
        }
        return switch (fhirVersion) {
            case DSTU3 -> (C) new org.hl7.fhir.dstu3.model.StringType(canonical);
            case R4 -> (C) new org.hl7.fhir.r4.model.CanonicalType(canonical);
            case R5 -> (C) new org.hl7.fhir.r5.model.CanonicalType(canonical);
            default -> null;
        };
    }
}
