package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// LUKETODO: javadoc
public class ResourceIdUtils {

    private static final Pattern PATTERN_SLASH = Pattern.compile("/");

    private ResourceIdUtils() {
        // Static utility class
    }

    public static boolean hasResourceQualifier(String resourceId) {
        return PATTERN_SLASH.matcher(resourceId).find();
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
