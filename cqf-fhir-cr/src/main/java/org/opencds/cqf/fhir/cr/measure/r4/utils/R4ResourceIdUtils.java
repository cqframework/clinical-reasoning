package org.opencds.cqf.fhir.cr.measure.r4.utils;

import jakarta.annotation.Nonnull;
import org.hl7.fhir.r4.model.ResourceType;

/**
 * Various utilities for dealing with R4 resource IDs or their Strings.
 */
public class R4ResourceIdUtils {

    private R4ResourceIdUtils() {
        // Static utility class
    }

    @Nonnull
    public static String addPatientQualifier(String t) {
        return ResourceType.Patient.toString().concat("/").concat(t);
    }

    @Nonnull
    public static String stripPatientQualifier(String subjectId) {
        return stripSpecificResourceQualifier(subjectId, ResourceType.Patient);
    }

    @Nonnull
    public static String stripSpecificResourceQualifier(String subjectId, ResourceType resourceType) {
        return subjectId.replace(resourceType.toString().concat("/"), "");
    }
}
