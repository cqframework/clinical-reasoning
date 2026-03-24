package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.ZonedDateTime;
import java.util.List;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Parameters for the care-gaps operation.
 *
 * @param periodStart measurement period starting interval
 * @param periodEnd measurement period ending interval
 * @param subject subject reference (Patient/{id}, Group/{id}, Practitioner/{id}, or null for all)
 * @param status care-gap statuses to include in results
 * @param measures measure references (by ID, identifier, or canonical URL)
 * @param notDocument if true, return summarized bundle with only DetectedIssue instead of document bundle
 */
public record R4CareGapsParameters(
        ZonedDateTime periodStart,
        ZonedDateTime periodEnd,
        String subject,
        List<String> status,
        List<MeasureReference> measures,
        boolean notDocument) {}
