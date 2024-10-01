package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.opencds.cqf.cql.engine.exception.InvalidInterval;

import java.time.ZonedDateTime;

// LUKETODO:  javadoc
// This gets called IMMEDIATELY from ALL parts of ClinicalReasoning called by hapi-fhir or cdr that pass periods
public class MeasurePeriodValidator {
	public void validateParsedPeriodStartAndEnd(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        if ((periodStart == null && periodEnd != null) || (periodStart != null && periodEnd == null)) {
            throw new InvalidInterval(String.format(
                "Invalid Period - Either both or neither should null: start date: %s and end date %s", periodStart, periodEnd));
        }

        if (periodStart == null && periodEnd == null) {
            // Nothing more to do
            return;
        }

		// This should probably never happen
		if (periodStart.isEqual(periodEnd)) {
			throw new InvalidInterval(String.format(
					"Invalid Period - Start date: %s is the same as end date: %s", periodStart, periodEnd));
		}

		if (periodStart.isAfter(periodEnd)) {
            throw new InvalidInterval(String.format(
					"Invalid Period - the ending boundary: %s must be greater than or equal to the starting boundary: %s",
					periodEnd, periodStart));
		}
	}
}
