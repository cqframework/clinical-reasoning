package org.opencds.cqf.fhir.cr.measure.r4.utils;

import jakarta.annotation.Nonnull;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.ResourceType;

/**
 * Various utilities for dealing with R4 resource IDs or their Strings.
 */
public class R4ResourceIdUtils {
    private static final Pattern PATTERN_SLASH = Pattern.compile("/");

    private R4ResourceIdUtils() {
        // Static utility class
    }

    @Nonnull
    public static String stripPatientQualifier(String subjectId) {
        return stripSpecificResourceQualifier(subjectId, ResourceType.Patient);
    }

    @Nonnull
    public static String stripSpecificResourceQualifier(String subjectId, ResourceType resourceType) {
        return subjectId.replace(resourceType.toString().concat("/"), "");
    }

    public static String stripAnyResourceQualifier(String subjectId) {

        if (subjectId == null) {
            return null;
        }

        final String[] split = PATTERN_SLASH.split(subjectId);

        if (split.length >= 2) {
            return split[1];
        }

        return split[0];
    }
}
