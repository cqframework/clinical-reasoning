package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.exception.InvalidInterval;

import java.time.ZonedDateTime;

/**
 * Ensure that periods used for measure evaluation, specifically, start and end ZonedDateTimes,
 * are valid in relation to each other.
 * <p/>
 * For example, a period with a start of 2024-01-01 and an end of 2023-01-01 is invalid, since a
 * start date must be before an end date.
 */
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
