package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for handling FHIR resource ID formatting, including detection and removal of resource type qualifiers.
 */
public class ResourceIdUtils {

    private static final Pattern PATTERN_SLASH = Pattern.compile("/");

    private ResourceIdUtils() {
        // Static utility class
    }

    public static boolean hasResourceQualifier(String resourceId) {
        return PATTERN_SLASH.matcher(resourceId).find();
    }

    @Nonnull
    public static String addPatientQualifier(String t) {
        return "Patient".concat("/").concat(t);
    }

    public static Set<String> stripAnyResourceQualifiersAsSet(Collection<String> subjectIds) {
        return subjectIds.stream()
                .map(ResourceIdUtils::stripAnyResourceQualifier)
                .collect(Collectors.toUnmodifiableSet());
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
