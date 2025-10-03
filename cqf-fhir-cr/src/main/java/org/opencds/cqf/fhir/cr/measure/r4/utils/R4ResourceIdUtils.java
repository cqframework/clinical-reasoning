package org.opencds.cqf.fhir.cr.measure.r4.utils;

import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.ResourceType;

/**
 * Various utilities for dealing with R4 resource IDs or their Strings.
 */
public class R4ResourceIdUtils {

    @Nonnull
    public static String addPatientQualifier(String t) {
        return ResourceType.Patient.toString().concat("/").concat(t);
    }

    @Nonnull
    public static String stripPatientQualifier(String subjectId) {
        return subjectId.replace(ResourceType.Patient.toString().concat("/"), "");
    }
}
