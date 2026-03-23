package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.IdType;

/**
 * Parameters for the care-gaps operation.
 *
 * @param periodStart measurement period starting interval
 * @param periodEnd measurement period ending interval
 * @param subject subject reference (Patient/{id}, Group/{id}, Practitioner/{id}, or null for all)
 * @param status care-gap statuses to include in results
 * @param measureId measures to resolve by FHIR resource id
 * @param measureIdentifier measures to resolve by identifier value or system|value
 * @param measureUrl measures to resolve by canonical URL
 * @param notDocument if true, return summarized bundle with only DetectedIssue instead of document bundle
 */
public record R4CareGapsParameters(
        ZonedDateTime periodStart,
        ZonedDateTime periodEnd,
        String subject,
        List<String> status,
        List<IdType> measureId,
        List<String> measureIdentifier,
        List<String> measureUrl,
        boolean notDocument) {}
