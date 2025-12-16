package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.fhir.cr.measure.common.def.report.GroupReportDef;

/**
 * Utility class for handling FHIR resource ID formatting, including detection and removal of resource type qualifiers.
 */
public class FhirResourceUtils {

    private static final Pattern PATTERN_SLASH = Pattern.compile("/");

    private FhirResourceUtils() {
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
                .map(FhirResourceUtils::stripAnyResourceQualifier)
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

    @Nullable
    public static String determineFhirResourceTypeOrNull(FhirContext fhirContext, GroupReportDef groupDef) {
        final String populationBasis = groupDef.getPopulationBasis().code();

        if (StringUtils.isBlank(populationBasis)) {
            return null;
        }

        try {
            // This will throw a DataFormatException if the resource name is invalid
            // for the current FHIR version.
            return fhirContext.getResourceDefinition(populationBasis).toString();
        } catch (DataFormatException exception) {
            // Ignore:  this is not a FHIR resource
            return null;
        }
    }
}
